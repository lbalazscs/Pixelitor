/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.compactions.Crop;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;
import pixelitor.selection.Selection;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Represents the source of the image that will be copied to the clipboard
 */
public enum CopySource {
    LAYER_OR_MASK {
        @Override
        BufferedImage getImage(Composition comp) {
            Layer layer = comp.getActiveLayer();
            if (layer.isMaskEditing()) {
                layer = layer.getMask();
            }

            BufferedImage canvasSizedImage = null;

            if (layer instanceof ImageLayer) {
                canvasSizedImage = ((ImageLayer) layer).getCanvasSizedSubImage();
            }

            // TODO Text layers are rasterized, but they should be probably copied
            // in other formats as well (as a string, as a serialized object)
            // and pasting into Pixelitor should choose the serialized object
            // There could be also an internal clipboard, to handle such cases
            if (layer instanceof TextLayer) {
                canvasSizedImage = ((TextLayer) layer).createRasterizedImage();
            }

            if (canvasSizedImage == null) {
                return null;
            }

            return createImageWithSelectedPixels(canvasSizedImage, comp);
        }

        @Override
        public String toResourceKey() {
            return "copy_layer_mask";
        }
    }, COMPOSITE {
        @Override
        BufferedImage getImage(Composition comp) {
            return createImageWithSelectedPixels(comp.getCompositeImage(), comp);
        }

        @Override
        public String toResourceKey() {
            return "copy_composite";
        }
    };

    private static BufferedImage createImageWithSelectedPixels(
            BufferedImage canvasSizedImage, Composition comp) {
        if (!comp.hasSelection()) {
            return canvasSizedImage;
        }

        Selection selection = comp.getSelection();
        Shape selectionShape = selection.getShape();
        if (selection.isRectangular()) {
            // for rectangular selections a simple crop is needed
            Rectangle2D selRect = (Rectangle2D) selectionShape;
            Rectangle selBounds = Crop.roundCropRect(selRect);
            return cropToSelectionBounds(canvasSizedImage, comp.getCanvas(), selBounds);
        }

        // in the case of a nonrectangular selection
        // set the unselected parts to transparent with an AA border
        Rectangle selBounds = selectionShape.getBounds();

        BufferedImage tmpImg = ImageUtils.createSysCompatibleImage(selBounds.width, selBounds.height);
        Graphics2D g2 = ImageUtils.setupForSoftSelection(tmpImg, selection.getShape(), selBounds.x, selBounds.y);

        g2.drawImage(canvasSizedImage, -selBounds.x, -selBounds.y, null);
        g2.dispose();
        return tmpImg;
    }

    private static BufferedImage cropToSelectionBounds(BufferedImage canvasSizedImage, Canvas canvas, Rectangle selBounds) {
        // just to be sure that the bounds are inside the canvas
        selBounds = SwingUtilities.computeIntersection(
                0, 0, canvas.getImWidth(), canvas.getImHeight(),
                selBounds
        );
        if (selBounds.isEmpty()) { // the selection was outside the image
            return null;
        }
        return ImageUtils.crop(canvasSizedImage, selBounds);
    }

    abstract BufferedImage getImage(Composition comp);

    abstract String toResourceKey();
}
