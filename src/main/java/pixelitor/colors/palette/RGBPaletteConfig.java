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

package pixelitor.colors.palette;

import javax.swing.*;
import java.awt.GridBagLayout;

import static pixelitor.colors.palette.PaletteConfig.addSliderRow;

/**
 * Configuration for RGB color mixing with cyan-red, magenta-green, and yellow-blue sliders.
 */
public class RGBPaletteConfig implements PaletteConfig {
    private float cyanRed = 0.5f;
    private JSlider crSlider;

    private float magentaGreen = 0.5f;
    private JSlider mgSlider;

    private float yellowBlue = 0.5f;
    private JSlider ybSlider;

    public float getCyanRed() {
        return cyanRed;
    }

    public float getMagentaGreen() {
        return magentaGreen;
    }

    public float getYellowBlue() {
        return yellowBlue;
    }

    @Override
    public JPanel createConfigPanel(PalettePanel palettePanel) {
        JPanel panel = new JPanel(new GridBagLayout());

        crSlider = addSliderRow("C-R:", "Cyan-red shift",
            e -> cyanRedChanged(palettePanel), cyanRed, panel, 0);
        mgSlider = addSliderRow("M-G:", "Magenta-green shift",
            e -> magentaGreenChanged(palettePanel), magentaGreen, panel, 1);
        ybSlider = addSliderRow("Y-B:", "Yellow-blue shift",
            e -> yellowBlueChanged(palettePanel), yellowBlue, panel, 2);

        return panel;
    }

    private void cyanRedChanged(PalettePanel panel) {
        float oldValue = cyanRed;
        cyanRed = crSlider.getValue() / 100.0f;
        if (oldValue != cyanRed) {
            panel.onConfigChanged();
        }
    }

    private void magentaGreenChanged(PalettePanel panel) {
        float oldValue = magentaGreen;
        magentaGreen = mgSlider.getValue() / 100.0f;
        if (oldValue != magentaGreen) {
            panel.onConfigChanged();
        }
    }

    private void yellowBlueChanged(PalettePanel panel) {
        float oldValue = yellowBlue;
        yellowBlue = ybSlider.getValue() / 100.0f;
        if (oldValue != yellowBlue) {
            panel.onConfigChanged();
        }
    }
}
