package pixelitor.filters;

import net.jafama.FastMath;
import pd.OpenSimplex2F;
import pixelitor.ThreadPool;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.particles.Particle;
import pixelitor.particles.ParticleSystem;
import pixelitor.utils.ReseedSupport;
import pixelitor.utils.Rnd;
import pixelitor.utils.Shapes;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

public class FlowField extends ParametrizedFilter {

    public static final String NAME = "Flow Field";

    public static float GOLDEN_RATIO_CONJUGATE = 0.618033988749895f;

    private static final int PAD = 100;
    private static final int PARTICLES_PER_GROUP = 100;

    public enum PhysicsMode {
        FORCE_MODE_VELOCITY("No Mass") {
            @Override
            void updateParticle(FlowFieldParticle particle, float dx, float dy) {
                particle.x += dx;
                particle.y += dy;
            }
        }, FORCE_MODE_ACCELERATION("Uniform Mass") {
            @Override
            void updateParticle(FlowFieldParticle particle, float dx, float dy) {
                particle.x += particle.vx += dx;
                particle.y += particle.vy += dy;
            }
        }, FORCE_MODE_JOLT("Something") {
            @Override
            void updateParticle(FlowFieldParticle particle, float dx, float dy) {
                particle.x += particle.vx += particle.ax += dx;
                particle.y += particle.vy += particle.ay += dy;
            }
        }, FORCE_MODE_FORCE("Random Mass") {
            @Override
            void updateParticle(FlowFieldParticle particle, float dx, float dy) {
                particle.x += particle.vx += dx / particle.mass;
                particle.y += particle.vy += dy / particle.mass;
            }
        };

        private final String name;

        PhysicsMode(String name) {
            this.name = name;
        }

        abstract void updateParticle(FlowFieldParticle particle, float dx, float dy);

        @Override
        public String toString() {
            return name;
        }
    }

