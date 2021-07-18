package pixelitor.filters;

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

public class FlowFields_dev extends ParametrizedFilter {

    public static final String NAME = "Flow Fields test";

    private final RangeParam iterationsParam = new RangeParam("Iterations", 0, 100, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam particlesParam = new RangeParam("Particle Count", 0, 100, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam fieldDensityParam = new RangeParam("Field Density (millis)", 0, 50, 1000);
    private final RangeParam noiseDensityParam = new RangeParam("Noise Density (millis)", 0, 100, 1000);
    private final RangeParam radiusParam = new RangeParam("Particle Radius", 0, 5, 100);
    private final RangeParam displaceParam = new RangeParam("Displacement (millis)", 0, 100, 1000);
    private final ColorParam colorParam = new ColorParam("Particle Color", new Color(0, 0, 0, 0.3f), ColorParam.TransparencyPolicy.FREE_TRANSPARENCY);

    private final BooleanParam showFlowVectors = new BooleanParam("Flow Vectors", false);

    public FlowFields_dev() {
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

        int w = dest.getWidth();
        int h = dest.getHeight();
        int iteration_count = iterationsParam.getValue();
        int particle_count = particlesParam.getValue();
        float field_density = fieldDensityParam.getValue() * 0.001f; //0.05f; // every 10 pixel
        float noise_density = noiseDensityParam.getValue() * 0.001f;// 0.1f;
        boolean view_flow_vectors = showFlowVectors.isChecked();
        int radius = radiusParam.getValue();
        float displace = displaceParam.getValue() * 0.001f;
        Color color = colorParam.getColor();


        Random r = ThreadLocalRandom.current();
        Graphics2D g2 = dest.createGraphics();

        int field_w = (int) (w * field_density) + 1;
        int field_h = (int) (h * field_density) + 1;

        float[][] field = new float[field_w][field_h];
        for (int i = 0; i < field_w; i++)
            for (int j = 0; j < field_h; j++)
                field[i][j] = (float) (Noise.noise2(i * noise_density, j * noise_density) * FastMath.PI);

        Particle[] particles = new Particle[particle_count];

        for (int i = 0; i < particles.length; i++)
            particles[i] = new Particle(new Point2D.Float(w * r.nextFloat(), h * r.nextFloat()));

        g2.setColor(color);
        for (int iteration = 0; iteration < iteration_count; iteration++) {

            for (int i = 0; i < particle_count; i++) {
                Particle par = particles[i];

                int field_x = FastMath.toRange(0, field_w - 1, (int) (par.position.x * field_density));
                int field_y = FastMath.toRange(0, field_h - 1, (int) (par.position.y * field_density));

                par.update(displace, field[field_x][field_y]);

                g2.fillOval(par.getX(), par.getY(), radius, radius);

            }

        }

        if (view_flow_vectors) {
            g2.setColor(Color.RED);
            float field_number = 1 / field_density;
            for (int i = 0; i < field_w; i++) {
                for (int j = 0; j < field_h; j++) {
                    int x = (int) (i * field_number);
                    int y = (int) (j * field_number);
                    g2.fillRect(x, y, 2, 2);
                    g2.drawLine(x, y, x + (int) (20 * FastMath.cos(field[i][j])), y + (int) (20 * FastMath.sin(field[i][j])));
                }
            }
        }

        return dest;
    }

    public static class Particle {
        Point2D.Float position = new Point2D.Float();
        Point2D.Float velocity = new Point2D.Float();
        float dirn;
        float spd = 2;
        Point2D.Float acceleration = new Point2D.Float();

        public Particle(Point2D.Float position) {
            this.position = position;
        }

        public void update(float mag, float dirn) {
            if (this.dirn != dirn) {
                acceleration.setLocation(mag * (FastMath.cos(dirn)), mag * (FastMath.sin(dirn)));
                this.dirn = dirn;
            }

            velocity.x = FastMath.toRange(-spd, spd, velocity.x + acceleration.x);
            velocity.y = FastMath.toRange(-spd, spd, velocity.y + acceleration.y);

            position.x += velocity.x;
            position.y += velocity.y;
        }

        public int getX() {
            return (int) position.x;
        }

        public int getY() {
            return (int) position.y;
        }
    }

}
