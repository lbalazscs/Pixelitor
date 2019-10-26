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

package pixelitor.filters.comp;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.selection.SelectionActions;
import pixelitor.utils.Messages;

import java.awt.geom.AffineTransform;

import static pixelitor.Composition.ImageChangeActions.REPAINT;

/**
 * Resizes all content layers of a composition
 */
public class Resize implements CompAction {
    private final int targetWidth;
    private final int targetHeight;

    // if true, resizes an image so that the proportions
    // are kept and the result fits into the given dimensions
    private final boolean resizeInBox;

    public Resize(int targetWidth, int targetHeight, boolean resizeInBox) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.resizeInBox = resizeInBox;
    }

    @Override
    public Composition process(Composition comp) {
        Canvas oldCanvas = comp.getCanvas();
        int canvasCurrWidth = oldCanvas.getImWidth();
        int canvasCurrHeight = oldCanvas.getImHeight();

        if (canvasCurrWidth == targetWidth && canvasCurrHeight == targetHeight) {
            // nothing to do
            return comp;
        }

        // it is important to use local copies of the final global
        // variables, otherwise batch resize in box gets different
        // values for each input image, see issue #74
        int canvasTargetWidth = targetWidth;
        int canvasTargetHeight = targetHeight;

        if (resizeInBox) {
            double heightScale = canvasTargetHeight / (double) canvasCurrHeight;
            double widthScale = canvasTargetWidth / (double) canvasCurrWidth;
            double scale = Math.min(heightScale, widthScale);

            canvasTargetWidth = (int) (scale * (double) canvasCurrWidth);
            canvasTargetHeight = (int) (scale * (double) canvasCurrHeight);
        }

        View view = comp.getView();
        Composition newComp = comp.createCopy(true, true);
        Canvas newCanvas = newComp.getCanvas();

        double sx = ((double) canvasTargetWidth) / canvasCurrWidth;
        double sy = ((double) canvasTargetHeight) / canvasCurrHeight;
        AffineTransform canvasTx = AffineTransform.getScaleInstance(sx, sy);
        newComp.imCoordsChanged(canvasTx, false);

        resizeLayers(newComp, canvasTargetWidth, canvasTargetHeight);

        newCanvas.changeImSize(canvasTargetWidth, canvasTargetHeight, comp.getView());

        History.addEdit(new CompositionReplacedEdit(
                "Resize", view, comp, newComp, canvasTx));
        view.replaceComp(newComp);
        SelectionActions.setEnabled(newComp.hasSelection(), newComp);

        Guides guides = comp.getGuides();
        if (guides != null) {
            // in the case of resize the guides don't
            // need transforming, just a correct canvas size
            Guides newGuides = guides.copyForNewComp(view);
            newComp.setGuides(newGuides);
        }

        // Only after the shared canvas size was updated.
        // The icon image could change if the proportions were
        // changed or if it was resized to a very small size
        newComp.updateAllIconImages();

        newComp.imageChanged(REPAINT, true);
        newComp.getView().revalidate(); // make sure the scrollbars are OK

        Messages.showInStatusBar("Image resized to "
                + canvasTargetWidth + " x " + canvasTargetHeight + " pixels.");
        return newComp;
    }

    private void resizeLayers(Composition comp, int width, int height) {
        comp.forEachLayer(layer -> {
            layer.resize(width, height);
            if (layer.hasMask()) {
                layer.getMask().resize(width, height);
            }
        });
    }
}
