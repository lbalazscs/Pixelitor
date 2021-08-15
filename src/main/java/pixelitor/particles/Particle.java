package pixelitor.particles;

import pixelitor.utils.Vector2D;

import java.awt.Color;
import java.awt.geom.Point2D;

public abstract class Particle {

    public Point2D pos, las_pos;
    public Vector2D vel;
    public Color color;
    public int iterationIndex;

    public abstract void flush();

    public abstract void reset();

    public abstract boolean isDead();

    public abstract void update();

}
