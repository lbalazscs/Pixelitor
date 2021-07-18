package pixelitor.filters;

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pixelitor.ThreadPool;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.particles.Particle;
import pixelitor.particles.ParticleSystem;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

public class FlowFields extends ParametrizedFilter {

    public static final String NAME = "Flow Fields";

    private static final int PAD = 100;
    private static final int GROUP_COUNT = 20;
    private static final int ITERATION_LENGTH = 20;

    private final RangeParam iterationsParam = new RangeParam("Iterations", 0, 100, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam particlesParam = new RangeParam("Particle Count", 0, 100, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam fieldDensityParam = new RangeParam("Field Density (millis)", 0, 50, 1000);
    private final RangeParam noiseDensityParam = new RangeParam("Noise Density (millis)", 0, 100, 1000);
    private final RangeParam radiusParam = new RangeParam("Particle Radius", 0, 5, 100);
    private final RangeParam displaceParam = new RangeParam("Displacement (millis)", 0, 100, 100000);
    private final ColorParam colorParam = new ColorParam("Particle Color", new Color(0, 0, 0, 0.3f), ColorParam.TransparencyPolicy.FREE_TRANSPARENCY);

    private final BooleanParam showFlowVectors = new BooleanParam("Flow Vectors", false);

    public FlowFields() {
        super(false);

        setParams(
                iterationsParam,
                particlesParam,
                fieldDensityParam,
                noiseDensityParam,
                radiusParam,
                displaceParam,
                colorParam,
                showFlowVectors
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        int iteration_count = iterationsParam.getValue();
        int particle_count = particlesParam.getValue();
        float field_density = fieldDensityParam.getValue() * 0.001f;
        float noise_density = noiseDensityParam.getValue() * 0.001f;
        boolean view_flow_vectors = showFlowVectors.isChecked();
        int radius = radiusParam.getValue();
        float displace = displaceParam.getValue() * 0.001f;
        Color color = colorParam.getColor();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        int w = dest.getWidth();
        int h = dest.getHeight();
        Random r = ThreadLocalRandom.current();
        Graphics2D g2 = dest.createGraphics();

        int field_w = (int) (w * field_density) + 1;
        int field_h = (int) (h * field_density) + 1;

//        var pt = new StatusBarProgressTracker(NAME, GROUP_COUNT);

        Rectangle bounds = new Rectangle(-PAD, -PAD, w + PAD * 2, h + PAD * 2);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        float[][] field = new float[field_w][field_h];
        for (int i = 0; i < field_w; i++)
            for (int j = 0; j < field_h; j++)
                field[i][j] = (float) (Noise.noise2(i * noise_density, j * noise_density) * FastMath.PI);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        g2.setColor(color);

//        ParticleSystem system = new ParticleSystem(
//                GROUP_COUNT,
//                particle_count / 10,
//                position -> position.setLocation(w * r.nextFloat(), h * r.nextFloat()),
//                particle -> {
//                    int field_x = FastMath.toRange(0, field_w - 1, (int) (particle.position.x * field_density));
//                    int field_y = FastMath.toRange(0, field_h - 1, (int) (particle.position.y * field_density));
//
//                    particle.update(displace, field[field_x][field_y]);
//
//                    g2.fillRect(particle.getX(), particle.getY(), radius, radius);
//                },
//                new Rectangle(-PAD, -PAD, w + PAD * 2, h + PAD * 2)
//        );

        ParticleSystem<SimpleParticle> system = new ParticleSystem<>(GROUP_COUNT, particle_count / GROUP_COUNT) {

            @Override
            protected SimpleParticle newParticle() {
                return new SimpleParticle();
            }

            @Override
            protected void initializeParticle(SimpleParticle particle) {
                particle.lastX = particle.x = bounds.x + bounds.width * r.nextFloat();
                particle.lastY = particle.y = bounds.y + bounds.height * r.nextFloat();
            }

            @Override
            protected boolean isParticleDead(SimpleParticle particle) {
                return !bounds.contains(particle.x, particle.y);
            }

            @Override
            protected void updateParticle(SimpleParticle particle) {
                int field_x = FastMath.toRange(0, field_w - 1, (int) (particle.x * field_density));
                int field_y = FastMath.toRange(0, field_h - 1, (int) (particle.y * field_density));
                float dirn = field[field_x][field_y];

//                particle.ax = (float) (displace * FastMath.cos(dirn));
//                particle.ay = (float) (displace * FastMath.sin(dirn));
                particle.vx = (float) (displace * FastMath.cos(dirn));
                particle.vy = (float) (displace * FastMath.sin(dirn));
//                particle.vx += particle.ax;
//                particle.vy += particle.ay;

                particle.lastX = particle.x;
                particle.lastY = particle.y;

                particle.x += particle.vx;
                particle.y += particle.vy;

//                g2.fillRect((int) particle.y, (int) particle.x, radius, radius);
                g2.drawLine((int) particle.lastX, (int) particle.lastY, (int) particle.x, (int) particle.y);

            }

        };

        Future<?>[] futures = new Future[iteration_count / ITERATION_LENGTH];
        var pt = new StatusBarProgressTracker(NAME, futures.length);

        for (int i = 0; i < futures.length; i++) {
            futures[i] = ThreadPool.submit(() -> {
                for (int j = 0; j < ITERATION_LENGTH; j++)
                    system.step();
                pt.unitDone();
            });
        }

        ThreadPool.waitFor(futures, pt);

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

        pt.finished();
        return dest;
    }

    private static class SimpleParticle implements Particle {
        public float x, y, lastX, lastY, vx, vy, ax, ay;
    }

}
