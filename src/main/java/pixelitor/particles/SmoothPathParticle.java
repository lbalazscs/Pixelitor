package pixelitor.particles;

import pixelitor.utils.Shapes;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public abstract class SmoothPathParticle extends Particle {

    private final ArrayList<Point2D> pathPoints;
    private Graphics2D g2 = null;
    protected Graphics2D[] gc = null;

    public SmoothPathParticle(Graphics2D g2) {
        this.pathPoints = new ArrayList<>();
        this.g2 = g2;
    }

    public SmoothPathParticle(Graphics2D[] gc) {
        this.pathPoints = new ArrayList<>();
        this.gc = gc;
    }

    public void addPoint(Point2D point) {
        pathPoints.add(point);
    }

    @Override
    public void flush() {
        if (isPathReady()) {
            Graphics2D g2 = getGraphics();
            g2.setColor(color);
            g2.draw(getPath());
        }
        pathPoints.clear();
    }

    private boolean isPathReady() {
        return pathPoints.size() >= 3;
    }

    private Shape getPath() {
        return Shapes.smoothConnect(pathPoints, 0.5f);
    }

    protected Graphics2D getGraphics() {
        if (g2 != null) {
            return g2;
        } else if (gc != null) {
            if (groupIndex != -1) {
                return g2 = gc[groupIndex];
            }
        }
        throw new IllegalStateException("Either Graphics2D or it's array has to be non-null!");
    }

}
