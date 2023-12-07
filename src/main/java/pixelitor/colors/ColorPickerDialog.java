/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.colors;

import com.bric.swing.ColorSwatch;
import pixelitor.gui.utils.GUIUtils;

import java.awt.Color;
import java.util.function.Consumer;

/**
 * Color picker dialog helper, makes using {@link ColorSwatch} easier.
 * When the user selects a new color, the provided action is performed.
 */
public class ColorPickerDialog {
    private final ColorSwatch colorSwatch;
    private final Consumer<Color> action;

    public ColorPickerDialog(ColorSwatch colorSwatch, Consumer<Color> action) {
        this.colorSwatch = colorSwatch;
        this.action = action;
        GUIUtils.addClickAction(colorSwatch, this::showColorDialog);
    }

    private void showColorDialog() {
        Colors.selectColorWithDialog(colorSwatch, "Select Color",
            colorSwatch.getForeground(), false,
            color -> {
                colorSwatch.setForeground(color);
                action.accept(color);
            }
        );
    }
}
