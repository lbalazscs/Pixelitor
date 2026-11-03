/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.colors.Colors;
import pixelitor.utils.ImageUtils;

import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * Finds the layer under a given image-space point. Used by the Move Tool,
 * to select the moved layer when "Auto Select Layer" is checked.
 */
public class HitTesting {
    // layers that are (almost) invisible or (almost) transparent
    // at a point can't be selected by clicking on them
    private static final float MIN_HIT_OPACITY = 0.05f;
    private static final int MIN_HIT_ALPHA = 30;

    private HitTesting() {
        // static utility methods only
    }

    /**
     * Finds the layer that should be selected when the user clicks
     * at the given image-space point in the given (open) composition.
     * Returns null if there is no such layer.
     */
    public static Layer findTopmostOpaqueLayer(Composition comp, Point p) {
        assert comp.isOpen();

        // in mask editing mode never auto-select another layer
        if (comp.getMaskViewMode().isShowingMask()) {
            return comp.getActiveLayer();
        }

        return findInChildren(comp, p);
    }

    // searches the direct children of the holder, from top to bottom
    private static ContentLayer findInChildren(LayerHolder holder, Point p) {
        for (int i = holder.getNumLayers() - 1; i >= 0; i--) {
            if (holder.getLayer(i) instanceof ContentLayer layer) {
                ContentLayer hit = hitTest(layer, p);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    private static ContentLayer hitTest(ContentLayer layer, Point p) {
        if (!layer.isVisible()) {
            return null;
        }

        if (layer instanceof LayerGroup group && group.isPassThrough()) {
            // renderPassThrough ignores the group's opacity, so only its
            // visibility counts, and its children are hit-tested individually
            return findInChildren(group, p);
        }

        // simple content layers, isolated groups and smart objects are
        // single entities: only their own (composite) pixels count
        if (layer.getOpacity() < MIN_HIT_OPACITY) {
            return null;
        }
        return isOpaqueAt(layer, p) ? layer : null;
    }

    private static boolean isOpaqueAt(ContentLayer layer, Point p) {
        int pixel = layer.getPixelAtPoint(p);
        return (pixel >>> 24) > MIN_HIT_ALPHA;
    }

    /**
     * Returns the ARGB pixel of the given layer image at the given image-space
     * point, taking the layer translation and the (enabled) layer mask into
     * account. Returns zero if the point is outside the image.
     * Meant to be called from {@link ContentLayer#getPixelAtPoint(Point)}
     * implementations.
     */
    public static int getPixelAt(ContentLayer layer, BufferedImage image, Point p) {
        int x = p.x - layer.getTx();
        int y = p.y - layer.getTy();

        if (!ImageUtils.isWithinBounds(x, y, image)) {
            return 0x00_00_00_00;
        }

        int imagePixel = image.getRGB(x, y);

        if (layer.hasMask() && layer.isMaskEnabled()) {
            int maskPixel = layer.getMask().getPixelAtPoint(p);
            float maskAlpha = (maskPixel & 0xFF) / 255.0f;
            int imageAlpha = (imagePixel >>> 24) & 0xFF;
            int effectiveAlpha = (int) (imageAlpha * maskAlpha);
            return Colors.setAlpha(imagePixel, effectiveAlpha);
        }

        return imagePixel;
    }
}
