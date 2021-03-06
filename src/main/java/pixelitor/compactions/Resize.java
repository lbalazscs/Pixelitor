/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.selection.SelectionActions;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressHandler;
import pixelitor.utils.Utils;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.utils.Threads.*;

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
    public CompletableFuture<Composition> process(Composition oldComp) {
        Canvas oldCanvas = oldComp.getCanvas();
        int canvasCurrWidth = oldCanvas.getWidth();
        int canvasCurrHeight = oldCanvas.getHeight();

        if (canvasCurrWidth == targetWidth && canvasCurrHeight == targetHeight) {
            // nothing to do
            return CompletableFuture.completedFuture(oldComp);
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

            canvasTargetWidth = (int) (scale * canvasCurrWidth);
            canvasTargetHeight = (int) (scale * canvasCurrHeight);
        }
        var targetSize = new Dimension(canvasTargetWidth, canvasTargetHeight);

        // The resize runs outside the EDT so that the progress bar animation
        // can update and multiple resizing operations can run in parallel
        var progressHandler = Messages.startProgress("Resizing", -1);
        return CompletableFuture
            .supplyAsync(() -> oldComp.copy(true, true), onPool)
            .thenCompose(newComp -> resizeLayers(newComp, targetSize))
            .thenApplyAsync(newComp -> afterResizeActions(oldComp, newComp, targetSize, progressHandler), onEDT)
            .handle((newComp, ex) -> {
                if (ex != null) {
                    Messages.showExceptionOnEDT(ex);
                }
                return newComp;
            });
    }

    private static Composition afterResizeActions(Composition oldComp,
                                                  Composition newComp,
                                                  Dimension newCanvasSize,
                                                  ProgressHandler progressHandler) {
        assert calledOnEDT() : threadInfo();

        Canvas newCanvas = newComp.getCanvas();
        var canvasTransform = createCanvasTransform(newCanvasSize, newCanvas);
        newComp.imCoordsChanged(canvasTransform, false);

        View view = newComp.getView();
        newCanvas.changeSize(newCanvasSize.width, newCanvasSize.height, view, false);

        History.add(new CompositionReplacedEdit("Resize",
            view, oldComp, newComp, canvasTransform, false));
        view.replaceComp(newComp);

        // the view was active when the resize started, but since the
        // resize was asynchronous, this could have changed
        if (view.isActive()) {
            SelectionActions.update(newComp);
        }

        Guides oldGuides = oldComp.getGuides();
        if (oldGuides != null) {
            // the guides don't need transforming,
            // just a correct canvas size
            Guides newGuides = oldGuides.copyForNewComp(view);
            newComp.setGuides(newGuides);
        }

        // Only after the shared canvas size was updated.
        // The icon image could change if the proportions were
        // changed or if it was resized to a very small size
        newComp.updateAllIconImages();

        newComp.imageChanged(REPAINT, true);
        newComp.getView().revalidate(); // make sure the scrollbars are OK

        progressHandler.stopProgress();
        Messages.showInStatusBar(format("<b>%s</b> was resized to %dx%d pixels.",
            newComp.getName(), newCanvasSize.width, newCanvasSize.height));

        return newComp;
    }

    private static AffineTransform createCanvasTransform(Dimension targetSize, Canvas newCanvas) {
        double sx = targetSize.width / (double) newCanvas.getWidth();
        double sy = targetSize.height / (double) newCanvas.getHeight();
        return AffineTransform.getScaleInstance(sx, sy);
    }

    private static CompletableFuture<Composition> resizeLayers(Composition comp, Dimension newSize) {
        // this could be called on the EDT or on another thread, the layers
        // themselves are resized in parallel using the thread pool's threads
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        comp.forEachLayer(layer -> {
            futures.add(layer.resize(newSize));
            if (layer.hasMask()) {
                futures.add(layer.getMask().resize(newSize));
            }
        });
        return Utils.allOfList(futures)
            .thenApply(v -> comp);
    }
}
