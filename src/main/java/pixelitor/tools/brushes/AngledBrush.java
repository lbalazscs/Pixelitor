package pixelitor.tools.brushes;

import java.awt.Graphics2D;

public interface AngledBrush extends Brush {
    /**
     * Similar to drawPoint, but draws rotated with the given angle
     *
     * @param g
     * @param x        the x of the mouse event (NOT translated with the radius)
     * @param y        the y of the mouse event (NOT translated with the radius)
     * @param radius
     * @param theta
     */
    void drawPointWithAngle(Graphics2D g, int x, int y, int radius, double theta);
}
