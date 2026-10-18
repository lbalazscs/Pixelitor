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

package pixelitor.colors.palette;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import static pixelitor.utils.Utils.toPercentage;

/**
 * A builder for creating a panel with labeled sliders in a {@link GridBagLayout}.
 */
final class SliderPanelBuilder {
    @FunctionalInterface
    interface FloatSetter {
        void set(float value);
    }

    private static final Insets SLIDER_INSETS = new Insets(2, 4, 2, 4);

    private final JPanel panel = new JPanel(new GridBagLayout());
    private final Runnable onChange;
    private int row = 0;

    SliderPanelBuilder(Runnable onChange) {
        this.onChange = onChange;
    }

    SliderPanelBuilder add(String label, String toolTip,
                           float initialValue, FloatSetter setter) {
        int initialPercent = toPercentage(initialValue);
        JSlider slider = new JSlider(0, 100, initialPercent);
        slider.setToolTipText(toolTip);

        int[] lastValue = {initialPercent};
        slider.addChangeListener(_ -> {
            int value = slider.getValue();
            if (value != lastValue[0]) {
                lastValue[0] = value;
                setter.set(value / 100.0f);
                onChange.run();
            }
        });

        var labelConstraints = new GridBagConstraints(0, row, 1, 1, 0, 0,
            GridBagConstraints.CENTER, GridBagConstraints.NONE, SLIDER_INSETS, 0, 0);
        panel.add(new JLabel(label), labelConstraints);

        var sliderConstraints = new GridBagConstraints(1, row, 1, 1, 1.0, 0,
            GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, SLIDER_INSETS, 0, 0);
        panel.add(slider, sliderConstraints);

        row++;
        return this;
    }

    JPanel build() {
        return panel;
    }
}
