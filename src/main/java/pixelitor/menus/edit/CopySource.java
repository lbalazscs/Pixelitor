/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.Composition;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Represents the source of the image that will be copied to the clipboard
 */
public enum CopySource {
    LAYER {
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

            if (layer instanceof TextLayer) {
                canvasSizedImage = ((TextLayer) layer).createRasterizedImage();
            }

            if (canvasSizedImage == null) {
                return null;
            }

            if (!comp.hasSelection()) {
                return canvasSizedImage;
            }

            // If we get here, it means that there is an image and also a selection
            // Now create an image with the selected pixels only

            Shape selectionShape = comp.getSelection().getShape();
            Rectangle bounds = selectionShape.getBounds();

            // just to be sure that the bounds are inside the canvas
            bounds = SwingUtilities.computeIntersection(
                    0, 0, comp.getCanvasWidth(), comp.getCanvasHeight(), // image bounds
                    bounds
            );
            if (bounds.isEmpty()) { // the selection was outside the image
                return null;
            }

            BufferedImage boundsSizeImg = canvasSizedImage.getSubimage(
                    bounds.x, bounds.y, bounds.width, bounds.height);

            BufferedImage finalImage = ImageUtils.createSysCompatibleImage(bounds.width, bounds.height);
            Graphics2D g2 = finalImage.createGraphics();
            AffineTransform at = AffineTransform.getTranslateInstance(-bounds.x, -bounds.y);
            g2.setClip(at.createTransformedShape(selectionShape));
            g2.drawImage(boundsSizeImg, 0, 0, null);
            g2.dispose();
            return finalImage;
        }

        @Override
        public String toString() {
            return "Copy Layer";
        }
    }, COMPOSITE {
        @Override
        BufferedImage getImage(Composition comp) {
            return comp.getCompositeImage();
        }

        @Override
        public String toString() {
            return "Copy Composite";
        }
    };

    abstract BufferedImage getImage(Composition comp);
}
