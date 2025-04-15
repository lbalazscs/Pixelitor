/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

/**
 * Crops all layers of a {@link Composition}.
 */
public class Crop implements CompAction {
    // the crop rectangle in image space (relative to the canvas)
    private final Rectangle2D imCropRect;

    // whether the crop is based on a selection
    private final boolean fromSelection;

    // whether the crop can expand beyond the canvas bounds
    private final boolean allowGrowing;

    // whether pixels outside the crop area should be deleted
    private final boolean deleteCroppedPixels;

    // whether a mask should be added to hide cropped areas
    private final boolean addMaskForHiding;

    public Crop(Rectangle2D imCropRect,
                boolean fromSelection, boolean allowGrowing,
                boolean deleteCroppedPixels, boolean addMaskForHiding) {
        this.imCropRect = imCropRect;
        this.fromSelection = fromSelection;
        this.allowGrowing = allowGrowing;
        this.deleteCroppedPixels = deleteCroppedPixels;
        this.addMaskForHiding = addMaskForHiding;
    }

    @Override
    public CompletableFuture<Composition> process(Composition srcComp) {
        if (srcComp.containsLayerOfType(SmartObject.class)) {
            Messages.showSmartObjectUnsupportedWarning("Cropping");
            return CompletableFuture.completedFuture(srcComp);
        }

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
            // we get here if the crop rectangle is
            // outside the canvas bounds in the crop tool
            return CompletableFuture.completedFuture(srcComp);
        }

        var canvasTransform = createCropTransform(cropRect);

        View view = srcComp.getView();
        Composition croppedComp = srcComp.copy(CopyType.UNDO, !fromSelection);

        Guides guides = srcComp.getGuides();
        if (guides != null) {
            Guides newGuides = guides.copyCropped(cropRect, view);
            croppedComp.setGuides(newGuides);
        }

        if (!fromSelection) {
            // If the cropping was started from the crop tool, there
            // could still be a selection that needs to be cropped.
            croppedComp.intersectSelectionWith(cropRect);
        }

        // crop the layers
        croppedComp.forEachNestedLayerAndMask(layer ->
            layer.crop(cropRect, deleteCroppedPixels, allowGrowing));

        // crop the canvas
        croppedComp.getCanvas().resize(cropRect.width, cropRect.height, view, false);

        // Move the intersected selection, tool widgets, etc.,
        // into the coordinate system of the new, cropped image.
        // Done only after the canvas size has been changed!
        croppedComp.imCoordsChanged(canvasTransform, false, view);

        if (addMaskForHiding) {
            assert fromSelection;
            assert srcComp.hasSelection();

            Shape hidingShape = srcComp.getSelectionShape();
            hidingShape = canvasTransform.createTransformedShape(hidingShape);
            addHidingMask(croppedComp, hidingShape, false);
        }

        // If before the crop the internal frame started
        // at large negative coordinates, it might become
        // unreachable after the crop, so move it.
        view.ensurePositiveLocation();

        String editName = addMaskForHiding ? "Crop and Hide" : "Crop";
        History.add(new CompositionReplacedEdit(
            editName, view, srcComp, croppedComp, canvasTransform, false));
        view.replaceComp(croppedComp);

        croppedComp.updateAllIconImages();
        SelectionActions.update(croppedComp);

        croppedComp.update(true, true);

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
                                boolean deleteCroppedPixels) {
        new Crop(cropRect, false, allowGrowing,
            deleteCroppedPixels, false).process(comp);
    }

    /**
     * Crops the given composition based on the non-transparent content.
     */
    public static void contentCrop(Composition comp) {
        Rectangle2D bounds = comp.calcContentBounds(false);
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
     * Crops the active composition based on the selection bounds.
     */
    public static void selectionCropActiveComp() {
        Views.onActiveComp(Crop::selectionCrop);
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
                comp.update();
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

    /**
     * Adds a hiding mask to the composition.
     */
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
     * Creates an AffineTransform that describes how
     * image space coordinates change after a crop.
     */
    public static AffineTransform createCropTransform(Rectangle2D imCropRect) {
        double tx = -imCropRect.getX();
        double ty = -imCropRect.getY();
        return AffineTransform.getTranslateInstance(tx, ty);
    }
}
