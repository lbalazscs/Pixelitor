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

package pixelitor.menus.edit;

import pixelitor.Canvas;
import pixelitor.Views;
import pixelitor.colors.Colors;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

/**
 * Represents the destination of a pasted image.
 */
public enum PasteTarget {
    /**
     * Paste the image as a new layer in the active composition.
     */
    NEW_LAYER(true, "paste_as_new_layer") {
        @Override
        void paste(BufferedImage pastedImage) {
            Views.getActiveComp().addExternalImageAsNewLayer(pastedImage,
                "Pasted Layer", "New Pasted Layer");
        }
    },
    /**
     * Paste the image as a completely new image.
     */
    NEW_IMAGE(false, "paste_as_new_img") {
        @Override
        void paste(BufferedImage pastedImage) {
            Views.addNewPasted(pastedImage);
        }
    },
    /**
     * Paste the image as a layer mask, centering and/or cropping to match the canvas.
     */
    MASK(true, "paste_as_layer_mask") {
        @Override
        void paste(BufferedImage pastedImage) {
            var comp = Views.getActiveComp();

            updateLayerMask(
                comp.getActiveLayer(),
                createCanvasSizedMaskImage(pastedImage, comp.getCanvas()));
        }

        private static BufferedImage createCanvasSizedMaskImage(BufferedImage pastedImage, Canvas canvas) {
            int canvasWidth = canvas.getWidth();
            int canvasHeight = canvas.getHeight();

            int imgWidth = pastedImage.getWidth();
            int imgHeight = pastedImage.getHeight();

            // the mask image will be canvas-sized, even
            // if the pasted image has a different size
            BufferedImage bwImage = new BufferedImage(
                canvasWidth, canvasHeight, TYPE_BYTE_GRAY);

            Graphics2D g = bwImage.createGraphics();
            // if the pasted image is too small, pad it with white
            if (!canvas.isFullyCoveredBy(pastedImage)) {
                Colors.fillWith(Color.WHITE, g, canvasWidth, canvasHeight);
            }

            // center the pasted image
            int x = (canvasWidth - imgWidth) / 2;
            int y = (canvasHeight - imgHeight) / 2;
            g.drawImage(pastedImage, x, y, null);

            g.dispose();
            return bwImage;
        }

        private static void updateLayerMask(Layer layer, BufferedImage bwImage) {
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                mask.replaceImage(bwImage, "Replace Mask");
            } else {
                layer.addImageAsMask(bwImage, false, true,
                    true, "Add Pasted Mask", false);
            }
        }
    };

    private final boolean requiresOpenView;

    // the resource key for the localized action name
    private final String resourceKey;

    PasteTarget(boolean requiresOpenView, String resourceKey) {
        this.requiresOpenView = requiresOpenView;
        this.resourceKey = resourceKey;
    }

    abstract void paste(BufferedImage pastedImage);

    public boolean requiresOpenView() {
        return requiresOpenView;
    }

    public String getResourceKey() {
        return resourceKey;
    }
}
