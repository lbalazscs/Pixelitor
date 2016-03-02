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
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.history.MultiLayerBackup;
import pixelitor.history.MultiLayerEdit;
import pixelitor.layers.Layer;
import pixelitor.utils.Messages;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;

import static pixelitor.Composition.ImageChangeActions.FULL;

public class Crop implements CompAction {
    private Rectangle2D cropRect;
    private final boolean selectionCrop;
    private final boolean allowGrowing;

    public Crop(Rectangle2D cropRect, boolean selectionCrop, boolean allowGrowing) {
        this.cropRect = cropRect;
        this.selectionCrop = selectionCrop;
        this.allowGrowing = allowGrowing;
    }

    @Override
    public void process(Composition comp) {
        Canvas canvas = comp.getCanvas();
        if (!allowGrowing) {
            cropRect = cropRect.createIntersection(canvas.getBounds());
        }

        if (cropRect.isEmpty()) {
            // empty selection, can't do anything useful
            return;
        }

        MultiLayerBackup backup = new MultiLayerBackup(comp, "Crop", true);

        if (selectionCrop) {
            assert comp.hasSelection();
            comp.deselect(AddToHistory.NO);
        } else {
            // if this is a crop started from the crop tool
            // we still could have a selection that needs to be
            // cropped
            comp.cropSelection(cropRect);
        }

        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            layer.crop(cropRect);
            if (layer.hasMask()) {
                layer.getMask().crop(cropRect);
            }
        }

        MultiLayerEdit edit = new MultiLayerEdit(comp, "Crop", backup);
        History.addEdit(edit);

        int cropRectWidth = (int) cropRect.getWidth();
        int cropRectHeight = (int) cropRect.getHeight();
        canvas.updateSize(cropRectWidth, cropRectHeight);
        comp.updateAllIconImages();

        ImageComponent ic = comp.getIC();
        if (!ic.isMock()) { // not in a test
            ic.setPreferredSize(new Dimension(cropRectWidth, cropRectHeight));
            ic.revalidate();
            ic.makeSureItIsVisible();

            ic.updateDrawStart();
        }
        comp.imageChanged(FULL, true);

        AppLogic.activeCompSizeChanged(comp);
        Messages.showStatusMessage("Image cropped to "
                + cropRectWidth + " x " + cropRectHeight + " pixels.");
    }
}
