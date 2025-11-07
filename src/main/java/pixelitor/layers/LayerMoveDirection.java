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

package pixelitor.layers;

import pixelitor.utils.Icons;

import javax.swing.*;

import static pixelitor.utils.Texts.i18n;

/**
 * The vertical direction for reordering layers in the layer stack.
 */
public enum LayerMoveDirection {
    UP("raise_layer", "raise_layer_tt", Icons.getUpArrowIcon()),
    DOWN("lower_layer", "lower_layer_tt", Icons.getDownArrowIcon());

    private final String name;
    private final String toolTip;
    private final Icon icon;

    LayerMoveDirection(String nameKey, String toolTipKey, Icon icon) {
        this.name = i18n(nameKey);
        this.toolTip = i18n(toolTipKey);
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getToolTip() {
        return toolTip;
    }

    /**
     * Returns whether the layer is already at the edge of its holder.
     */
    public boolean isAtEdge(int index, int numLayers) {
        return this == UP
            ? index == numLayers - 1
            : index == 0;
    }
}
