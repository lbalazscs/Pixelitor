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

import pixelitor.layers.LayerMask;

/**
 * A debugging node for a LayerMask
 */
public class LayerMaskNode extends ImageLayerNode {
    public LayerMaskNode(LayerMask mask) {
        this("layer mask", mask);
    }

    public LayerMaskNode(String name, LayerMask mask) {
        super(name, mask);

        boolean enabled = mask.getParent().isMaskEnabled();
        addBooleanChild("enabled", enabled);

        boolean linked = mask.isLinked();
        addBooleanChild("linked", linked);
    }
}
