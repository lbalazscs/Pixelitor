package pixelitor.tools.move;

import java.awt.geom.Rectangle2D;

/**
 * Represents objects selection on stage. It keep track on objects bounding box
 * Bounding box must be represented in canvas coordinate space
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ObjectsSelection {

    private Object objects[];
    private Object object;
    private Rectangle2D rect;

    public Object[] getObjects() {
        return objects;
    }

    public void setObjects(Object[] objects) {
        this.objects = objects;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Rectangle2D getRect() {
        return rect;
    }

    public void setRect(Rectangle2D rect) {
        this.rect = rect;
    }

    public boolean isEmpty() {
        return object == null;
    }
}
