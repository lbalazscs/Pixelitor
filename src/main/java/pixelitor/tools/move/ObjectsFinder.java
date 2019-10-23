/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.tools.move;

import pixelitor.Composition;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.MaskViewMode;

import java.awt.Point;
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

    private static final float layerOpacityThreshold = 0.05f;
    private static final int pixelAlphaThreshold = 30;

    private ObjectsFinder() {
    }

    public static ObjectsSelection findObjectAtPoint(Point2D p, Composition stage) {
        ObjectsSelection result;

        // search layers
        result = findLayerAtPoint(p, stage);
        if (!result.isEmpty()) {
            return result;
        }

        // search guides
        result = findGuideAtPoint(p, stage);
        if (!result.isEmpty()) {
            return result;
        }

        return new ObjectsSelection();
    }

    public static ObjectsSelection findGuideAtPoint(Point2D p, Composition stage) {

        ObjectsSelection result = new ObjectsSelection();
        // here guides selection, but it would be convenient to operate on objects
        // instead of doubles in pixel coordinates space
        return result;
    }

    public static ObjectsSelection findLayerAtPoint(Point2D p, Composition stage) {
        ObjectsSelection result = new ObjectsSelection();
        Point pPixel = new Point((int) p.getX(), (int) p.getY());

        // in edit mask mode - do not auto select other layers as they are invisible
        // in this mode, active layer is mask layer
        MaskViewMode viewMode = stage.getView().getMaskViewMode();
        if (viewMode == MaskViewMode.SHOW_MASK) {
            ContentLayer contentLayer = (ContentLayer) stage.getActiveLayer();
            result.setObject(contentLayer);
            result.setSnappingBoundingBox(contentLayer.getSnappingBoundingBox());
            result.setEffectiveBoundingBox(contentLayer.getEffectiveBoundingBox());
            return result;
        }

        // iterate in reverse order (we need to search layers from top to bottom)
        List<Layer> layers = stage.getLayers();
        ListIterator<Layer> li = layers.listIterator(layers.size());
        while (li.hasPrevious()) {
            Layer layer = li.previous();
            if (!(layer instanceof ContentLayer)) {
                continue;
            }
            if (!layer.isVisible()) {
                continue;
            }
            if (layer.getOpacity() < layerOpacityThreshold) {
                continue;
            }

            ContentLayer contentLayer = (ContentLayer) layer;
            int pixel = contentLayer.getMouseHitPixelAtPoint(pPixel);

            if (((pixel >> 24) & 0xff) > pixelAlphaThreshold) {
                result.setObject(layer);
                result.setSnappingBoundingBox(contentLayer.getSnappingBoundingBox());
                result.setEffectiveBoundingBox(contentLayer.getEffectiveBoundingBox());
                break;
            }
        }

        return result;
    }
}
