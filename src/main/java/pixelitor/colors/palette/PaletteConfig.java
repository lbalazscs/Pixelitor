/*
 * Copyright 2018 Laszlo Balazs-Csiki
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

/**
 * An interactive configuration for a palette with sliders
 */
public interface PaletteConfig {
    /**
     * Creates the actual configuration GUI with the sliders
     */
    JPanel createConfigPanel(PalettePanel palettePanel);

    static JSlider createSlider(float value, String toolTip) {
        int intValue = (int) (value * 100);
        JSlider slider = new JSlider(0, 100, intValue);
        slider.setToolTipText(toolTip);
        return slider;
    }
}
