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

package pixelitor.colors.palette;

import javax.swing.*;

import static pixelitor.utils.Utils.toPercentage;

/**
 * An interactive configuration for a {@link Palette}
 * with sliders.
 */
public interface PaletteConfig {
    /**
     * Creates the actual configuration panel with the sliders
     */
    JPanel createConfigPanel(PalettePanel palettePanel);

    static JSlider createSlider(float value, String toolTip) {
        JSlider slider = new JSlider(0, 100, toPercentage(value));
        slider.setToolTipText(toolTip);
        return slider;
    }
}
