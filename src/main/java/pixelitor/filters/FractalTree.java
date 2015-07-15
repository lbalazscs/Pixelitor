package pixelitor.filters;

import net.jafama.FastMath;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ReseedSupport;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Renders a fractal tree
 */
public class FractalTree extends FilterWithParametrizedGUI {
    public static final Color BROWN = new Color(140, 100, 73);
    public static final Color GREEN = new Color(31, 125, 42);

    private RangeParam iterations = new RangeParam("Age (Iterations)", 1, 15, 10);
    private RangeParam angle = new RangeParam("Angle", 1, 45, 20);
    private RangeParam randomnessParam = new RangeParam("Randomness", 0, 10, 5);
    private RangeParam width = new RangeParam("Width (%)", 100, 300, 100);
    private RangeParam zoom = new RangeParam("Zoom", 1, 20, 10);
    private RangeParam curvedness = new RangeParam("Curvedness", 0, 50, 10);
    private RangeParam gravityParam = new RangeParam("Gravity", -50, 50, 0);
    private RangeParam windParam = new RangeParam("Wind", -50, 50, 0);

    GradientParam colors = new GradientParam("Colors",
            new float[]{0.25f, 0.75f},
            new Color[]{BROWN, GREEN}, true);

    public FractalTree() {
        super("Fractal Tree", true, false);
        setParamSet(new ParamSet(
                iterations,
                zoom,
                randomnessParam,
                curvedness,
                angle,
                gravityParam,
                windParam,
                width,
                colors
        ).withAction(ReseedSupport.createAction()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        ReseedSupport.reInitialize();
        Random rand = ReseedSupport.getRand();

        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float c = curvedness.getValueAsPercentage();
        if (rand.nextBoolean()) {
            c = -c;
        }
        drawTree(g, src.getWidth() / 2, src.getHeight(), 270, iterations.getValue(), rand, c);

        g.dispose();

        return dest;
    }

    private void drawTree(Graphics2D g, int x1, int y1, double angle, int depth, Random rand, float c) {
        if (depth == 0) {
            return;
        }
        c = -c; // change the direction of the curvature in each iteration

        angle = considerGravityAndWind(angle, depth);

        g.setStroke(new BasicStroke(depth * width.getValueAsPercentage(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        float where = ((float) depth) / iterations.getValue();
        int rgb = colors.getValue().getColor(1.0f - where);
        g.setColor(new Color(rgb));

        double angleRad = Math.toRadians(angle);
        int x2 = x1 + (int) (FastMath.cos(angleRad) * depth * calcRandomLength(rand));
        int y2 = y1 + (int) (FastMath.sin(angleRad) * depth * calcRandomLength(rand));
        connectPoints(g, x1, y1, x2, y2, c);

        int split = this.angle.getValue();
        drawTree(g, x2, y2, angle - split + calcAngleRandomness(rand), depth - 1, rand, c);
        drawTree(g, x2, y2, angle + split + calcAngleRandomness(rand), depth - 1, rand, c);
    }

    private double considerGravityAndWind(double angle, int depth) {
        int gravity = gravityParam.getValue();
        int wind = windParam.getValue();


        if (gravity != 0 || wind != 0) {
            // make sure we have the angle in the range 0-360
            angle += 720;
            angle = angle % 360;

            // the lower the depth is (we are towards leaves),
            // the stronger the gravity and wind effect
            double effectStrength = (iterations.getValue() - depth) / 500.0;
            double gravityStrength = effectStrength * gravity;
            double windStrength = effectStrength * wind;

            if (angle < 90) {
                angle += (90 - angle) * gravityStrength;
                angle -= (angle / 90.0) * windStrength;
            } else if (angle < 180) {
                angle -= (angle - 90) * gravityStrength;
                angle -= (180 - angle) * windStrength;
            } else if (angle < 270) {
                angle -= (270 - angle) * gravityStrength;
                angle += (angle - 180) * windStrength;
            } else if (angle <= 360) {
                angle += (angle - 270) * gravityStrength;
                angle += (360 - angle) * windStrength;
            } else {
                throw new IllegalStateException("angle = " + angle);
            }
        }

        return angle;
    }

    private void connectPoints(Graphics2D g, int x1, int y1, int x2, int y2, float c) {
        if (c == 0) {
            g.drawLine(x1, y1, x2, y2);
        } else {
            Path2D.Double path = new Path2D.Double();
            path.moveTo(x1, y1);

            double dx = x2 - x1;
            double dy = y2 - y1;

            // center point
            double cx = x1 + dx / 2.0;
            double cy = y1 + dy / 2.0;

            // We calculate only one Bezier control point,
            // and use it for both.
            // The normal vector is -dy, dx.
            double ctrlX = cx - dy * c;
            double ctrlY = cy + dx * c;

            path.curveTo(ctrlX, ctrlY, ctrlX, ctrlY, x2, y2);
            g.draw(path);
        }
    }

    private int calcAngleRandomness(Random rand) {
        int randomness = randomnessParam.getValue();
        if (randomness == 0) {
            return 0;
        }
        int maxDeviation = 10;
        double randPercent = randomness / 10.0;
        int deviation = (int) (maxDeviation * randPercent);
        return (int) (-deviation + rand.nextDouble() * 2 * deviation);
    }

    private int calcRandomLength(Random rand) {
        int length = zoom.getValue();
        int randomness = randomnessParam.getValue();
        if (randomness == 0) {
            return length;
        }
        double randPercent = randomness / 10.0;
        int deviation = (int) (length * randPercent);
        int minLength = length - deviation;

        return (int) (minLength + 2 * deviation * rand.nextDouble());
    }
}