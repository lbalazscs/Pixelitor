/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.Composition;
import pixelitor.history.History;
import pixelitor.history.MultiLayerBackup;
import pixelitor.history.MultiLayerEdit;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.utils.Messages;

import java.awt.geom.AffineTransform;

import static pixelitor.Composition.ImageChangeActions.INVALIDATE_CACHE;

public class Resize implements CompAction {
    private int targetWidth;
    private int targetHeight;

    // if true, resizes an image so that the proportions
    // are kept and the result fits into the given dimensions
    private final boolean resizeInBox;

    public Resize(int targetWidth, int targetHeight, boolean resizeInBox) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.resizeInBox = resizeInBox;
    }

    @Override
    public void process(Composition comp) {
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

        String editName = "Resize";
        MultiLayerBackup backup = new MultiLayerBackup(comp, editName, true);

        if (comp.hasSelection()) {
            Selection selection = comp.getSelectionOrNull();

            double sx = ((double) targetWidth) / actualWidth;
            double sy = ((double) targetHeight) / actualHeight;
            AffineTransform tx = AffineTransform.getScaleInstance(sx, sy);
            selection.transform(tx);
        }

        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            layer.resize(targetWidth, targetHeight, progressiveBilinear);
            if (layer.hasMask()) {
                layer.getMask().resize(targetWidth, targetHeight, progressiveBilinear);
            }
        }

        MultiLayerEdit edit = new MultiLayerEdit(comp, editName, backup);
        History.addEdit(edit);

        comp.getCanvas().updateSize(targetWidth, targetHeight);

        // Only after the shared canvas size was updated
        // The icon image should change if the proportions were
        // changed or if it was resized to a very small size
        comp.updateAllIconImages();

        comp.setDirty(true);
        comp.imageChanged(INVALIDATE_CACHE);

        AppLogic.activeCompSizeChanged(comp);

        Messages.showStatusMessage("Image resized to "
                + targetWidth + " x " + targetHeight + " pixels.");
    }
}
