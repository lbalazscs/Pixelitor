package pixelitor.filters;

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pd.OpenSimplex2F;
import pixelitor.ThreadPool;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.particles.Particle;
import pixelitor.particles.ParticleSystem;
import pixelitor.utils.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.DoubleAdder;
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
        }, FORCE_MODE_JOLT("Jolt") {
            @Override
            void updateParticle(FlowFieldParticle particle, float dx, float dy) {
                particle.x += particle.vx += particle.ax += dx;
                particle.y += particle.vy += particle.ay += dy;
            }
        }, FORCE_MODE_VELOCITY_AND_NOISE_BASED_RANDOMNESS("Thicken") {
            @Override
            void updateParticle(FlowFieldParticle particle, float dx, float dy) {
                particle.x += dx + Noise.noise2(dx, dy) * 10;
                particle.y += dy + Noise.noise2(dx, dy) * 10;
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

    private final EnumParam<PhysicsMode> physicsModeParam = new EnumParam<>("Field Effect", PhysicsMode.class);
    //    private final RangeParam massRandomnessParam = new RangeParam("Mass Randomness", 0, 10, 100);
    private final RangeParam velocityCapParam = new RangeParam("Maximum Velocity", 1, 100000, 6300000);
    private final LogZoomParam forceParam = new LogZoomParam("Force", 1, 320, 400);
    private final RangeParam varianceParam = new RangeParam("Variance", 1, 20, 100);
    private final DialogParam physicsParam = new DialogParam("Physics", physicsModeParam, velocityCapParam, forceParam, varianceParam);

    private final ColorParam backgroundColorParam = new ColorParam("Background Color", new Color(0, 0, 0, 1f), ColorParam.TransparencyPolicy.FREE_TRANSPARENCY);
    private final ColorParam foregroundColorParam = new ColorParam("Particle Color", new Color(1, 1, 1, 0.12f), ColorParam.TransparencyPolicy.FREE_TRANSPARENCY);
    private final RangeParam colorRandomnessParam = new RangeParam("Color Randomness (%)", 0, 0, 100);

    private final RangeParam qualityParam = new RangeParam("Smoothness (%)", 1, 75, 100);
    private final RangeParam iterationsParam = new RangeParam("Iterations (Makes simulation slow!!)", 1, 100, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam turbulenceParam = new RangeParam("Turbulence", 1, 1, 8);
    private final RangeParam zFactorParam = new RangeParam("Wind", 0, 0, 200);
    private final RangeParam drawToleranceParam = new RangeParam("Tolerance", 0, 30, 200);
    private final DialogParam advancedParam = new DialogParam("Advanced", qualityParam, iterationsParam, turbulenceParam, zFactorParam, drawToleranceParam);

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
        turbulenceParam.setToolTip("Adjust the variance provided by Noise.");
        zFactorParam.setToolTip("Spreads away the flow");
        drawToleranceParam.setToolTip("Require longer fibres to be drawn.");

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
        float maximumVelocity = velocityCapParam.getValue() * velocityCapParam.getValue() / 10000f;
        float force = (float) forceParam.getZoomRatio();
        float variance = varianceParam.getValue() / 10f;

        float quality = qualityParam.getValueAsFloat() / 99 * 400 / zoom;
        int iteration_count = iterationsParam.getValue();
        int turbulence = turbulenceParam.getValue();
        float zFactor = zFactorParam.getValueAsFloat() / 10000;
        int tolerance = drawToleranceParam.getValue();

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

        ArrayList<DoubleAdder> zFactors = new ArrayList<>(groupCount);
        for (int i = 0; i < groupCount; i++)
            zFactors.add(new DoubleAdder());

        ParticleSystem<FlowFieldParticle> system = new ParticleSystem<>(groupCount, PARTICLES_PER_GROUP, particle_count) {

            @Override
            public void step(int idx) {
                super.step(idx);
                zFactors.get(idx).add(zFactor);
            }

            @Override
            protected FlowFieldParticle newParticle() {
                return new FlowFieldParticle(tolerance);
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
                particle.path.add(new Point2D.Float(particle.lastX, particle.lastY));
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

                float value = initTheta + (float) (noise.turbulence3(field_x / zoom / field_density, field_y / zoom / field_density, zFactors.get(particle.groupIndex).doubleValue(), turbulence) * PI);
                physicsMode.updateParticle(particle, (float) (force * FastMath.cos(value)), (float) (force * FastMath.sin(value)));

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

        return dest;
    }

    private static class FlowFieldParticle extends Particle {
        float ax, ay;
        public ArrayList<Point2D> path;
        public Color color;
        public float tolerance;

        public FlowFieldParticle(float tolerance) {
            this.tolerance = tolerance;
        }

        public void update() {
            if (anyDisplacementInXOrYIsGreaterThanOne()) {
                path.add(new Point2D.Float(x, y));
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
            return Shapes.smoothConnect(path, 0.5f);
        }
    }
}
