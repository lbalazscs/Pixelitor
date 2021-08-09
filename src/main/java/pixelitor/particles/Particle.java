package pixelitor.particles;

import java.awt.*;
import java.awt.geom.Point2D;

public abstract class Particle {

    public Point2D pos, las_pos, vel;
    public Color color;

    public abstract void flush();

    public abstract void reset();

    public abstract boolean isDead();

    public abstract void update();

}
