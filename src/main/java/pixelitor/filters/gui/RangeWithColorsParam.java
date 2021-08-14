/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
 * Its {@link ParamGUI} is a colorized SliderSpinner
 */
public class RangeWithColorsParam extends RangeParam {
    private final Color leftColor;
    private final Color rightColor;

    public RangeWithColorsParam(Color leftColor, Color rightColor, String name, int min, int def, int max) {
        super(name, min, def, max);
        this.leftColor = leftColor;
        this.rightColor = rightColor;
    }

    @Override
    public JComponent createGUI() {
        var sliderSpinner = new SliderSpinner(this, leftColor, rightColor);
        paramGUI = sliderSpinner;
        afterGUICreation();
        return sliderSpinner;
    }
}
