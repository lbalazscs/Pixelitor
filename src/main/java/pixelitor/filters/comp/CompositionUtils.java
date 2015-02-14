/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.filters.comp;

import pixelitor.AppLogic;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ImageComponent;
import pixelitor.history.OneLayerUndoableEdit;
import pixelitor.layers.Layer;

import java.awt.Dimension;
import java.awt.Rectangle;

import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.Composition.ImageChangeActions.INVALIDATE_CACHE;

/**
 * Composition-related static utility methods
 */
public final class CompositionUtils {

    private CompositionUtils() {
    }

    public static void cropImage(Composition comp, Rectangle selectionBounds, boolean selection, boolean allowGrowing) {
        if (selectionBounds.width == 0 || selectionBounds.height == 0) {
            // empty selection, can't do anything useful
            return;
        }

        Canvas canvas = comp.getCanvas();
        if(!allowGrowing) {
            selectionBounds = selectionBounds.intersection(canvas.getBounds());
        }

        OneLayerUndoableEdit.createAndAddToHistory(comp, "Crop", false, true);
        if(selection) {
            comp.deselect(false);
        }

        int nrLayers = comp.getNrLayers();

        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            layer.crop(selectionBounds);
        }

        canvas.updateSize(selectionBounds.width, selectionBounds.height);
        comp.setDirty(true);

        ImageComponent ic = comp.getIC();

        ic.setPreferredSize(new Dimension(selectionBounds.width, selectionBounds.height));
        ic.revalidate();
        ic.makeSureItIsVisible();

        ic.updateDrawStart();
        comp.imageChanged(FULL);

        AppLogic.activeCompositionDimensionsChanged(comp);
    }

    /**
     * Resizes the composition
     *
     * @param comp
     * @param targetWidth
     * @param targetHeight
     * @param resizeInBox  if true, resizes an image so that the proportions are kept and the result fits into the given dimensions
     */
    public static void resize(Composition comp, int targetWidth, int targetHeight, boolean resizeInBox) {
        int actualWidth = comp.getCanvasWidth();
        int actualHeight = comp.getCanvasHeight();

        if ((actualWidth == targetWidth) && (actualHeight == targetHeight)) {
            return;
        }

        if (resizeInBox) {
            int maxWidth = targetWidth;
            int maxHeight = targetHeight;

            double heightScale = maxHeight / (double) actualHeight;
            double widthScale = maxWidth / (double) actualWidth;
            double scale = Math.min(heightScale, widthScale);

            targetWidth = (int) (scale * (double) actualWidth);
            targetHeight = (int) (scale * (double) actualHeight);
        }

        boolean progressiveBilinear = false;
        if ((targetWidth < (actualWidth / 2)) || (targetHeight < (actualHeight / 2))) {
            progressiveBilinear = true;
        }

        OneLayerUndoableEdit.createAndAddToHistory(comp, "Resize", false, true);
        comp.deselect(false);

        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            layer.resize(targetWidth, targetHeight, progressiveBilinear);
        }

        comp.getCanvas().updateSize(targetWidth, targetHeight);
        comp.imageChanged(INVALIDATE_CACHE);

        AppLogic.activeCompositionDimensionsChanged(comp);
    }

}