    private final RangeParam particlesParam = new RangeParam("Particle Count", 1, 1000, 40000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam zoomParam = new RangeParam("Zoom", 1000, 40000, 100000);
    private final StrokeParam strokeParam = new StrokeParam("Stroke");

    private final EnumParam<PhysicsMode> physicsModeParam = new EnumParam<>("Some Name", PhysicsMode.class);
    private final RangeParam massRandomnessParam = new RangeParam("Mass Randomness", 0, 10, 100);
    private final RangeParam velocityCapParam = new RangeParam("MaximumVelocity", 1, 100000, 6300000);
    private final LogZoomParam forceParam = new LogZoomParam("Force", 1, 320, 400);
    private final RangeParam varienceParam = new RangeParam("Varience", 1, 20, 100);
    private final DialogParam physicsParam = new DialogParam("Physics", physicsModeParam, massRandomnessParam, velocityCapParam, forceParam, varienceParam);

    private final ColorParam backgroundColorParam = new ColorParam("Background Color", new Color(0, 0, 0, 1f), ColorParam.TransparencyPolicy.FREE_TRANSPARENCY);
    private final ColorParam foregroundColorParam = new ColorParam("Particle Color", new Color(1, 1, 1, 0.12f), ColorParam.TransparencyPolicy.FREE_TRANSPARENCY);
    private final RangeParam colorRandomnessParam = new RangeParam("Color Randomness (%)", 0, 0, 100);

    private final RangeParam qualityParam = new RangeParam("Quality (%)", 1, 75, 100);
    private final RangeParam iterationsParam = new RangeParam("Iterations", 1, 100, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam turbulenceParam = new RangeParam("Turbulence", 1, 1, 8);
    private final RangeParam zFactorParam = new RangeParam("Z Factor", 0, 0, 1000);
    private final RangeParam drawToleranceParam = new RangeParam("Tolerance", 0, 30, 200);
    private final RangeParam curvinessParam = new RangeParam("Curviness", 0, 100, 1000);
    private final BooleanParam showFlowVectors = new BooleanParam("Flow Vectors", false, IGNORE_RANDOMIZE);
    private final DialogParam advancedParam = new DialogParam("Advanced", qualityParam, iterationsParam, turbulenceParam, zFactorParam, drawToleranceParam, curvinessParam, showFlowVectors);

    public FlowField() {
        super(false);

        setParams(
                particlesParam,
                zoomParam,
                strokeParam,
                physicsParam,
                backgroundColorParam,
                foregroundColorParam,
                colorRandomnessParam,
                advancedParam
        ).withAction(ReseedSupport.createAction());

        iterationsParam.setToolTip("Change filament thickness");
        particlesParam.setToolTip("Number of filaments");
        zoomParam.setToolTip("Adjust the zoom");
        strokeParam.setToolTip("Adjust the stroke style");
        colorRandomnessParam.setToolTip("Randomize colors");

        qualityParam.setToolTip("Smoothness of filament");
        forceParam.setToolTip("Stroke Length");
        turbulenceParam.setToolTip("Adjust the varience provided by Noise.");
        showFlowVectors.setToolTip("View direction of flow");

    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        int particle_count = particlesParam.getValue();
        float zoom = zoomParam.getValue() * 0.01f;
        Stroke stroke = strokeParam.createStroke();
        Color bgColor = backgroundColorParam.getColor();
        Color fgColor = foregroundColorParam.getColor();
        float colorRandomness = colorRandomnessParam.getPercentageValF();
        boolean randomColor = colorRandomness != 0;

        PhysicsMode physicsMode = physicsModeParam.getSelected();
        int massRandomness = massRandomnessParam.getValue();
        float maximumVelocity = velocityCapParam.getValue() * velocityCapParam.getValue() / 10000f;
        float force = (float) forceParam.getZoomRatio();
        float variance = varienceParam.getValue() / 10f;

        float quality = qualityParam.getValueAsFloat() / 99;
        int iteration_count = iterationsParam.getValue();
        int turbulence = turbulenceParam.getValue();
        float zFactor = zFactorParam.getValueAsFloat() / 1000000;
        int tolerance = drawToleranceParam.getValue();
        float curviness = curvinessParam.getValue() / 100f;
        boolean showFlowVectors = this.showFlowVectors.isChecked();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        int w = dest.getWidth();
        int h = dest.getHeight();

        int groupCount = FastMath.ceilToInt(particle_count * 1d / PARTICLES_PER_GROUP);
        Future<?>[] futures = new Future[groupCount];
        var pt = new StatusBarProgressTracker(NAME, futures.length + 1);
//        var pt = new DebugProgressTracker(NAME, futures.length + 1, new StatusBarProgressTracker(NAME, futures.length + 1));
//        Utils.sleep(500, TimeUnit.MILLISECONDS);
//        pt.unitDone();

        Random r = ReseedSupport.reInitialize();
        OpenSimplex2F noise = ReseedSupport.getSimplexNoise();

        Graphics2D g2 = dest.createGraphics();
        g2.setStroke(stroke);
        Colors.fillWith(bgColor, g2, w, h);

        int field_w = (int) (w * quality + 1);
        float field_density = field_w * 1f / w;
        int field_h = (int) (h * field_density);

        Rectangle bounds = new Rectangle(-PAD, -PAD, w + PAD * 2, h + PAD * 2);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        float PI = (float) FastMath.PI * variance;
        float initTheta = (float) (r.nextFloat() * 2 * FastMath.PI);

        // NOTE: verify that it's better than creating a "new Color[field_w][field_h] when not needed".
        float[][] field = ((Supplier<float[][]>) () -> {
            if (showFlowVectors)
                return new float[field_w][field_h];
            return null;
        }).get();

        Color[][] col_field = ((Supplier<Color[][]>) () -> {
            if (randomColor)
                return new Color[field_w][field_h];
            return null;
        }).get();

        float[] hsb_col = null;
        if (randomColor)
            hsb_col = Colors.toHSB(Rnd.createRandomColor(r, false));

        for (int i = 0; i < field_w; i++) {
            for (int j = 0; j < field_h; j++) {
                if (randomColor) {
                    col_field[i][j] = Colors.rgbInterpolate(fgColor, new Color(Colors.HSBAtoARGB(hsb_col, fgColor.getAlpha()), true), colorRandomness);

                    hsb_col[0] = (hsb_col[0] + GOLDEN_RATIO_CONJUGATE) % 1;
                }
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        g2.setColor(fgColor);

        ParticleSystem<FlowFieldParticle> system = new ParticleSystem<>(groupCount, PARTICLES_PER_GROUP) {

            float z = 0;

            @Override
            public void step(int idx) {
                super.step(idx);
                z += zFactor;
            }

            @Override
            protected FlowFieldParticle newParticle() {
                return new FlowFieldParticle(tolerance, curviness, massRandomness);
            }

            @Override
            protected void initializeParticle(FlowFieldParticle particle) {

                particle.lastX = particle.x = bounds.x + bounds.width * r.nextFloat();
                particle.lastY = particle.y = bounds.y + bounds.height * r.nextFloat();

                int field_x = FastMath.toRange(0, field_w - 1, (int) (particle.x * field_density));
                int field_y = FastMath.toRange(0, field_h - 1, (int) (particle.y * field_density));

                particle.color = randomColor ? col_field[field_x][field_y] : fgColor;

                if (particle.isPathReady()) {
                    g2.setColor(particle.color);
                    g2.draw(particle.getPath());
                }
                particle.path = new ArrayList<>();
                particle.path.add(new float[]{particle.lastX, particle.lastY});
            }

            @Override
            protected boolean isParticleDead(FlowFieldParticle particle) {
                return !bounds.contains(particle.x, particle.y);
            }

            @Override
            protected void updateParticle(FlowFieldParticle particle) {
                int field_x = FastMath.toRange(0, field_w - 1, (int) (particle.x * field_density));
                int field_y = FastMath.toRange(0, field_h - 1, (int) (particle.y * field_density));

                float vx = particle.vx;
                float vy = particle.vy;

                float value = initTheta + (float) (noise.turbulence3(field_x / zoom / field_density, field_y / zoom / field_density, z, turbulence) * PI);
                physicsMode.updateParticle(particle, (float) (force * FastMath.cos(value)), (float) (force * FastMath.sin(value)));

//                physicsMode.updateParticle(particle, cos_field[field_x][field_y], sin_field[field_x][field_y]);

                if (particle.vx * particle.vx + particle.vy * particle.vy > maximumVelocity) {
                    particle.vx = vx;
                    particle.vy = vy;
                }

                particle.update();

            }
        };

        for (int i = 0; i < futures.length; i++) {
            int finalI = i;

            futures[i] = ThreadPool.submit(() -> {

                for (int j = 0; j < iteration_count; j++)
                    system.step(finalI);

                for (FlowFieldParticle particle : system.group(finalI).getParticles()) {
                    if (particle.isPathReady())
                        g2.draw(particle.getPath());
                }
            });
        }

        ThreadPool.waitFor(futures, pt);
        pt.finished();

        if (showFlowVectors) {
            for(int i = 0; i < field_w; i++)
                for (int j = 0; j < field_h; j++)
                    field[i][j] /= 100;
            fillImageWithFieldPointArrows(field_density, g2, field_w, field_h, field);
        }

        return dest;
    }

    private void fillImageWithFieldPointArrows(float field_density, Graphics2D g2, int field_w, int field_h, float[][] field) {

        Shape unitArrow = AffineTransform.getScaleInstance(25, 25).createTransformedShape(Shapes.createUnitArrow());

        g2.setColor(Color.RED);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int f;
        if (1 / field_density > 31) f = 1;
        else f = (int) (31 * field_density);

        for (int i = 0; i < field_w; i += f) {
            for (int j = 0; j < field_h; j += f) {

                int x = (int) (i / field_density);
                int y = (int) (j / field_density);

                AffineTransform transform = AffineTransform.getTranslateInstance(x, y);
                transform.rotate(field[i][j]);
                g2.fill(transform.createTransformedShape(unitArrow));
            }
        }
    }


    private static class FlowFieldParticle extends Particle {
        float ax, ay;
        public ArrayList<float[]> path;
        public Color color;
        public float tolerance;
        public float curviness;
        public float mass;

        public FlowFieldParticle(float tolerance, float curviness, float massRandom) {
            this.tolerance = tolerance;
            this.curviness = curviness;
            mass = (float) FastMath.random() + 1;
            mass *= massRandom;
        }

        public void update() {
            if (anyDisplacementInXOrYIsGreaterThanOne()) {
                path.add(new float[]{x, y});
                lastX = x;
                lastY = y;
            }
        }

        private boolean anyDisplacementInXOrYIsGreaterThanOne() {
            return FastMath.abs(lastX - x) > tolerance || FastMath.abs(lastY - y) > tolerance;
        }

        public boolean isPathReady() {
            return path != null && path.size() >= 3;
        }

        public Shape getPath() {
            return Shapes.smoothConnect(path.toArray(value -> new float[value][2]), curviness);
        }
    }

}
