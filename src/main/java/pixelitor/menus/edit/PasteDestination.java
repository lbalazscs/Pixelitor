/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;

import java.awt.image.BufferedImage;

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
            Composition comp = ImageComponents.getActiveComp().get();
            Layer newLayer = new ImageLayer(comp, pastedImage, "Pasted Layer", comp.getCanvasWidth(), comp.getCanvasHeight());

            comp.addLayer(newLayer, AddToHistory.YES, true, false);
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
    };

    abstract void addImage(BufferedImage pastedImage);
}
