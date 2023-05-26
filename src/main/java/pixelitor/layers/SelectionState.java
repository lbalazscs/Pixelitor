/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createLineBorder;

/**
 * Represents the selection state of the layer and mask icons.
 */
public enum SelectionState {
    /**
     * The layer is not the active layer.
     */
    UNSELECTED {
        @Override
        protected void show(JLabel layerLabel, JLabel maskLabel) {
            layerLabel.setBorder(unselectedIconOnUnselectedLayerBorder);
            if (maskLabel != null) {
                maskLabel.setBorder(unselectedIconOnUnselectedLayerBorder);
            }
        }
    },
    /**
     * The layer is active, but not in mask editing mode.
     */
    LAYER_SELECTED {
        @Override
        protected void show(JLabel layerLabel, JLabel maskLabel) {
            layerLabel.setBorder(selectedBorder);
            if (maskLabel != null) {
                maskLabel.setBorder(unselectedIconOnSelectedLayerBorder);
            }
        }
    },
    /**
     * The layer is active, and in mask editing mode.
     */
    MASK_SELECTED {
        @Override
        protected void show(JLabel layerLabel, JLabel maskLabel) {
            layerLabel.setBorder(unselectedIconOnSelectedLayerBorder);
            if (maskLabel != null) {
                maskLabel.setBorder(selectedBorder);
            }
        }
    };

    private static final Border lightBorder;

    static {
        if (JVM.isMac) {
            // seems to be a Mac-specific problem: with LineBorder,
            // a one pixel wide line disappears
            lightBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, LayerGUI.UNSELECTED_COLOR);
        } else {
            lightBorder = createLineBorder(LayerGUI.UNSELECTED_COLOR, 1);
        }
    }

    // used only in other borders

    // indicates the selection of a layer or mask icon
    private static Border selectedBorder;

    // the icon is unselected, but it is on a selected layer
    private static Border unselectedIconOnSelectedLayerBorder;

    // the icon is unselected, and it is on an unselected layer
    private static Border unselectedIconOnUnselectedLayerBorder;

    static {
        setupBorders(Themes.getCurrent().isDark());
    }

    public static void setupBorders(boolean dark) {
        if (dark) {
            Border transparentBorder = createLineBorder(Colors.TRANSPARENT_BLACK, 1);
            selectedBorder = createCompoundBorder(lightBorder, transparentBorder);
            unselectedIconOnSelectedLayerBorder = null;
            unselectedIconOnUnselectedLayerBorder = null;
        } else {
            Border darkBorder = createLineBorder(LayerGUI.SELECTED_COLOR, 1);
            selectedBorder = createCompoundBorder(lightBorder, darkBorder);
            unselectedIconOnSelectedLayerBorder = null;
            unselectedIconOnUnselectedLayerBorder = null;
        }
    }

    /**
     * Shows a selection state on a given layer and mask icon.
     * The mask argument can be null, if there is no mask.
     */
    protected abstract void show(JLabel layerLabel, JLabel maskLabel);
}
