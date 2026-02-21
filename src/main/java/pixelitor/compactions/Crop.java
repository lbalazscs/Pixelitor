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
import pixelitor.CopyType;
import pixelitor.Views;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.guides.Guides;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.history.MultiEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.Layer;
import pixelitor.layers.SmartObject;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

/**
 * Crops a {@link Composition} based on a rectangle.
 */
public class Crop implements CompAction {
    // the crop rectangle in image space (relative to the canvas)
    private final Rectangle2D imCropRect;

    // whether the crop is based on a selection
    private final boolean fromSelection;

    // whether the crop can enlarge the canvas
    private final boolean allowGrowing;

    // whether pixels outside the crop area should be deleted
    private final boolean deleteCroppedPixels;

    // whether a mask should be added to hide cropped areas
    private final boolean addMaskForHiding;
    private final PixelitorEdit cropBoxRestorationEdit;

    /**
     * Configures a new crop operation.
     */
    public Crop(Rectangle2D imCropRect,
                boolean fromSelection, boolean allowGrowing,
                boolean deleteCroppedPixels, boolean addMaskForHiding,
                PixelitorEdit cropBoxRestorationEdit) {
        this.imCropRect = imCropRect;
        this.fromSelection = fromSelection;
        this.allowGrowing = allowGrowing;
        this.deleteCroppedPixels = deleteCroppedPixels;
        this.addMaskForHiding = addMaskForHiding;
        this.cropBoxRestorationEdit = cropBoxRestorationEdit;
    }

    /**
     * Applies the configured crop operation to the source composition.
     */
    @Override
    public CompletableFuture<Composition> process(Composition srcComp) {
        if (srcComp.containsLayerOfType(SmartObject.class)) {
            Messages.showSmartObjectUnsupportedWarning("Cropping");
            return CompletableFuture.completedFuture(srcComp);
        }

        // use integer coordinates because cropping is pixel-based
        Rectangle roundedImCropRect = Shapes.roundRect(imCropRect);
        Canvas srcCanvas = srcComp.getCanvas();

        if (!allowGrowing) {
            // constrain crop rectangle to current canvas bounds
            Rectangle canvasBounds = srcCanvas.getBounds();
            roundedImCropRect = roundedImCropRect.intersection(canvasBounds);
        }

        // from here it's effectively final, so it can be used in a lambda
        Rectangle cropRect = roundedImCropRect;

        if (cropRect.isEmpty()) {
            // we can get here if the crop rectangle is
            // outside the canvas bounds in the crop tool
            return CompletableFuture.completedFuture(srcComp);
        }

        // the transform needed to map old image coordinates to new (cropped) coordinates
        var canvasTransform = createCropTransform(cropRect);

        View view = srcComp.getView();
        // create a copy for undo; copy selection only if the crop didn't originate
        // from the selection itself (if it did, the selection is consumed by the crop)
        Composition croppedComp = srcComp.copy(CopyType.UNDO, !fromSelection);

        // crop guides relative to the new canvas origin and size
        Guides guides = srcComp.getGuides();
        if (guides != null) {
            Guides newGuides = guides.copyCropped(cropRect, view);
            croppedComp.setGuides(newGuides);
        }

        if (!fromSelection) {
            // if cropping wasn't based on a selection,
            // ensure any existing selection is also cropped
            croppedComp.intersectSelectionWith(cropRect);
        }

        // crop individual layers
        croppedComp.forEachNestedLayerAndMask(layer ->
            layer.crop(cropRect, deleteCroppedPixels, allowGrowing));

        // resize the canvas to the crop dimensions
        croppedComp.getCanvas().resize(cropRect.width, cropRect.height, view, false);

        // Move the intersected selection, tool widgets, etc.,
        // into the coordinate system of the new, cropped image.
        // This must happen *after* the canvas size has changed!
        croppedComp.imCoordsChanged(canvasTransform, false, view);

        if (addMaskForHiding) {
            assert fromSelection;
            assert srcComp.hasSelection();

            // add a mask based on the original selection shape
            Shape hidingShape = srcComp.getSelectionShape();
            hidingShape = canvasTransform.createTransformedShape(hidingShape);
            addMaskDerivedFromShape(croppedComp, hidingShape, false);
        }

        // if before the crop the internal frame started
        // at large negative coordinates, it might become
        // unreachable after the crop, so move it
        view.ensurePositiveLocation();

        String editName = addMaskForHiding ? "Crop and Hide" : "Crop";
        CompositionReplacedEdit compReplacedEdit = new CompositionReplacedEdit(
            editName, view, srcComp, croppedComp, canvasTransform, false);
        if (cropBoxRestorationEdit != null) {
            // this crop originated from the crop tool
            History.add(new MultiEdit(editName, croppedComp,
                cropBoxRestorationEdit, compReplacedEdit));
        } else {
            // crop from other sources (selection, content trim), no crop box to manage
            History.add(compReplacedEdit);
        }

        view.replaceComp(croppedComp);

        croppedComp.updateAllIconImages();
        SelectionActions.update(croppedComp);

        croppedComp.update(true);

        Messages.showStatusMessage(format(
            "<b>%s</b> was cropped to %d x %d pixels.",
            croppedComp.getName(), cropRect.width, cropRect.height));

        return CompletableFuture.completedFuture(croppedComp);
    }

