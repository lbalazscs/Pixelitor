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
import pixelitor.Views;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.guides.Guides;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.history.MultiEdit;
import pixelitor.layers.Layer;
import pixelitor.layers.SmartObject;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static pixelitor.Composition.UpdateActions.FULL;

/**
 * A cropping action on all layers of a composition
 */
public class Crop implements CompAction {
    // the crop rectangle in image space (relative to the canvas)
    private final Rectangle2D imCropRect;

    private final boolean selectionCrop;
    private final boolean allowGrowing;
    private final boolean deleteCroppedPixels;
    private final boolean addHidingMask;

    public Crop(Rectangle2D imCropRect,
                boolean selectionCrop, boolean allowGrowing,
                boolean deleteCroppedPixels, boolean addHidingMask) {
        this.imCropRect = imCropRect;
        this.selectionCrop = selectionCrop;
        this.allowGrowing = allowGrowing;
        this.deleteCroppedPixels = deleteCroppedPixels;
        this.addHidingMask = addHidingMask;
    }

    @Override
    public CompletableFuture<Composition> process(Composition oldComp) {
        if (oldComp.containsLayerWithClass(SmartObject.class)) {
            Messages.showNotImplementedForSmartObjects("Cropping");
            return CompletableFuture.completedFuture(oldComp);
        }

        Rectangle roundedImCropRect = Shapes.roundCropRect(imCropRect);
        Canvas oldCanvas = oldComp.getCanvas();

        if (!allowGrowing) {
            Rectangle canvasBounds = oldCanvas.getBounds();
            roundedImCropRect = roundedImCropRect.intersection(canvasBounds);
        }

        // from here it is effectively final, so it can be used in a lambda
        Rectangle cropRect = roundedImCropRect;

        if (cropRect.isEmpty()) {
            // we get here if the crop rectangle is
            // outside the canvas bounds in the crop tool
            return CompletableFuture.completedFuture(oldComp);
        }

        var canvasTransform = createCanvasTransform(cropRect);

        View view = oldComp.getView();
        Composition newComp = oldComp.copy(CopyType.UNDO, !selectionCrop);
        Canvas newCanvas = newComp.getCanvas();

        Guides guides = oldComp.getGuides();
        if (guides != null) {
            Guides newGuides = guides.copyForCrop(cropRect, view);
            newComp.setGuides(newGuides);
        }

        if (!selectionCrop) {
            // if this crop was started from the crop tool, there
            // still could be a selection that needs to be cropped
            newComp.intersectSelection(cropRect);
        }

        newComp.forEachNestedLayerAndMask(layer ->
            layer.crop(cropRect, deleteCroppedPixels, allowGrowing));

        newCanvas.resize(cropRect.width, cropRect.height, view, false);

        // The intersected selection, tool widgets etc. have to be moved
        // into the coordinate system of the new, cropped image.
        // It is important to call this only AFTER the actual canvas size was changed
        // so that the component coords are calculated correctly from the new image coords.
        newComp.imCoordsChanged(canvasTransform, false, view);

        if (addHidingMask) {
            assert selectionCrop;
            assert oldComp.hasSelection();

            Shape hidingShape = oldComp.getSelectionShape();
            hidingShape = canvasTransform.createTransformedShape(hidingShape);
            addHidingMask(newComp, hidingShape, false);
        }

        // if before the crop the internal frame started
        // at large negative coordinates, after the crop it
        // could become unreachable, so move it
        view.ensurePositiveLocation();

        assert oldComp != newComp;
        String editName = addHidingMask ? "Crop and Hide" : "Crop";
        History.add(new CompositionReplacedEdit(
            editName, view, oldComp, newComp, canvasTransform, false));
        view.replaceComp(newComp);

        newComp.updateAllIconImages();
        SelectionActions.update(newComp);

        newComp.update(FULL, true);

        Messages.showInStatusBar(format(
            "<b>%s</b> was cropped to %d x %d pixels.",
            newComp.getName(), cropRect.width, cropRect.height));

        return CompletableFuture.completedFuture(newComp);
    }

