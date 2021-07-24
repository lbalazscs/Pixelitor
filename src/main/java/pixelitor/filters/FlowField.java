package pixelitor.filters;

import net.jafama.FastMath;
import pd.OpenSimplex2F;
import pixelitor.ThreadPool;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.particles.Particle;
import pixelitor.particles.ParticleSystem;
import pixelitor.utils.ReseedSupport;
import pixelitor.utils.Shapes;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
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

    private final RangeParam iterationsParam = new RangeParam("Iterations", 1, 100, 10000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam particlesParam = new RangeParam("Particle Count", 1, 1000, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam zoomParam = new RangeParam("Zoom", 1000, 40000, 100000);
    private final StrokeParam strokeParam = new StrokeParam("Stroke");

    private final ColorParam backgroundColorParam = new ColorParam("Background Color", new Color(0, 0, 0, 1f), ColorParam.TransparencyPolicy.FREE_TRANSPARENCY);
    private final ColorParam foregroundColorParam = new ColorParam("Particle Color", new Color(1, 1, 1, 0.12f), ColorParam.TransparencyPolicy.FREE_TRANSPARENCY);
    private final BooleanParam randomColorParam = new BooleanParam("Random Color", false);


    private final RangeParam qualityParam = new RangeParam("Quality", 1, 75, 100);
    private final LogZoomParam forceParam = new LogZoomParam("Force", 1, 320, 400);
    private final RangeParam turbulenceParam = new RangeParam("Turbulence", 1, 1, 8);
    private final BooleanParam showFlowVectors = new BooleanParam("Flow Vectors", false);
    private final DialogParam advancedParam = new DialogParam("Advanced", qualityParam, forceParam, turbulenceParam, showFlowVectors);

    public FlowField() {
        super(false);

        setParams(
                iterationsParam,
                particlesParam,
                zoomParam,
                strokeParam,
                backgroundColorParam,
                foregroundColorParam,
                randomColorParam,
                advancedParam
        ).withAction(ReseedSupport.createAction());

        iterationsParam.setToolTip("Change filament thickness");
        particlesParam.setToolTip("Number of filaments");
        zoomParam.setToolTip("Adjust the zoom");
        strokeParam.setToolTip("Adjust the stroke style");
        randomColorParam.setToolTip("Creates new colors based on choosen color's hue and transparency.");

        qualityParam.setToolTip("Smoothness of filament");
        forceParam.setToolTip("Stroke Length");
        turbulenceParam.setToolTip("Adjust the varience provided by Noise.");
        showFlowVectors.setToolTip("View direction of flow");

    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        int iteration_count = iterationsParam.getValue();
        int particle_count = particlesParam.getValue();
        float zoom = zoomParam.getValue() * 0.01f;
        Stroke stroke = strokeParam.createStroke();
        Color bgColor = backgroundColorParam.getColor();
        Color fgColor = foregroundColorParam.getColor();
        boolean randomColor = randomColorParam.isChecked();

        float quality = (qualityParam.getValueAsFloat() - 1) / 99;
        float force = (float) forceParam.getZoomRatio();
        int turbulence = turbulenceParam.getValue();
        boolean showFlowVectors = this.showFlowVectors.isChecked();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        int w = dest.getWidth();
        int h = dest.getHeight();

        Random r = ReseedSupport.reInitialize();
        OpenSimplex2F noise = ReseedSupport.getSimplexNoise();

        Graphics2D g2 = dest.createGraphics();
        g2.setStroke(stroke);
        Colors.fillWith(bgColor, g2, w, h);

        int field_w = (int) (w * quality + 1);
        float field_density = field_w * 1f / w;
        int field_h = (int) (h * field_density);

        Rectangle bounds = new Rectangle(-PAD, -PAD, w + PAD * 2, h + PAD * 2);

        int groupCount = FastMath.ceilToInt(particle_count * 1d / PARTICLES_PER_GROUP);
        Future<?>[] futures = new Future[groupCount];
        var pt = new StatusBarProgressTracker(NAME, futures.length + 1);
        pt.unitDone();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        float PI = (float) FastMath.PI;
        float initTheta = r.nextFloat() * 2 * PI;

        float[][] field = null;
        if (showFlowVectors)
            field = new float[field_w][field_h];

        float[][] cos_field = new float[field_w][field_h];
        float[][] sin_field = new float[field_w][field_h];

        // TODO: verify that it's better than creating a `new Color[field_w][field_h] when not needed`.
        Color[][] col_field = ((Supplier<Color[][]>) () -> {
            if (randomColor)
                return new Color[field_w][field_h];
            return null;
        }).get();

        float[] hsb_col = null;
        if (randomColor)
            hsb_col = Colors.toHSB(fgColor);

        for (int i = 0; i < field_w; i++) {
            for (int j = 0; j < field_h; j++) {
                float value = initTheta + (float) (noise.turbulence2(i / zoom / field_density, j / zoom / field_density, turbulence) * PI);

                if (showFlowVectors)
                    field[i][j] = value;

                cos_field[i][j] = (float) (force * FastMath.cos(value));
                sin_field[i][j] = (float) (force * FastMath.sin(value));

                if (randomColor) {
                    col_field[i][j] = new Color(Colors.HSBAtoARGB(hsb_col, fgColor.getAlpha()), true);
                    hsb_col[0] = (hsb_col[0] + GOLDEN_RATIO_CONJUGATE) % 1;
                }
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        g2.setColor(fgColor);

        ParticleSystem<SimpleParticle> system = new ParticleSystem<>(groupCount, PARTICLES_PER_GROUP) {

            @Override
            protected SimpleParticle newParticle() {
                return new SimpleParticle();
            }

            @Override
            protected void initializeParticle(SimpleParticle particle) {

                particle.lastX = particle.x = bounds.x + bounds.width * r.nextFloat();
                particle.lastY = particle.y = bounds.y + bounds.height * r.nextFloat();

                int field_x = FastMath.toRange(0, field_w - 1, (int) (particle.x * field_density));
                int field_y = FastMath.toRange(0, field_h - 1, (int) (particle.y * field_density));

                particle.color = randomColor ? col_field[field_x][field_y] : fgColor;

                if (particle.path != null) {
                    g2.setColor(particle.color);
                    g2.draw(particle.path);
                }
                particle.path = new GeneralPath();
                particle.path.moveTo(particle.lastX, particle.lastY);
            }

            @Override
            protected boolean isParticleDead(SimpleParticle particle) {
                return !bounds.contains(particle.x, particle.y);
            }

            @Override
            protected void updateParticle(SimpleParticle particle) {
                int field_x = FastMath.toRange(0, field_w - 1, (int) (particle.x * field_density));
                int field_y = FastMath.toRange(0, field_h - 1, (int) (particle.y * field_density));

                particle.update(cos_field[field_x][field_y], sin_field[field_x][field_y]);
            }

        };

        for (int i = 0; i < futures.length; i++) {
            int finalI = i;

            futures[i] = ThreadPool.submit(() -> {

                for (int j = 0; j < iteration_count; j++)
                    system.step(finalI);

                for (SimpleParticle particle : system.group(finalI).getParticles()) {
                    if (particle.path != null)
                        g2.draw(particle.path);
                }
            });
        }

        ThreadPool.waitFor(futures, pt);
        pt.finished();

        if (showFlowVectors)
            fillImageWithFieldPointArrows(field_density, g2, field_w, field_h, field);

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


    private static class SimpleParticle extends Particle {
        public GeneralPath path;
        public Color color;

        public void update(float vx, float vy) {
            lastX = x;
            lastY = y;

            x += vx;
            y += vy;

            path.lineTo(x, y);
        }
    }

}
