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
import pixelitor.gui.OpenComps;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.guides.Guides;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.history.MultiEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tools;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import static pixelitor.Composition.ImageChangeActions.FULL;

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
                boolean selectionCrop,
                boolean allowGrowing,
                boolean deleteCroppedPixels, boolean addHidingMask) {
        this.imCropRect = imCropRect;
        this.selectionCrop = selectionCrop;
        this.allowGrowing = allowGrowing;
        this.deleteCroppedPixels = deleteCroppedPixels;
        this.addHidingMask = addHidingMask;
    }

    @Override
    public void process(Composition comp) {
        Rectangle roundedImCropRect = roundCropRect(imCropRect);
        Canvas oldCanvas = comp.getCanvas();

        if (!allowGrowing) {
            Rectangle canvasBounds = oldCanvas.getImBounds();
            roundedImCropRect = roundedImCropRect.intersection(canvasBounds);
        }

        // from here it is effectively final, so it can be used in a lambda
        Rectangle cropRect = roundedImCropRect;

        if (cropRect.isEmpty()) {
            // we get here if the crop rectangle is
            // outside the canvas bounds in the crop tool
            return;
        }

        AffineTransform cropRectTranslation = createTransformForCropRect(cropRect);

        View view = comp.getView();
        Composition newComp = comp.createCopy(true, !selectionCrop);
        Canvas newCanvas = newComp.getCanvas();

        Guides guides = comp.getGuides();
        if (guides != null) {
            Guides newGuides = guides.copyForCrop(cropRect, view);
            newComp.setGuides(newGuides);
        }

        if (!selectionCrop) {
            // if this crop was started from the crop tool, there
            // still could be a selection that needs to be cropped
            newComp.intersectSelection(cropRect);
        }

        newComp.forEachLayer(layer -> {
            layer.crop(cropRect, deleteCroppedPixels, allowGrowing);
            if (layer.hasMask()) {
                layer.getMask().crop(cropRect, deleteCroppedPixels, allowGrowing);
            }
        });

        int newWidth = cropRect.width;
        int newHeight = cropRect.height;
        newCanvas.changeImSize(newWidth, newHeight, view);

        // The intersected selection, tool widgets etc. have to be moved
        // into the coordinate system of the new, cropped image.
        // It is important to call thins only AFTER the actual canvas size was changed
        // so that the component coords are calculated correctly from the new image coords.
        newComp.imCoordsChanged(cropRectTranslation, false);

        if (addHidingMask) {
            assert selectionCrop;
            assert comp.hasSelection();

            Selection hidingSelection = new Selection(comp.getSelection(), true);
            hidingSelection.transform(cropRectTranslation);
            addHidingMask(newComp, hidingSelection, false);
        }

        newComp.updateAllIconImages();

        // if before the crop the internal frame started
        // at large negative coordinates, after the crop it
        // could become unreachable, so move it
        view.ensurePositiveLocation();

        assert comp != newComp;
        History.addEdit(new CompositionReplacedEdit(
                "Crop", view, comp, newComp));
        view.replaceComp(newComp);
        SelectionActions.setEnabled(newComp.hasSelection(), newComp);

        newComp.imageChanged(FULL, true);

        Messages.showInStatusBar("Image cropped to "
                + newWidth + " x " + newHeight + " pixels.");
    }


    // in zoomed-in images fractional widths and heights can happen
    private static Rectangle roundCropRect(Rectangle2D rect) {
        int x = (int) Math.round(rect.getX());
        int y = (int) Math.round(rect.getY());
        int width = (int) Math.round(rect.getWidth());
        int height = (int) Math.round(rect.getHeight());

        if (width == 0) {
            width = 1;
        }
        if (height == 0) {
            height = 1;
        }
        return new Rectangle(x, y, width, height);
    }

    /**
     * The returned transform describes how the image space
     * coordinates for a surviving pixel change after a crop
     */
    public static AffineTransform createTransformForCropRect(Rectangle2D imCropRect) {
        double txx = -imCropRect.getX();
        double txy = -imCropRect.getY();
        return AffineTransform.getTranslateInstance(txx, txy);
    }

    /**
     * Crops the active image based on the crop tool
     */
    public static void toolCropActiveImage(boolean allowGrowing,
                                           boolean deleteCroppedPixels) {
        try {
            OpenComps.onActiveComp(comp -> {
                Rectangle2D cropRect = Tools.CROP.getCropRect().getIm();
                new Crop(cropRect, false, allowGrowing, deleteCroppedPixels, false)
                        .process(comp);
            });
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    /**
     * Crops the active image based on the selection bounds
     */
    public static void selectionCropActiveImage() {
        try {
            Composition comp = OpenComps.getActiveCompOrNull();
            if (comp != null) {
                selectionCrop(comp);
            }
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private static void selectionCrop(Composition comp) {
        Selection sel = comp.getSelection();
        if (sel == null) {
            return;
        }
        Shape selShape = sel.getShape();
        if (selShape instanceof Rectangle2D) {
            rectangularCrop(comp, sel, false);
        } else {
            selectionCropWithQuestion(comp, sel);
        }
    }

    private static void selectionCropWithQuestion(Composition comp, Selection sel) {
        String question = "<html>You have a nonrectangular selection, but every image has to be rectangular." +
                "<br>Pixelitor can crop to the rectangular bounds of the selection." +
                "<br>It can also hide parts of the image using layer masks.";
        // the yes-no-cancel dialog can actually show more than 3 options
        int answer = Dialogs.showYesNoCancelDialog("Selection Crop Type",
                new String[]{"Crop and Hide", "Only Crop", "Only Hide", "Cancel"},
                question, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.CLOSED_OPTION || answer == 3) {
            // canceled, do nothing
        } else if (answer == 0) {
            // crop and hide
            rectangularCrop(comp, sel, true);
        } else if (answer == 1) {
            // only crop
            rectangularCrop(comp, sel, false);
        } else if (answer == 2) {
            // only hide
            addHidingMask(comp, sel, true);
        } else {
            throw new IllegalStateException("answer = " + answer);
        }
    }

    private static void rectangularCrop(Composition comp,
                                        Selection sel,
                                        boolean addHidingMask) {
        new Crop(sel.getShapeBounds2D(), true,
                true, true, addHidingMask)
                .process(comp);
    }

    private static void addHidingMask(Composition comp, Selection sel, boolean addToHistory) {
        MultiEdit multiEdit = null;
        if (addToHistory) {
            multiEdit = new MultiEdit("Add Hiding Mask", comp);
        }

        int numLayers = comp.getNumLayers();
        for (int i = 0; i < numLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (addToHistory) {
                PixelitorEdit edit = layer.addHidingMask(sel, true);
                multiEdit.add(edit);
            } else {
                layer.addHidingMask(sel, false);
            }
        }

        if (addToHistory) {
            History.addEdit(multiEdit);
        }
    }
}
