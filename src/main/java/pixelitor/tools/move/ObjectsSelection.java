package pixelitor.tools.move;

import java.awt.geom.Rectangle2D;

/**
 * Represents objects selection on stage.
 * It keep track of objects snapping bounding box
 * It keep track of objects effective bounding box
 * Bounding boxes must be represented in canvas coordinate space
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ObjectsSelection {

    private Object object;
    private Rectangle2D snappingBoundingBox;
    private Rectangle2D effectiveBoundingBox;

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Rectangle2D getSnappingBoundingBox() {
        return snappingBoundingBox;
    }

    public void setSnappingBoundingBox(Rectangle2D snappingBoundingBox) {
        this.snappingBoundingBox = snappingBoundingBox;
    }

    public Rectangle2D getEffectiveBoundingBox() {
        return effectiveBoundingBox;
    }

    public void setEffectiveBoundingBox(Rectangle2D effectiveBoundingBox) {
        this.effectiveBoundingBox = effectiveBoundingBox;
    }

    public boolean isEmpty() {
        return object == null;
    }
}
