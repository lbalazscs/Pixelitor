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
    private int canvasTargetWidth;
    private int canvasTargetHeight;

    // if true, resizes an image so that the proportions
    // are kept and the result fits into the given dimensions
    private final boolean resizeInBox;

    public Resize(int canvasTargetWidth, int canvasTargetHeight, boolean resizeInBox) {
        this.canvasTargetWidth = canvasTargetWidth;
        this.canvasTargetHeight = canvasTargetHeight;
        this.resizeInBox = resizeInBox;
    }

    @Override
    public void process(Composition comp) {
        int canvasCurrWidth = comp.getCanvasWidth();
        int canvasCurrHeight = comp.getCanvasHeight();

        if ((canvasCurrWidth == canvasTargetWidth) && (canvasCurrHeight == canvasTargetHeight)) {
            return;
        }

        if (resizeInBox) {
            double heightScale = canvasTargetHeight / (double) canvasCurrHeight;
            double widthScale = canvasTargetWidth / (double) canvasCurrWidth;
            double scale = Math.min(heightScale, widthScale);

            canvasTargetWidth = (int) (scale * (double) canvasCurrWidth);
            canvasTargetHeight = (int) (scale * (double) canvasCurrHeight);
        }

        boolean progressiveBilinear = false;
        if ((canvasTargetWidth < (canvasCurrWidth / 2))
                || (canvasTargetHeight < (canvasCurrHeight / 2))) {
            progressiveBilinear = true;
        }

        String editName = "Resize";
        MultiLayerBackup backup = new MultiLayerBackup(comp, editName, true);

        if (comp.hasSelection()) {
            Selection selection = comp.getSelection();

            double sx = ((double) canvasTargetWidth) / canvasCurrWidth;
            double sy = ((double) canvasTargetHeight) / canvasCurrHeight;
            AffineTransform tx = AffineTransform.getScaleInstance(sx, sy);
            selection.transform(tx);
        }

        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            layer.resize(canvasTargetWidth, canvasTargetHeight, progressiveBilinear);
            if (layer.hasMask()) {
                layer.getMask().resize(canvasTargetWidth, canvasTargetHeight, progressiveBilinear);
            }
        }

        MultiLayerEdit edit = new MultiLayerEdit(comp, editName, backup);
        History.addEdit(edit);

        comp.getCanvas().updateSize(canvasTargetWidth, canvasTargetHeight);

        // Only after the shared canvas size was updated
        // The icon image should change if the proportions were
        // changed or if it was resized to a very small size
        comp.updateAllIconImages();

        comp.imageChanged(INVALIDATE_CACHE, true);

        AppLogic.activeCompSizeChanged(comp);

        Messages.showStatusMessage("Image resized to "
                + canvasTargetWidth + " x " + canvasTargetHeight + " pixels.");
    }
}
