package pixelitor.layers;

import java.awt.Graphics2D;

/**
 * A temporary drawing layer used by the brush and gradient tools.
 * It allows these tools to use blending modes.
 */
public interface TmpDrawingLayer {
    Graphics2D getGraphics();

    int getWidth();

    int getHeight();

    void dispose();

    void paintLayer(Graphics2D g, int translationX, int translationY);
}
