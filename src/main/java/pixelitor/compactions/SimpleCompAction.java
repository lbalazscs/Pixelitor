/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.OpenViewEnabledAction;
import pixelitor.guides.Guides;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.SmartObject;
import pixelitor.selection.SelectionActions;
import pixelitor.utils.Messages;

import java.awt.geom.AffineTransform;
import java.util.concurrent.CompletableFuture;

/**
 * A {@link CompAction} where the processing can be simplified
 * by using the template method pattern.
 */
public abstract class SimpleCompAction extends OpenViewEnabledAction.Checked implements CompAction {
    private final boolean affectsCanvasSize;

    SimpleCompAction(String name, boolean affectsCanvasSize) {
        this.affectsCanvasSize = affectsCanvasSize;
        assert name != null;
        setText(name);
    }

    @Override
    protected void onClick(Composition comp) {
        process(comp);
    }

    @Override
    public CompletableFuture<Composition> process(Composition srcComp) {
        if (srcComp.containsLayerWithClass(SmartObject.class)) {
            Messages.showSmartObjectUnsupportedWarning(getText());
            return CompletableFuture.completedFuture(srcComp);
        }

        View view = srcComp.getView();
        Composition newComp = srcComp.copy(CopyType.UNDO, true);
        Canvas newCanvas = newComp.getCanvas();
        Canvas srcCanvas = srcComp.getCanvas();

        var canvasTransform = createCanvasTransform(newCanvas);
        newComp.imCoordsChanged(canvasTransform, false, view);

        newComp.forEachNestedLayerAndMask(this::processLayer);

        if (affectsCanvasSize) {
            updateCanvasSize(newCanvas, view);
        }

        History.add(new CompositionReplacedEdit(
            getEditName(), view, srcComp, newComp, canvasTransform, false));
        view.replaceComp(newComp);
        SelectionActions.update(newComp);

        Guides guides = srcComp.getGuides();
        if (guides != null) {
            Guides newGuides = createTransformedGuides(guides, view, srcCanvas);
            newComp.setGuides(newGuides);
        }

        // Only after the canvas size was updated, because
        // they are based on the canvas-sized subimage
        newComp.updateAllIconImages();

        newComp.update(false, true);
        if (affectsCanvasSize) {
            view.revalidate(); // make sure the scrollbars are OK
        }

        Messages.showPlainStatusMessage(getStatusBarMessage());

        return CompletableFuture.completedFuture(newComp);
    }

    private void processLayer(Layer layer) {
        if (layer instanceof ContentLayer contentLayer) {
            transform(contentLayer);
        }
    }

    protected abstract void updateCanvasSize(Canvas newCanvas, View view);

    protected abstract String getEditName();

    /**
     * Applies the transformation to the given content layer.
     */
    protected abstract void transform(ContentLayer contentLayer);

    /**
     * Returns the change made by this action as a transform in
     * image-space coordinates relative to the canvas
     */
    protected abstract AffineTransform createCanvasTransform(Canvas canvas);

    // the srcCanvas is used by "Enlarge Canvas"
    protected abstract Guides createTransformedGuides(
        Guides srcGuides, View view, Canvas srcCanvas);

    protected abstract String getStatusBarMessage();
}
