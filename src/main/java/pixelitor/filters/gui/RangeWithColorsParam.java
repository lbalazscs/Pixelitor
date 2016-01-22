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

package pixelitor.filters.gui;

import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.Color;

/**
 * A range where the endpoints correspond to colors.
 */
public class RangeWithColorsParam extends RangeParam {
    private final Color leftColor;
    private final Color rightColor;

    public RangeWithColorsParam(Color leftColor, Color rightColor, String name, int minValue, int defaultValue, int maxValue) {
        super(name, minValue, defaultValue, maxValue);
        this.leftColor = leftColor;
        this.rightColor = rightColor;
    }

    @Override
    public JComponent createGUI() {
        SliderSpinner sliderSpinner = new SliderSpinner(this, leftColor, rightColor);
        paramGUI = sliderSpinner;
        setParamGUIEnabledState();
        return sliderSpinner;
    }
}
