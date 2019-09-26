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
import pixelitor.guides.Guides;
import pixelitor.guides.GuidesChangeEdit;
import pixelitor.history.History;
import pixelitor.history.MultiLayerBackup;
import pixelitor.history.MultiLayerEdit;
import pixelitor.tools.Tools;
import pixelitor.utils.Messages;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * A cropping action on all layers of a composition
 */
public class Crop implements CompAction {
    // the crop rectangle in image space (relative to the canvas)
    private Rectangle2D imCropRect;

    private final boolean selectionCrop;
    private final boolean allowGrowing;
    private final boolean deleteCroppedPixels;

    public Crop(Rectangle2D imCropRect,
                boolean selectionCrop,
                boolean allowGrowing,
                boolean deleteCroppedPixels) {
        this.imCropRect = imCropRect;
        this.selectionCrop = selectionCrop;
        this.allowGrowing = allowGrowing;
        this.deleteCroppedPixels = deleteCroppedPixels;
    }

    @Override
    public void process(Composition comp) {
        Canvas canvas = comp.getCanvas();
        if (!allowGrowing) {
            imCropRect = imCropRect.createIntersection(canvas.getImBounds());
        }

        Rectangle roundedImCropRect = roundCropRect(imCropRect);
        assert !roundedImCropRect.isEmpty();

        Guides guides = comp.getGuides();
        Guides newGuides = null;
        if (guides != null) {
            newGuides = guides.copyForCrop(roundedImCropRect);
            comp.setGuides(newGuides);
        }

        MultiLayerBackup backup = new MultiLayerBackup(comp, "Crop", true);
        if (guides != null) {
            GuidesChangeEdit gce = new GuidesChangeEdit(comp, guides, newGuides);
            backup.setGuidesChangeEdit(gce);
        }

        if (selectionCrop) {
            assert comp.hasSelection();
            comp.deselect(false);
        } else {
            // if this crop was started from the crop tool, there
            // still could be a selection that needs to be cropped
            comp.intersectSelection(roundedImCropRect);
        }

        comp.forEachLayer(layer -> {
            layer.crop(roundedImCropRect, deleteCroppedPixels, allowGrowing);
            if (layer.hasMask()) {
                layer.getMask().crop(roundedImCropRect, deleteCroppedPixels, allowGrowing);
            }
        });

        AffineTransform tx = createTransformForCropRect(roundedImCropRect);
        MultiLayerEdit edit = new MultiLayerEdit("Crop", comp, backup, tx);
        History.addEdit(edit);

        int newWidth = roundedImCropRect.width;
        int newHeight = roundedImCropRect.height;
        canvas.changeImSize(newWidth, newHeight);

        // The intersected selection, tool widgets etc. have to be moved
        // into the coordinate system of the new, cropped image.
        // It is important to call thins only AFTER the actual canvas size was changed
        // so that the component coords are calculated correctly from the new image coords.
        comp.imCoordsChanged(tx, false);

        comp.updateAllIconImages();

        View view = comp.getView();
        if (!view.isMock()) { // not in a test
            view.revalidate();

            // if before the crop the internal frame started
            // at large negative coordinates, after the crop it
            // could become unreachable, so move it
            view.ensurePositiveLocation();
        }
        comp.imageChanged(FULL, true);

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
                new Crop(cropRect, false, allowGrowing, deleteCroppedPixels)
                        .process(comp);
                comp.repaint();
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
                //noinspection CodeBlock2Expr
                comp.onSelection(sel -> {
                    new Crop(sel.getShapeBounds2D(), true,
                            true, true)
                            .process(comp);
                });
            }
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }
}
