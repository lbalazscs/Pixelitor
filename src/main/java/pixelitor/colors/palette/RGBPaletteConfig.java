/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
 * A palette configuration for RGB color mixing, containing
 * a cyan-red, a magenta-green and a yellow-blue slider.
 */
public class RGBPaletteConfig implements PaletteConfig {
    private float cyanRed = 0.5f;
    private float magentaGreen = 0.5f;
    private float yellowBlue = 0.5f;
    private JSlider crSlider;
    private JSlider mgSlider;
    private JSlider ybSlider;

    public RGBPaletteConfig() {
    }

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
        var p = new JPanel(new GridBagLayout());

        crSlider = createSlider(cyanRed, "Cyan-red shift");
        crSlider.addChangeListener(e -> onNewRed(palettePanel));

        mgSlider = createSlider(magentaGreen, "Magenta-green shift");
        mgSlider.addChangeListener(e -> onNewGreen(palettePanel));

        ybSlider = createSlider(yellowBlue, "Yellow-Blue shift");
        ybSlider.addChangeListener(e -> onNewBlue(palettePanel));

        Insets insets = new Insets(2, 4, 2, 4);
        var labelCtr = new GridBagConstraints(0, 0, 1, 1, 0, 0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE, insets, 0, 0);
        var sliderCtr = new GridBagConstraints(1, 0, 1, 1, 1.0, 0,
                GridBagConstraints.EAST,
                GridBagConstraints.HORIZONTAL, insets, 0, 0);

        p.add(new JLabel("C-R:"), labelCtr);
        p.add(crSlider, sliderCtr);

        labelCtr.gridy = 1;
        p.add(new JLabel("M-G:"), labelCtr);
        sliderCtr.gridy = 1;
        p.add(mgSlider, sliderCtr);

        labelCtr.gridy = 2;
        p.add(new JLabel("Y-B:"), labelCtr);
        sliderCtr.gridy = 2;
        p.add(ybSlider, sliderCtr);

        return p;
    }

    private void onNewRed(PalettePanel palettePanel) {
        float oldValue = cyanRed;
        cyanRed = crSlider.getValue() / 100.0f;
        if (oldValue != cyanRed) {
            palettePanel.configChanged();
        }
    }

    private void onNewGreen(PalettePanel palettePanel) {
        float oldValue = magentaGreen;
        magentaGreen = mgSlider.getValue() / 100.0f;
        if (oldValue != magentaGreen) {
            palettePanel.configChanged();
        }
    }

    private void onNewBlue(PalettePanel palettePanel) {
        float oldValue = yellowBlue;
        yellowBlue = ybSlider.getValue() / 100.0f;
        if (oldValue != yellowBlue) {
            palettePanel.configChanged();
        }
    }
}
