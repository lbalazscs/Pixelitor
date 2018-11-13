/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.guides.Guides;
import pixelitor.guides.GuidesChangeEdit;
import pixelitor.history.History;
import pixelitor.history.MultiLayerBackup;
import pixelitor.history.MultiLayerEdit;
import pixelitor.tools.Tools;
import pixelitor.utils.Messages;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * A cropping action on all layers of a composition
 */
public class Crop implements CompAction {
    // the crop rectangle in image space
    private Rectangle2D imCropRect;

    private final boolean selectionCrop;
    private final boolean allowGrowing;

    public Crop(Rectangle2D imCropRect, boolean selectionCrop, boolean allowGrowing) {
        this.imCropRect = imCropRect;
        this.selectionCrop = selectionCrop;
        this.allowGrowing = allowGrowing;
    }

    @Override
    public void process(Composition comp) {
        Canvas canvas = comp.getCanvas();
        if (!allowGrowing) {
            imCropRect = imCropRect.createIntersection(canvas.getImBounds());
        }

        if (imCropRect.isEmpty()) {
            // empty selection, can't do anything useful
            return;
        }
        Guides guides = comp.getGuides();
        Guides newGuides = null;
        if (guides != null) {
            newGuides = guides.copyForCrop(imCropRect);
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
            comp.intersectSelection(imCropRect);
        }

        comp.forEachLayer(layer -> {
            layer.crop(imCropRect);
            if (layer.hasMask()) {
                layer.getMask().crop(imCropRect);
            }
        });

        AffineTransform tx = createTransformForCropRect(imCropRect);
        MultiLayerEdit edit = new MultiLayerEdit("Crop", comp, backup, tx);
        History.addEdit(edit);

        int newWidth = (int) imCropRect.getWidth();
        int newHeight = (int) imCropRect.getHeight();
        canvas.changeImSize(newWidth, newHeight);

        // The intersected selection, tool widgets etc. have to be moved
        // into the coordinate system of the new, cropped image.
        // It is important to call thins only AFTER the actual canvas size was changed
        // so that the component coords are calculated correctly from the new image coords.
        comp.imCoordsChanged(tx, false);

        comp.updateAllIconImages();

        ImageComponent ic = comp.getIC();
        if (!ic.isMock()) { // not in a test
            ic.revalidate();

            // if before the crop the internal frame started
            // at large negative coordinates, after the crop it
            // could become unreachable, so move it
            ic.ensurePositiveLocation();
        }
        comp.imageChanged(FULL, true);

        Messages.showInStatusBar("Image cropped to "
                + newWidth + " x " + newHeight + " pixels.");
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
    public static void toolCropActiveImage(boolean allowGrowing) {
        try {
            ImageComponents.onActiveComp(comp -> {
                Rectangle2D cropRect = Tools.CROP.getCropRect().getIm();
                new Crop(cropRect, false, allowGrowing).process(comp);
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
            Composition comp = ImageComponents.getActiveCompOrNull();
            if (comp != null) {
                //noinspection CodeBlock2Expr
                comp.onSelection(sel -> {
                    new Crop(sel.getShapeBounds(), true, true)
                            .process(comp);
                });
            }
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }
}
