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
import javax.swing.event.ChangeListener;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import static pixelitor.utils.Utils.toPercentage;

/**
 * An interactive configuration for a {@link Palette}, typically with sliders.
 */
public interface PaletteConfig {
    Insets SLIDER_INSETS = new Insets(2, 4, 2, 4);

    /**
     * Creates the configuration panel with its controls.
     */
    JPanel createConfigPanel(PalettePanel palettePanel);

    /**
     * Creates and adds a labeled JSlider as a new row to a panel.
     */
    static JSlider addSliderRow(String label, String toolTip, ChangeListener listener, float value, JPanel p, int y) {
        JSlider slider = new JSlider(0, 100, toPercentage(value));
        slider.addChangeListener(listener);
        slider.setToolTipText(toolTip);

        var labelConstraints = new GridBagConstraints(0, y, 1, 1, 0, 0,
            GridBagConstraints.CENTER, GridBagConstraints.NONE, SLIDER_INSETS, 0, 0);
        p.add(new JLabel(label), labelConstraints);

        var sliderConstraints = new GridBagConstraints(1, y, 1, 1, 1.0, 0,
            GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, SLIDER_INSETS, 0, 0);
        p.add(slider, sliderConstraints);

        return slider;
    }

    /**
     * A simple, non-interactive config that returns an empty panel.
     */
    class NoOpPaletteConfig implements PaletteConfig {
        @Override
        public JPanel createConfigPanel(PalettePanel palettePanel) {
            // return an empty panel, without any controls
            return new JPanel();
        }
    }
}
