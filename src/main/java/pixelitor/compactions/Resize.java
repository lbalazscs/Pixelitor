/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.CopyType;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.selection.SelectionActions;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressHandler;
import pixelitor.utils.Utils;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onPool;
import static pixelitor.utils.Threads.threadInfo;

/**
 * A resizing action on all layers of a {@link Composition}.
 */
public class Resize implements CompAction {
    private final int targetWidth;
    private final int targetHeight;

    // if true, resizes an image so that the proportions
    // are kept and the result fits into the given dimensions
    private final boolean resizeInBox;

    public Resize(int targetWidth, int targetHeight) {
        this(targetWidth, targetHeight, false);
    }

    public Resize(int targetWidth, int targetHeight, boolean resizeInBox) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.resizeInBox = resizeInBox;
    }

    @Override
    public CompletableFuture<Composition> process(Composition oldComp) {
        Canvas oldCanvas = oldComp.getCanvas();
        if (oldCanvas.hasImSize(targetWidth, targetHeight)) {
            // nothing to do
            return CompletableFuture.completedFuture(oldComp);
        }

        var targetSize = calcTargetSize(oldCanvas);

        // The resizing runs outside the EDT to allow the progress bar animation
        // to update, and to enable the parallel resizing of multiple layers.
        var progressHandler = Messages.startProgress("Resizing", -1);
        return CompletableFuture
            .supplyAsync(() -> oldComp.copy(CopyType.UNDO, true), onPool)
            .thenCompose(newComp -> resizeLayers(newComp, targetSize))
            .thenApplyAsync(newComp -> afterResizeActions(oldComp, newComp, targetSize, progressHandler), onEDT)
            .handle((newComp, ex) -> {
                if (ex != null) {
                    Messages.showExceptionOnEDT(ex);
                }
                return newComp;
            });
    }

    private Dimension calcTargetSize(Canvas oldCanvas) {
        int canvasCurrHeight = oldCanvas.getWidth();
        int canvasCurrWidth = oldCanvas.getHeight();

        // it's important to use local copies of the final global
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
        return new Dimension(canvasTargetWidth, canvasTargetHeight);
    }

    private static Composition afterResizeActions(Composition oldComp,
                                                  Composition newComp,
                                                  Dimension newCanvasSize,
                                                  ProgressHandler progressHandler) {
        assert calledOnEDT() : threadInfo();

        View view = oldComp.getView();
        assert view != null;

        Canvas newCanvas = newComp.getCanvas();
        var canvasTransform = newCanvas.createImTransformToSize(newCanvasSize);
        newComp.imCoordsChanged(canvasTransform, false, view);

        newCanvas.resize(newCanvasSize.width, newCanvasSize.height, view, false);

        History.add(new CompositionReplacedEdit("Resize",
            view, oldComp, newComp, canvasTransform, false));
        view.replaceComp(newComp);

        // The view was active when the resizing started, but since the
        // resizing was asynchronous, this could have changed.
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

        newComp.update(false, true);
        view.revalidate(); // make sure the scrollbars are OK

        progressHandler.stopProgress();
        Messages.showInStatusBar(format("<b>%s</b> was resized to %dx%d pixels.",
            newComp.getName(), newCanvasSize.width, newCanvasSize.height));

        return newComp;
    }

    private static CompletableFuture<Composition> resizeLayers(Composition comp, Dimension newSize) {
        // This could be called on the EDT or on another thread. The layers
        // themselves are resized in parallel using the thread pool's threads.
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        comp.forEachNestedLayerAndMask(layer ->
            futures.add(layer.resize(newSize)));
        return Utils.allOf(futures).thenApply(v -> comp);
    }
}
