package pixelitor;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.Serializable;

/**
 * Represents the painting canvas.
 */
public class Canvas implements Serializable {
    private int width;
    private int height;

    private int zoomedWidth;
    private int zoomedHeight;

    private transient ImageComponent ic;

    // for consistency with Pixelitor 2.1.0
    private static final long serialVersionUID = -1459254568616232274L;


    /**
     * If a Composition is deserialized, then this object is also deserialized,
     * and later associated with the (transient!) ImageComponent
     * In the case of a new image, this object is first created in ImageComponent
     */
    public Canvas(ImageComponent ic, int width, int height) {
        this.width = width;
        this.height = height;
        this.ic = ic;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Rectangle getBounds() {
        return new Rectangle(0, 0, width, height);
    }

    public void updateSize(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;

        double viewScale = ic.getViewScale();
        zoomedWidth = (int) (viewScale * newWidth);
        zoomedHeight = (int) (viewScale * newHeight);

        ic.canvasSizeChanged();
    }

    public void updateForZoom(double viewScale) {
        zoomedWidth = (int) (viewScale * width);
        zoomedHeight = (int) (viewScale * height);
    }

    public Dimension getZoomedSize() {
        return new Dimension(zoomedWidth, zoomedHeight);
    }

    public int getZoomedWidth() {
        return zoomedWidth;
    }

    public int getZoomedHeight() {
        return zoomedHeight;
    }

    public void setIc(ImageComponent ic) {
        this.ic = ic;
    }
}
