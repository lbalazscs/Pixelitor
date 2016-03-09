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

import pixelitor.AppLogic;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

/**
 * Represents the destination of the pasted image
 */
public enum PasteDestination {
    NEW_LAYER {
        @Override
        public String toString() {
            return "Paste as New Layer";
        }

        @Override
        void addImage(BufferedImage pastedImage) {
            Composition comp = ImageComponents.getActiveCompOrNull();
            Layer newLayer = new ImageLayer(comp, pastedImage, "Pasted Layer", comp.getCanvasWidth(), comp.getCanvasHeight());

            comp.addLayer(newLayer, AddToHistory.YES, "New Pasted Layer", true, false);
        }
    }, NEW_IMAGE {
        private int pastedCount = 1;

        @Override
        public String toString() {
            return "Paste as New Image";
        }

        @Override
        void addImage(BufferedImage pastedImage) {
            String title = "Pasted Image " + pastedCount;

            Composition comp = Composition.fromImage(pastedImage,
                    null, title);

            AppLogic.addComposition(comp);
            pastedCount++;
        }
    }, MASK {
        @Override
        public String toString() {
            return "Paste as Layer Mask";
        }

        @Override
        void addImage(BufferedImage pastedImage) {
            Composition comp = ImageComponents.getActiveCompOrNull();
            Canvas canvas = comp.getCanvas();
            int width = canvas.getWidth();
            int height = canvas.getHeight();

            int imgWidth = pastedImage.getWidth();
            int imgHeight = pastedImage.getHeight();

            BufferedImage bwImage = new BufferedImage(width, height, TYPE_BYTE_GRAY);
            Graphics2D g = bwImage.createGraphics();

            if (imgWidth < width || imgHeight < height) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);
            }

            int x = (width - imgWidth) / 2;
            int y = (height - imgHeight) / 2;
            g.drawImage(pastedImage, x, y, null);

            g.dispose();

            Layer layer = comp.getActiveLayer();
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                mask.replaceImage(bwImage, "Replace Mask");
            } else {
                layer.addImageAsMask(bwImage, false, "Add Pasted Mask", false);
            }
        }
    };

    abstract void addImage(BufferedImage pastedImage);
}
