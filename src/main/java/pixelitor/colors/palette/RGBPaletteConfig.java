/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

public class RGBPaletteConfig implements PaletteConfig {
    private float cyanRed = 0.5f;
    private float magentaGreen = 0.5f;
    private float yellowBlue = 0.5f;

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
    public JPanel createConfigPanel(VariationsPanel variationsPanel) {
        JPanel p = new JPanel(new GridBagLayout());

        JSlider redSlider = createSlider(cyanRed, "Cyan-red shift");
        redSlider.addChangeListener(e -> {
            float oldValue = cyanRed;
            cyanRed = redSlider.getValue() / 100.0f;
            if (oldValue != cyanRed) {
                variationsPanel.configChanged();
            }
        });

        JSlider greenSlider = createSlider(magentaGreen, "Magenta-green shift");
        greenSlider.addChangeListener(e -> {
            float oldValue = magentaGreen;
            magentaGreen = greenSlider.getValue() / 100.0f;
            if (oldValue != magentaGreen) {
                variationsPanel.configChanged();
            }
        });

        JSlider blueSlider = createSlider(yellowBlue, "Yellow-Blue shift");
        blueSlider.addChangeListener(e -> {
            float oldValue = yellowBlue;
            yellowBlue = blueSlider.getValue() / 100.0f;
            if (oldValue != yellowBlue) {
                variationsPanel.configChanged();
            }
        });

        Insets insets = new Insets(2, 4, 2, 4);
        GridBagConstraints labelConstraint = new GridBagConstraints(0, 0, 1, 1, 0, 0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE, insets, 0, 0);
        GridBagConstraints sliderConstraint = new GridBagConstraints(1, 0, 1, 1, 1.0, 0,
                GridBagConstraints.EAST,
                GridBagConstraints.HORIZONTAL, insets, 0, 0);

        p.add(new JLabel("C-R:"), labelConstraint);
        p.add(redSlider, sliderConstraint);

        labelConstraint.gridy = 1;
        p.add(new JLabel("M-G:"), labelConstraint);
        sliderConstraint.gridy = 1;
        p.add(greenSlider, sliderConstraint);

        labelConstraint.gridy = 2;
        p.add(new JLabel("Y-B:"), labelConstraint);
        sliderConstraint.gridy = 2;
        p.add(blueSlider, sliderConstraint);

        return p;
    }
}
