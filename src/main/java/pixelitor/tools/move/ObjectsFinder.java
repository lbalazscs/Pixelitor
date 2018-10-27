package pixelitor.tools.move;

import pixelitor.Composition;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ListIterator;

/**
 * Search for object/s on the stage/composition from end user perspective
 * Result may not be accurate, for example almost transparent layers are ignored
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ObjectsFinder {
    public ObjectsFinder() {
        super();
    }

    public ObjectsSelection findObjectAtPoint(Point2D p, Composition stage) {
        ObjectsSelection result;

        // search layers
        result = findLayerAtPoint(p, stage);
        if (!result.isEmpty()) return result;

        // search guidelines
        result = findGuideLineAtPoint(p, stage);
        if (!result.isEmpty()) return result;

        return new ObjectsSelection();
    }

    public ObjectsSelection findGuideLineAtPoint(Point2D p, Composition stage) {

        ObjectsSelection result = new ObjectsSelection();
        // here guides selection, but it would be convenient to operate on objects
        // instead of doubles in pixel coordinates space
        return result;
    }

    public ObjectsSelection findLayerAtPoint(Point2D p, Composition stage) {
        ObjectsSelection result = new ObjectsSelection();

        // iterate in reverse order (we need to search layers from top to bottom)
        List layers = stage.getLayers();
        ListIterator li = layers.listIterator(layers.size());
        while (li.hasPrevious()) {
            Layer layer = (Layer) li.previous();
            if (!(layer instanceof ContentLayer)) continue;
            if (!layer.isVisible()) continue;
            if (layer.getOpacity() < 0.05) continue;

            ContentLayer imageLayer = (ContentLayer) layer;
            Rectangle rect = imageLayer.getEffectiveBoundingBox();
            if (rect.contains(p)) {
                result.setObject(layer);
                result.setRect(rect);
                break;
            }
        }

        return result;
    }
}
