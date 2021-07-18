package pixelitor.filters;

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pixelitor.ThreadPool;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.particles.Particle;
import pixelitor.particles.ParticleSystem;
import pixelitor.utils.StatusBarProgressTracker;
import pixelitor.utils.VanishingStroke;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

public class FlowField extends ParametrizedFilter {

    public static final String NAME = "Flow Field";

    private static final int PAD = 100;
    private static final int GROUP_COUNT = 128;

    private final RangeParam iterationsParam = new RangeParam("Iterations", 0, 100, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam particlesParam = new RangeParam("Particle Count", 0, 100, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam lifeParam = new RangeParam("Particle Life", 0, 1000, 5000);
    private final RangeParam fieldDensityParam = new RangeParam("Field Density (millis)", 0, 50, 1000);
    private final RangeParam noiseDensityParam = new RangeParam("Noise Density (millis)", 0, 100, 1000);
    private final StrokeParam strokeParam = new StrokeParam("Stroke");
    private final RangeParam displaceParam = new RangeParam("Displacement", 0, 15, 100);
    private final ColorParam colorParam = new ColorParam("Particle Color", new Color(0, 0, 0, 0.01f), ColorParam.TransparencyPolicy.FREE_TRANSPARENCY);

    private final BooleanParam showFlowVectors = new BooleanParam("Flow Vectors", false);

    public FlowField() {
        super(false);

        setParams(
                iterationsParam,
                particlesParam,
                lifeParam,
                fieldDensityParam,
                noiseDensityParam,
                strokeParam,
                displaceParam,
                colorParam,
                showFlowVectors
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        int iteration_count = iterationsParam.getValue();
        int particle_count = particlesParam.getValue();
        int life = lifeParam.getValue();
        float field_density = fieldDensityParam.getValue() * 0.001f;
        float noise_density = noiseDensityParam.getValue() * 0.001f;
        Stroke stroke = strokeParam.createStroke();
        float displace = displaceParam.getValue();
        Color color = colorParam.getColor();
        boolean view_flow_vectors = showFlowVectors.isChecked();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        int w = dest.getWidth();
        int h = dest.getHeight();

        Random r = ThreadLocalRandom.current();

        Graphics2D g2 = dest.createGraphics();
//        g2.setStroke(stroke);
        g2.setStroke(new VanishingStroke());

        int field_w = (int) (w * field_density) + 1;
        int field_h = (int) (h * field_density) + 1;

        Rectangle bounds = new Rectangle(-PAD, -PAD, w + PAD * 2, h + PAD * 2);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        float PI = (float) FastMath.PI;
        float[][] field = new float[field_w][field_h];
        for (int i = 0; i < field_w; i++)
            for (int j = 0; j < field_h; j++)
                field[i][j] = Noise.noise2(i * noise_density, j * noise_density) * PI;

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        g2.setColor(color);

        ParticleSystem<SimpleParticle> system = new ParticleSystem<>(GROUP_COUNT, FastMath.ceilToInt(particle_count * 1d / GROUP_COUNT)) {

            @Override
            protected SimpleParticle newParticle() {
                SimpleParticle simpleParticle = new SimpleParticle();
                simpleParticle.life = life;
                return simpleParticle;
            }

            @Override
            protected void initializeParticle(SimpleParticle particle) {
                if(particle.path!=null)
                    g2.draw(particle.path);

                particle.lastX = particle.x = bounds.x + bounds.width * r.nextFloat();
                particle.lastY = particle.y = bounds.y + bounds.height * r.nextFloat();
                particle.path = new GeneralPath();
                particle.path.moveTo(particle.lastX, particle.lastY);
            }

            @Override
            protected boolean isParticleDead(SimpleParticle particle) {
                return !bounds.contains(particle.x, particle.y) || life < 0;
            }

            @Override
            protected void updateParticle(SimpleParticle particle) {
                int field_x = FastMath.toRange(0, field_w - 1, (int) (particle.x * field_density));
                int field_y = FastMath.toRange(0, field_h - 1, (int) (particle.y * field_density));
                float dirn = field[field_x][field_y];

                particle.update(displace, dirn);

//                g2.drawLine((int) particle.lastX, (int) particle.lastY, (int) particle.x, (int) particle.y);

                particle.life--;
            }

        };

        Future<?>[] futures = new Future[GROUP_COUNT];
        var pt = new StatusBarProgressTracker(NAME, futures.length);

        for (int i = 0; i < futures.length; i++) {
            int finalI = i;
            futures[i] = ThreadPool.submit(() -> {
                for (int j = 0; j < iteration_count; j++)
                    system.step(finalI);
            });
        }

        ThreadPool.waitFor(futures, pt);
        pt.finished();

        if (view_flow_vectors) {
            g2.setColor(Color.RED);
            int displacer = displaceParam.getValue();
            for (int i = 0; i < field_w; i++) {
                for (int j = 0; j < field_h; j++) {
                    int x = (int) (i / field_density);
                    int y = (int) (j / field_density);
                    g2.fillRect(x, y, 2, 2);
                    g2.drawLine(x, y, x + (int) (displacer * FastMath.cos(field[i][j])), y + (int) (displacer * FastMath.sin(field[i][j])));
                }
            }
        }

        return dest;
    }

    private static class SimpleParticle implements Particle {
        public float x, y, lastX, lastY, vx, vy;
        public GeneralPath path;
        public int life;

        public void update(float displace, float dirn) {
            vx = (float) (displace * FastMath.cos(dirn));
            vy = (float) (displace * FastMath.sin(dirn));

            lastX = x;
            lastY = y;

            x += vx;
            y += vy;

            path.lineTo(x, y);
        }
    }

}
