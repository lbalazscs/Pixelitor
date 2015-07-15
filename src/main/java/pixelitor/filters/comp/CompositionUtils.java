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
import pixelitor.history.AddToHistory;
import pixelitor.history.CanvasChangeEdit;
import pixelitor.history.History;
import pixelitor.history.MultiLayerEdit;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.Composition.ImageChangeActions.INVALIDATE_CACHE;

/**
 * Composition-related static utility methods
 */
public final class CompositionUtils {
    private CompositionUtils() {
    }

    public static void cropImage(Composition comp, Rectangle cropRect, boolean selection, boolean allowGrowing) {
        Canvas canvas = comp.getCanvas();
        if(!allowGrowing) {
            cropRect = cropRect.intersection(canvas.getBounds());
        }

        if (cropRect.isEmpty()) {
            // empty selection, can't do anything useful
            return;
        }

        if(selection) {
            comp.deselect(AddToHistory.NO);
        } else {
            comp.cropSelection(cropRect);
        }

        int nrLayers = comp.getNrLayers();

        BufferedImage backupImage = null;
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ImageLayer) {
                backupImage = ((ImageLayer) layer).getImage();
            }
            layer.crop(cropRect);
        }

        CanvasChangeEdit canvasChangeEdit = new CanvasChangeEdit("", comp);
        MultiLayerEdit edit = new MultiLayerEdit(comp, "Crop", backupImage, canvasChangeEdit);
        History.addEdit(edit);

        canvas.updateSize(cropRect.width, cropRect.height);
        comp.updateAllIconImages();
        comp.setDirty(true);

        ImageComponent ic = comp.getIC();

        ic.setPreferredSize(new Dimension(cropRect.width, cropRect.height));
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

        Shape backupShape = null;
        if (comp.hasSelection()) {
            Selection selection = comp.getSelectionOrNull();
            backupShape = selection.getShape();

            double sx = ((double) targetWidth) / actualWidth;
            double sy = ((double) targetHeight) / actualHeight;
            AffineTransform tx = AffineTransform.getScaleInstance(sx, sy);
            selection.transform(tx, AddToHistory.NO);
        }
        BufferedImage backupImage = null;

        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ImageLayer) {
                backupImage = ((ImageLayer) layer).getImage();
            }
            layer.resize(targetWidth, targetHeight, progressiveBilinear);
            if (layer.hasMask()) {
                layer.getMask().resize(targetWidth, targetHeight, progressiveBilinear);
            }
        }

        CanvasChangeEdit canvasChangeEdit = new CanvasChangeEdit("", comp);
        MultiLayerEdit edit = new MultiLayerEdit(comp, "Resize", backupImage, canvasChangeEdit);

        SelectionChangeEdit selectionChangeEdit = new SelectionChangeEdit(comp, backupShape, "");
        edit.setSelectionChangeEdit(selectionChangeEdit);
        History.addEdit(edit);

        comp.getCanvas().updateSize(targetWidth, targetHeight);

        // Only after the shared canvas size was updated
        // The icon image should change if the proportions were
        // changed or if it was resized to a very small size
        comp.updateAllIconImages();

        comp.imageChanged(INVALIDATE_CACHE);

        AppLogic.activeCompositionDimensionsChanged(comp);
    }

}