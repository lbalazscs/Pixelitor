/*
 * Copyright 2017 Laszlo Balazs-Csiki
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

public class HueSatPaletteConfig implements PaletteConfig {
    private float saturation = 0.9f;
    private float hueShift = 0.0f;
    private JSlider satSlider;
    private JSlider hueSlider;

    public HueSatPaletteConfig(float hueShift, float saturation) {
        this.hueShift = hueShift;
        this.saturation = saturation;
    }

    public float getHueShift() {
        return hueShift;
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    @Override
    public JPanel createConfigPanel(VariationsPanel variationsPanel) {
        JPanel p = new JPanel(new GridBagLayout());

        satSlider = createSlider(saturation, "Saturation of the colors");
        satSlider.addChangeListener(e -> onNewSat(variationsPanel));

        hueSlider = createSlider(hueShift, "Rotate the hue of the colors");
        hueSlider.addChangeListener(e -> onNewHue(variationsPanel));

        Insets insets = new Insets(2, 4, 2, 4);
        GridBagConstraints labelCtr = new GridBagConstraints(0, 0, 1, 1, 0, 0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE, insets, 0, 0);
        GridBagConstraints sliderCtr = new GridBagConstraints(1, 0, 1, 1, 1.0, 0,
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

    private void onNewSat(VariationsPanel variationsPanel) {
        float oldSat = saturation;
        saturation = satSlider.getValue() / 100.0f;
        if (oldSat != saturation) {
            variationsPanel.configChanged();
        }
    }

    private void onNewHue(VariationsPanel variationsPanel) {
        float oldHueShift = hueShift;
        hueShift = hueSlider.getValue() / 100.0f;
        if (oldHueShift != hueShift) {
            variationsPanel.configChanged();
        }
    }
}
