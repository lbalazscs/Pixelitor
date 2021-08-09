package pixelitor.particles;

import pixelitor.utils.Shapes;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public abstract class SmoothPathParticle extends Particle {

    private final ArrayList<Point2D> pathPoints;
    protected final Graphics2D g2;

    public SmoothPathParticle(Graphics2D g2) {
        this.pathPoints = new ArrayList<>();
        this.g2 = g2;
    }

    public void addPoint(Point2D point) {
        pathPoints.add(point);
    }

    @Override
    public void flush() {
        if (isPathReady()) {
            g2.setColor(color);
            g2.draw(getPath());
        }
        pathPoints.clear();
    }

    private boolean isPathReady() {
        return pathPoints.size() >= 3;
    }

    private Shape getPath() {
        return Shapes.smoothConnect(pathPoints);
    }
}
