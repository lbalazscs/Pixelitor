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
 * Configuration for a palette with hue and saturation sliders.
 */
public class HueSatPaletteConfig implements PaletteConfig {
    private float saturation;
    private JSlider satSlider;

    private float hueOffset;
    private JSlider hueSlider;

    public HueSatPaletteConfig(float hueOffset, float saturation) {
        this.hueOffset = hueOffset;
        this.saturation = saturation;
    }

    public float getHueOffset() {
        return hueOffset;
    }

    public float getSaturation() {
        return saturation;
    }

    @Override
    public JPanel createConfigPanel(PalettePanel palettePanel) {
        JPanel panel = new JPanel(new GridBagLayout());

        satSlider = addSliderRow("Sat:", "Saturation of the colors",
            e -> saturationChanged(palettePanel), saturation, panel, 0);
        hueSlider = addSliderRow("Hue:", "Rotate the hue of the colors",
            e -> hueChanged(palettePanel), hueOffset, panel, 1);

        return panel;
    }

    private void saturationChanged(PalettePanel palettePanel) {
        float oldSat = saturation;
        saturation = satSlider.getValue() / 100.0f;
        if (oldSat != saturation) {
            palettePanel.onConfigChanged();
        }
    }

    private void hueChanged(PalettePanel palettePanel) {
        float oldHueOffset = hueOffset;
        hueOffset = hueSlider.getValue() / 100.0f;
        if (oldHueOffset != hueOffset) {
            palettePanel.onConfigChanged();
        }
    }
}
