/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;

public class LayerNode extends DebugNode {
    public LayerNode(Layer layer) {
        this("layer \"" + layer.getName() + "\"", layer);
    }

    public LayerNode(String name, Layer layer) {
        super(name, layer);

        addQuotedString("name", layer.getName());
        addClass();

        if (layer.hasMask()) {
            addString("has mask", "yes");
            addBoolean("mask enabled", layer.isMaskEnabled());
            addBoolean("mask editing", layer.isMaskEditing());
            LayerMask mask = layer.getMask();
            add(new LayerMaskNode(mask));
        } else {
            addString("has mask", "no");
        }

        addBoolean("visible", layer.isVisible());
        addFloat("opacity", layer.getOpacity());
        addQuotedString("blending mode", layer.getBlendingMode().toString());
    }

    public static String descrToName(String descr, Layer layer) {
        return descr + " - " + layer.getName();
    }
}