    /**
     * Crops the active image based on the crop tool
     */
    public static void toolCropActiveImage(Rectangle2D cropRect,
                                           boolean allowGrowing,
                                           boolean deleteCroppedPixels) {
        try {
            Views.onActiveComp(comp ->
                new Crop(cropRect, false, allowGrowing,
                    deleteCroppedPixels, false).process(comp));
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    /**
     * Crops the active composition based on the non-transparent content.
     */
    public static void contentCrop(Composition comp) {
        Rectangle2D bounds = comp.getNonTransparentContentBounds();
        if (bounds == null) {
            Messages.showError("No Bounds",
                "<html>No bounds found in <b>%s</b>".formatted(comp.getName()));
        } else if (bounds.equals(comp.getCanvasBounds())) {
            Messages.showError("Nothing to Crop",
                    "<html><b>%s</b> doesn't have removable transparent pixels at the edges.".formatted(comp.getName()));
        } else {
            new Crop(bounds, true, false, true, false)
                .process(comp);
        }
    }

    /**
     * Crops the active composition based on the selection bounds
     */
    public static void selectionCropActiveComp() {
        try {
            Views.onActiveComp(Crop::selectionCrop);
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private static void selectionCrop(Composition comp) {
        Selection sel = comp.getSelection();
        if (sel == null) {
            return;
        }

        if (RandomGUITest.isRunning()) {
            // ask no questions, just do the simplest crop
            rectangularSelectionCrop(comp, sel, false);
            return;
        }

        if (sel.isRectangular()) {
            rectangularSelectionCrop(comp, sel, false);
        } else {
            selectionCropWithQuestion(comp, sel);
        }
    }

    private static void selectionCropWithQuestion(Composition comp, Selection sel) {
        String title = "Selection Crop Type";
        String question = "<html>You have a non-rectangular selection, but every image has to be rectangular." +
                "<br>Pixelitor can crop to the rectangular bounds of the selection." +
                "<br>It can also hide parts of the image using layer masks.";
        int answer = Dialogs.showManyOptionsDialog(comp.getDialogParent(), title, question,
                new String[]{"Crop and Hide", "Only Crop", "Only Hide", GUIText.CANCEL},
                JOptionPane.QUESTION_MESSAGE);
        switch (answer) {
            case 0: // crop and hide
                rectangularSelectionCrop(comp, sel, true);
                break;
            case 1: // only crop
                rectangularSelectionCrop(comp, sel, false);
                break;
            case 2: // only hide
                addHidingMask(comp, sel.getShape(), true);
                comp.update(FULL);
                break;
            case JOptionPane.CLOSED_OPTION:
            case 3: // canceled
                break;
            default:
                throw new IllegalStateException("answer = " + answer);
        }
    }

    private static void rectangularSelectionCrop(Composition comp,
                                                 Selection sel,
                                                 boolean addHidingMask) {
        new Crop(sel.getShapeBounds2D(), true,
            true, true, addHidingMask).process(comp);
    }

    private static void addHidingMask(Composition comp, Shape shape, boolean addToHistory) {
        MultiEdit multiEdit = null;
        if (addToHistory) {
            multiEdit = new MultiEdit("Add Hiding Mask", comp);
        }

        int numLayers = comp.getNumLayers();
        for (int i = 0; i < numLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (addToHistory) {
                var edit = layer.hideWithMask(shape, true);
                multiEdit.add(edit);
            } else {
                layer.hideWithMask(shape, false);
            }
        }

        if (addToHistory) {
            var deselectEdit = comp.deselect(false);
            multiEdit.add(deselectEdit);

            History.add(multiEdit);
        }
    }

    /**
     * The returned transform describes how the image space
     * coordinates for a surviving pixel change after a crop
     */
    public static AffineTransform createCanvasTransform(Rectangle2D imCropRect) {
        double tx = -imCropRect.getX();
        double ty = -imCropRect.getY();
        return AffineTransform.getTranslateInstance(tx, ty);
    }
}
