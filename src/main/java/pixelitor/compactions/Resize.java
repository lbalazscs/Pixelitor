/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.ThreadPool;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.selection.SelectionActions;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressHandler;
import pixelitor.utils.Utils;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
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
    public CompletableFuture<Composition> process(Composition comp) {
        Canvas oldCanvas = comp.getCanvas();
        int canvasCurrWidth = oldCanvas.getImWidth();
        int canvasCurrHeight = oldCanvas.getImHeight();

        if (canvasCurrWidth == targetWidth && canvasCurrHeight == targetHeight) {
            // nothing to do
            return CompletableFuture.completedFuture(comp);
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
        Dimension targetSize = new Dimension(canvasTargetWidth, canvasTargetHeight);

        // The resize runs outside the EDT so that the progress bar animation
        // can update and multiple resizing operations can run in parallel
        var progressHandler = Messages.startProgress("Resizing", -1);
        return CompletableFuture
                .supplyAsync(() -> comp.createCopy(true, true),
                        ThreadPool.getExecutor())
                .thenCompose(newComp -> resizeLayers(newComp, targetSize))
                .thenApplyAsync(newComp -> afterResizeActions(comp, newComp, targetSize, progressHandler),
                        EventQueue::invokeLater)
                .handle((newComp, ex) -> {
                    if (ex != null) {
                        Messages.showExceptionOnEDT(ex);
                    }
                    return newComp;
                });
    }

    private static Composition afterResizeActions(Composition comp, Composition newComp, Dimension targetSize, ProgressHandler progressHandler) {
        assert EventQueue.isDispatchThread() : "called on " + Thread.currentThread().getName();

        int canvasTargetWidth = targetSize.width;
        int canvasTargetHeight = targetSize.height;

        Canvas newCanvas = newComp.getCanvas();
        int canvasCurrWidth = newCanvas.getImWidth();
        int canvasCurrHeight = newCanvas.getImHeight();
        double sx = ((double) canvasTargetWidth) / canvasCurrWidth;
        double sy = ((double) canvasTargetHeight) / canvasCurrHeight;
        var canvasTransform = AffineTransform.getScaleInstance(sx, sy);
        newComp.imCoordsChanged(canvasTransform, false);

        View view = newComp.getView();
        newCanvas.changeImSize(canvasTargetWidth, canvasTargetHeight, view);

        History.add(new CompositionReplacedEdit("Resize",
                false, view, comp, newComp, canvasTransform));
        view.replaceComp(newComp);

        // the view was active when the resize started, but since the
        // resize was asynchronous, this could have changed
        if (view.isActive()) {
            SelectionActions.setEnabled(newComp.hasSelection(), newComp);
        }

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

        progressHandler.stopProgress();
        Messages.showInStatusBar(format("<html><b>%s</b> was resized to %dx%d pixels.",
                newComp.getName(), canvasTargetWidth, canvasTargetHeight));
        return newComp;
    }

    private static CompletableFuture<Composition> resizeLayers(Composition comp, Dimension newSize) {
        // this could be called on the EDT or on another thread, the layers
        // themselves are resized in parallel using the thread pool's threads
        List<CompletableFuture<?>> futures = new ArrayList<>();
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
