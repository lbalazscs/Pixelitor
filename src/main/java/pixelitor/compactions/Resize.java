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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.CopyOptions;
import pixelitor.gui.CompositionReplacedContext;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.progress.ProgressHandler;
import pixelitor.selection.SelectionActions;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static pixelitor.utils.Threads.*;

/**
 * Resizes all layers of a {@link Composition} to the given dimensions.
 * It can either stretch the content to exactly match the target
 * dimensions or maintain the aspect ratio while fitting within them.
 */
public class Resize implements CompAction {
    // in aspect-ratio mode these define only the target bounding box
    private final int targetWidth;
    private final int targetHeight;

    public enum Mode {STRETCH, ASPECT_RATIO}

    private final Mode mode;

    public Resize(int targetWidth, int targetHeight) {
        this(targetWidth, targetHeight, Mode.STRETCH);
    }

    public Resize(int targetWidth, int targetHeight, Mode mode) {
        assert targetWidth > 0 : "targetWidth = " + targetWidth;
        assert targetHeight > 0 : "targetHeight = " + targetHeight;

        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.mode = mode;
    }

    @Override
    public CompletableFuture<Composition> process(Composition srcComp) {
        Canvas srcCanvas = srcComp.getCanvas();
        var newSize = calcNewCanvasSize(srcCanvas);

        if (srcCanvas.hasImSize(newSize.width, newSize.height)) {
            // no resize needed
            return CompletableFuture.completedFuture(srcComp);
        }

        // The resizing runs outside the EDT to allow the progress bar animation
        // to update, and to enable the parallel resizing of multiple layers.
        var progressHandler = Messages.startProgress("<html>Resizing <b>" + srcComp.getName() + "</b>", -1);
        return CompletableFuture
            .supplyAsync(() -> srcComp.copy(CopyOptions.fullStateBackup(true, false)), onPool)
            .thenCompose(newComp -> resizeLayersInParallel(newComp, newSize))
            .thenApplyAsync(newComp -> finishResize(srcComp, newComp, newSize, progressHandler), onEDT)
            .handle((newComp, ex) -> {
                if (ex != null) {
                    progressHandler.stopProgressOnEDT();
                    Messages.showExceptionOnEDT(ex);
                }
                return newComp;
            });
    }

    private Dimension calcNewCanvasSize(Canvas srcCanvas) {
        if (mode == Mode.STRETCH) {
            return new Dimension(targetWidth, targetHeight);
        }

        int srcWidth = srcCanvas.getWidth();
        int srcHeight = srcCanvas.getHeight();

        double heightScale = targetHeight / (double) srcHeight;
        double widthScale = targetWidth / (double) srcWidth;
        double scale = Math.min(heightScale, widthScale);

        return new Dimension(
            Math.max(1, (int) (scale * srcWidth + 0.5)),
            Math.max(1, (int) (scale * srcHeight + 0.5))
        );
    }

    private static Composition finishResize(Composition srcComp,
                                            Composition newComp,
                                            Dimension newCanvasSize,
                                            ProgressHandler progressHandler) {
        assert calledOnEDT() : callInfo();

        View view = srcComp.getView();
        assert view != null;

        Canvas newCanvas = newComp.getCanvas();
        var imTransform = newCanvas.createImTransformToFit(newCanvasSize);

        newCanvas.resize(newCanvasSize.width, newCanvasSize.height, view, false);
        newComp.imCoordsChanged(imTransform, false, view);

        History.add(new CompositionReplacedEdit("Resize", view, srcComp, newComp, imTransform, CompositionReplacedContext.TRANSFORM));
        view.replaceComp(newComp);

        // the view was active when the resizing started, but since the
        // resizing was asynchronous, it could be inactive now
        if (view.isActive()) {
            SelectionActions.update(newComp);
        }

        Guides srcGuides = srcComp.getGuides();
        if (srcGuides != null) {
            // guides are stored as ratios of the canvas size, so they are only
            // copied and their component-space lines regenerated for the new canvas
            Guides newGuides = srcGuides.copy(view);
            newComp.setGuides(newGuides);
        }

        // update icon images after the canvas size has been updated,
        // as they could change if proportions were modified
        // or if the canvas was resized to a very small size
        newComp.updateAllIconImages();

        newComp.update(true);
        view.revalidate(); // so that the scrollbars reflect the new canvas size

        progressHandler.stopProgress();
        Messages.showStatusMessage(String.format("<b>%s</b> was resized to %dx%d pixels.",
            newComp.getName(), newCanvasSize.width, newCanvasSize.height));

        return newComp;
    }

    private static CompletableFuture<Composition> resizeLayersInParallel(Composition comp, Dimension newSize) {
        // This could be called on the EDT or on another thread. The layers
        // themselves are resized in parallel using the thread pool's threads.
        List<CompletableFuture<Void>> layerResizeFutures = new ArrayList<>();
        comp.forEachNestedLayerAndMask(layer ->
            layerResizeFutures.add(layer.resize(newSize)));
        return Utils.allOf(layerResizeFutures).thenApply(_ -> comp);
    }
}
