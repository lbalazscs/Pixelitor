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
package pixelitor.utils.debug;

import pixelitor.layers.ImageLayer;
import pixelitor.layers.LayerMask;

import java.awt.image.BufferedImage;

/**
 * A debugging node for a Layer
 */
public class ImageLayerNode extends DebugNode {
    public ImageLayerNode(ImageLayer layer) {
        this("Layer", layer);
    }

    public ImageLayerNode(String name, ImageLayer layer) {
        super(name, layer);

        BufferedImage image = layer.getImage();
        add(new BufferedImageNode(image));

        if(layer.hasLayerMask()) {
            LayerMask mask = layer.getLayerMask();
            add(new ImageLayerNode("Layer Mask", mask));
        }

        addFloatChild("opacity", layer.getOpacity());
        addQuotedStringChild("BlendingMode", layer.getBlendingMode().toString());
        addQuotedStringChild("name", layer.getName());
        addIntChild("translationX", layer.getTranslationX());
        addIntChild("translationY", layer.getTranslationY());
    }
}
