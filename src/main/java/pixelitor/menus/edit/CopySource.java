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

package pixelitor.menus.edit;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Result;
import pixelitor.utils.Shapes;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static pixelitor.utils.Texts.i18n;

/**
 * Defines different sources for images that can be copied to the clipboard.
 */
public enum CopySource {
    LAYER_OR_MASK {
        @Override
        Result<BufferedImage, String> getImage(Composition comp) {
            // use the active layer, or its mask if mask editing is active
            Layer src = comp.getActiveLayer();
            if (src.isMaskEditing()) {
                src = src.getMask();
            }

            BufferedImage layerImage = src.toImage(true, false);
            if (layerImage == null) {
                return Result.error("this layer can't be copied");
            }

            return extractSelectedRegion(layerImage, comp);
        }

        @Override
        public String getName() {
            return i18n("copy_layer_mask");
        }
    }, COMPOSITE {
        @Override
        Result<BufferedImage, String> getImage(Composition comp) {
            return extractSelectedRegion(comp.getCompositeImage(), comp);
        }

        @Override
        public String getName() {
            return i18n("copy_composite");
        }
    };

    /**
     * Returns the selected portion of the image or the entire image if no selection exists.
     */
    private static Result<BufferedImage, String> extractSelectedRegion(BufferedImage sourceImage,
                                                                       Composition comp) {
        if (!comp.hasSelection()) {
            return Result.success(sourceImage);
        }

        Selection selection = comp.getSelection();
        Shape selectionShape = selection.getShape();

        if (selection.isRectangular()) {
            // for rectangular selections a simple crop is sufficient
            Rectangle2D selRect = (Rectangle2D) selectionShape;
            Rectangle selBounds = Shapes.roundRect(selRect);
            return cropToSelectionBounds(sourceImage, comp.getCanvas(), selBounds);
        }

        // for a non-rectangular selection, set the
        // unselected parts to transparent with an AA border
        Rectangle selBounds = selectionShape.getBounds();

        // fill the new image first with a mask generated from the selection shape
        BufferedImage selectedRegion = ImageUtils.createSysCompatibleImage(
            selBounds.width, selBounds.height);
        Graphics2D g2 = ImageUtils.createSoftSelectionMask(
            selectedRegion, selection.getShape(), selBounds.x, selBounds.y);

        // draw the source image, offset to align with the selection bounds
        g2.drawImage(sourceImage, -selBounds.x, -selBounds.y, null);
        g2.dispose();
        return Result.success(selectedRegion);
    }

    private static Result<BufferedImage, String> cropToSelectionBounds(BufferedImage canvasSizedImage,
                                                                       Canvas canvas,
                                                                       Rectangle selBounds) {
        // clip the selection bounds to the canvas area
        selBounds = canvas.intersect(selBounds);

        if (selBounds.isEmpty()) {
            return Result.error("the selection is completely outside the canvas");
        }
        return Result.success(ImageUtils.crop(canvasSizedImage, selBounds));
    }

    /**
     * Extracts an image from the composition to be copied to the clipboard.
     */
    abstract Result<BufferedImage, String> getImage(Composition comp);

    abstract String getName();
}
