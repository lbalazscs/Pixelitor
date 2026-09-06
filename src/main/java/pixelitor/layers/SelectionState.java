/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import com.bric.util.JVM;
import pixelitor.colors.Colors;
import pixelitor.gui.utils.Themes;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.Color;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createLineBorder;

/**
 * The visual selection state of the layer and mask icons.
 */
public enum SelectionState {
    INACTIVE(false, false), // inactive layer
    LAYER_SELECTED(true, false), // active layer, not in mask editing mode
    MASK_SELECTED(false, true); // active layer, in mask editing mode

    private final boolean layerSelected;
    private final boolean maskSelected;

    // seems to be a Mac-specific problem: with LineBorder, a 1px line disappears
    private static final Border baseBorder = JVM.isMac
        ? BorderFactory.createMatteBorder(1, 1, 1, 1, LayerGUI.UNSELECTED_COLOR)
        : createLineBorder(LayerGUI.UNSELECTED_COLOR, 1);

    private static Border selectedBorder;

    static {
        setupBorders(Themes.getActive().isDark());
        Themes.addThemeChangeListener(theme -> setupBorders(theme.isDark()));
    }

    SelectionState(boolean layerSelected, boolean maskSelected) {
        this.layerSelected = layerSelected;
        this.maskSelected = maskSelected;
    }

    public static void setupBorders(boolean dark) {
        Color innerColor = dark ? Colors.TRANSPARENT_BLACK : LayerGUI.SELECTED_COLOR;
        selectedBorder = createCompoundBorder(baseBorder, createLineBorder(innerColor, 1));
    }

    /**
     * Shows a selection state on a given layer and mask icon.
     * The mask argument can be null if there is no mask.
     */
    public void applyBorderStyles(JLabel layerIcon, JLabel maskIcon) {
        layerIcon.setBorder(layerSelected ? selectedBorder : null);
        if (maskIcon != null) {
            maskIcon.setBorder(maskSelected ? selectedBorder : null);
        }
    }
}
