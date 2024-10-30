/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import static pixelitor.colors.palette.PaletteConfig.createSlider;

/**
 * A palette configuration with a hue slider and
 * a saturation slider
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
        JPanel p = new JPanel(new GridBagLayout());

        satSlider = createSlider(saturation, "Saturation of the colors");
        satSlider.addChangeListener(e -> satChanged(palettePanel));

        hueSlider = createSlider(hueOffset, "Rotate the hue of the colors");
        hueSlider.addChangeListener(e -> hueChanged(palettePanel));

        Insets insets = new Insets(2, 4, 2, 4);
        var labelCtr = new GridBagConstraints(0, 0, 1, 1, 0, 0,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE, insets, 0, 0);
        var sliderCtr = new GridBagConstraints(1, 0, 1, 1, 1.0, 0,
            GridBagConstraints.EAST,
            GridBagConstraints.HORIZONTAL, insets, 0, 0);
        p.add(new JLabel("Sat:"), labelCtr);
        p.add(satSlider, sliderCtr);
        labelCtr.gridy = 1;
        p.add(new JLabel("Hue:"), labelCtr);
        sliderCtr.gridy = 1;
        p.add(hueSlider, sliderCtr);

        return p;
    }

    private void satChanged(PalettePanel palettePanel) {
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
