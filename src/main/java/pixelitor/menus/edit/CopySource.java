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

            if (layer instanceof ImageLayer) {
                // TODO calling with "false, false" results in
                // raster "has minX or minY not equal to zero" exception?
                // "false, true" avoids it, but now it is copied twice
                return ((ImageLayer) layer).getImageOrSubImageIfSelected(false, true);
            }

            if (layer instanceof TextLayer) {
                return ((TextLayer) layer).createRasterizedImage();
            }

            return null;
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
