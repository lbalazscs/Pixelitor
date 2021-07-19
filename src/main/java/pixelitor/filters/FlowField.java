package pixelitor.filters;

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pixelitor.ThreadPool;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.particles.Particle;
import pixelitor.particles.ParticleSystem;
import pixelitor.utils.OpenSimplex2F;
import pixelitor.utils.Shapes;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

public class FlowField extends ParametrizedFilter {

    public static final String NAME = "Flow Field";

    private static final int PAD = 100;
    private static final int PARTICLES_PER_GROUP = 100;

    private static final int BG_BLACK = 1;
    private static final int BG_ORIGINAL = 2;
    private static final int BG_TRANSPARENT = 3;
    private static final int BG_TOOL = 4;

    private final RangeParam iterationsParam = new RangeParam("Iterations", 1, 100, 10000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam particlesParam = new RangeParam("Particle Count", 1, 100, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam lifeParam = new RangeParam("Particle Life", 1, 1000, 5000);
    private final RangeParam qualityParam = new RangeParam("Quality", 1, 500, 4000);
    private final RangeParam noiseDensityParam = new RangeParam("Scale", 1000, 40000, 100000);
    private final StrokeParam strokeParam = new StrokeParam("Stroke");
    private final LogZoomParam forceParam = new LogZoomParam("Force", 1, 320, 400);

    private final ColorParam backgroundColorParam = new ColorParam("Background Color", new Color(0, 0, 0, 1f), ColorParam.TransparencyPolicy.FREE_TRANSPARENCY);
    private final ColorParam foregroundColorParam = new ColorParam("Particle Color", new Color(1, 1, 1, 0.01f), ColorParam.TransparencyPolicy.FREE_TRANSPARENCY);

    private final BooleanParam showFlowVectors = new BooleanParam("Flow Vectors", false);

    public FlowField() {
        super(false);

        setParams(
                iterationsParam,
                particlesParam,
                lifeParam,
                qualityParam,
                noiseDensityParam,
                strokeParam,
                forceParam,
                backgroundColorParam,
                foregroundColorParam,
                showFlowVectors
        ).withAction(ReseedActions.reseedNoise());
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        int iteration_count = iterationsParam.getValue();
        int particle_count = particlesParam.getValue();
        int life = lifeParam.getValue();
        float field_density = qualityParam.getValue() * 0.001f;
        float noise_density = noiseDensityParam.getValue() * 0.01f;
        Stroke stroke = strokeParam.createStroke();
        float force = (float) forceParam.getZoomRatio();
        Color bgColor = backgroundColorParam.getColor();
        Color color = foregroundColorParam.getColor();
        boolean view_flow_vectors = showFlowVectors.isChecked();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        int w = dest.getWidth();
        int h = dest.getHeight();

        Random r = ThreadLocalRandom.current();
        OpenSimplex2F noise = new OpenSimplex2F(r.nextLong());

        Graphics2D g2 = dest.createGraphics();
        g2.setStroke(stroke);
        Colors.fillWith(bgColor, g2, w, h);

        int field_w = (int) (w * field_density) + 1;
        int field_h = (int) (h * field_density) + 1;

        Rectangle bounds = new Rectangle(-PAD, -PAD, w + PAD * 2, h + PAD * 2);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        float PI = (float) FastMath.PI;

        float[][] field = null;
        if (view_flow_vectors)
            field = new float[field_w][field_h];
        float[][] cos_field = new float[field_w][field_h];
        float[][] sin_field = new float[field_w][field_h];

        for (int i = 0; i < field_w; i++) {
            for (int j = 0; j < field_h; j++) {
                float value = (float) (noise.noise2(i / noise_density / field_density, j / noise_density / field_density) * PI);
                System.out.println(value);
                if (view_flow_vectors)
                    field[i][j] = value;
                cos_field[i][j] = (float) (force * FastMath.cos(value));
                sin_field[i][j] = (float) (force * FastMath.sin(value));
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        g2.setColor(color);

//        ParticleSystem<SimpleParticle> system = new ParticleSystem<>(1, particle_count) {
        ParticleSystem<SimpleParticle> system = new ParticleSystem<>(FastMath.ceilToInt(particle_count * 1d / PARTICLES_PER_GROUP), PARTICLES_PER_GROUP) {

            @Override
            protected SimpleParticle newParticle() {
                return new SimpleParticle();
            }

            @Override
            protected void initializeParticle(SimpleParticle particle) {
                particle.life = life;
                if (particle.path != null)
                    g2.draw(particle.path);

                particle.lastX = particle.x = bounds.x + bounds.width * r.nextFloat();
                particle.lastY = particle.y = bounds.y + bounds.height * r.nextFloat();

                particle.path = new GeneralPath();
                particle.path.moveTo(particle.lastX, particle.lastY);
            }

            @Override
            protected boolean isParticleDead(SimpleParticle particle) {
                return !bounds.contains(particle.x, particle.y) || particle.life <= 0;
            }

            @Override
            protected void updateParticle(SimpleParticle particle) {
                int field_x = FastMath.toRange(0, field_w - 1, (int) (particle.x * field_density));
                int field_y = FastMath.toRange(0, field_h - 1, (int) (particle.y * field_density));

                particle.update(cos_field[field_x][field_y], sin_field[field_x][field_y]);

                particle.life--;
            }

        };

        Future<?>[] futures = new Future[system.getGroupCount()];
        var pt = new StatusBarProgressTracker(NAME, futures.length);

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

        if (view_flow_vectors) {

            Shape unitArrow = AffineTransform.getScaleInstance(25, 25).createTransformedShape(Shapes.createUnitArrow());

            g2.setColor(Color.RED);

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
//                    g2.fillRect(x, y, 2, 2);
//                    g2.drawLine(x, y, x + (int) (displacer * FastMath.cos(field[i][j])), y + (int) (displacer * FastMath.sin(field[i][j])));
                }
            }
        }

        return dest;
    }

    private static class SimpleParticle extends Particle {
        public GeneralPath path;
        public int life;

        public void update(float vx, float vy) {
            lastX = x;
            lastY = y;

            x += vx;
            y += vy;

            path.lineTo(x, y);
        }
    }

}