    /**
     * Crops the given composition based on the crop tool.
     */
    public static void toolCrop(Composition comp,
                                Rectangle2D cropRect,
                                boolean allowGrowing,
                                boolean deleteCroppedPixels,
                                PixelitorEdit cropBoxRestorationEdit) {
        new Crop(cropRect, false, allowGrowing,
            deleteCroppedPixels, false,
            cropBoxRestorationEdit).process(comp);
    }

    /**
     * Crops the given composition based on the non-transparent content bounds.
     */
    public static void contentCrop(Composition comp) {
        Rectangle2D bounds = comp.calcContentBounds(false);
        if (bounds == null) {
            Messages.showError("Cannot Determine Bounds",
                "<html>No bounds found in <b>%s</b>".formatted(comp.getName()));
        } else if (bounds.equals(comp.getCanvasBounds())) {
            Messages.showInfo("Nothing to Crop",
                "<html><b>%s</b> has no transparent border pixels to remove.".formatted(comp.getName()));
        } else {
            new Crop(bounds, false, false, true, false, null)
                .process(comp);
        }
    }

    /**
     * Starts a crop based on the selection in the active composition.
     */
    public static void selectionCropActiveComp() {
        Views.onActiveComp(Crop::selectionCrop);
    }

    private static void selectionCrop(Composition comp) {
        Selection sel = comp.getSelection();
        if (sel == null) {
            // the menu should be disabled
            throw new IllegalStateException();
        }

        if (RandomGUITest.isRunning()) {
            // ask no questions, just do the simplest crop
            rectangularSelectionCrop(comp, sel, false);
            return;
        }

        if (sel.isRectangular()) {
            rectangularSelectionCrop(comp, sel, false);
        } else {
            askNonRectangularSelectionCropType(comp, sel);
        }
    }

    /**
     * Asks the user how to handle cropping a non-rectangular selection.
     */
    private static void askNonRectangularSelectionCropType(Composition comp, Selection sel) {
        String title = "Non-Rectangular Selection Crop";
        String question = "<html>The selection is not rectangular, but the canvas must be." +
            "<br>Choose how to proceed:<ul>" +
            "<li><b>Crop and Hide:</b> Crop to selection bounds and add a mask to hide areas outside the selection.</li>" +
            "<li><b>Only Crop:</b> Crop to the rectangular bounds of the selection.</li>" +
            "<li><b>Only Hide:</b> Add a mask based on the selection without changing the canvas size.</li></ul>";
        String[] options = {"Crop and Hide", "Only Crop", "Only Hide", GUIText.CANCEL};
        int answer = Dialogs.showManyOptionsDialog(comp.getDialogParent(), title, question,
            options, JOptionPane.QUESTION_MESSAGE);
        switch (answer) {
            case 0: // crop and hide
                rectangularSelectionCrop(comp, sel, true);
                break;
            case 1: // only crop
                rectangularSelectionCrop(comp, sel, false);
                break;
            case 2: // only hide
                addMaskDerivedFromShape(comp, sel.getShape(), true);
                comp.update();
                break;
            case JOptionPane.CLOSED_OPTION: // dialog closed
            case 3: // cancel selected
                break;
            default:
                throw new IllegalStateException("answer = " + answer);
        }
    }

    /**
     * Crops based on the rectangular bounds of a selection.
     */
    private static void rectangularSelectionCrop(Composition comp,
                                                 Selection sel,
                                                 boolean addHidingMask) {
        new Crop(sel.getShapeBounds2D(), true,
            true, true, addHidingMask, null).process(comp);
    }

    /**
     * Adds a layer mask derived from the given shape to all layers.
     */
    private static void addMaskDerivedFromShape(Composition comp, Shape shape, boolean addToHistory) {
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

            assert !multiEdit.isEmpty();
            History.add(multiEdit);
        }
    }

    /**
     * Creates an AffineTransform that describes how
     * image space coordinates change after a crop.
     */
    public static AffineTransform createCropTransform(Rectangle cropRect) {
        return AffineTransform.getTranslateInstance(-cropRect.x, -cropRect.y);
    }
}
