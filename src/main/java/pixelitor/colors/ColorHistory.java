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

package pixelitor.colors;

import pixelitor.colors.palette.ColorSwatchButton;
import pixelitor.colors.palette.ColorSwatchClickHandler;
import pixelitor.gui.utils.DialogBuilder;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

public class ColorHistory {
    public static final ColorHistory FOREGROUND = new ColorHistory("Foreground Color History");
    public static final ColorHistory BACKGROUND = new ColorHistory("Background Color History");
    public static final ColorHistory FILTER = new ColorHistory("Filter Color History");

    private static final int MAX_SIZE = 200;

    private final List<Color> colors;
    private final String dialogTitle;

    private ColorHistory(String dialogTitle) {
        this.dialogTitle = dialogTitle;
        colors = new ArrayList<>();
    }

    public void add(Color c) {
        colors.add(c);
        if (colors.size() > MAX_SIZE) {
            colors.remove(0);
        }
    }

    public void showDialog(Window window, ColorSwatchClickHandler clickHandler) {
        assert window != null;
        assert clickHandler != null;

        int numColors = colors.size();
        int colorsInRow = 10;
        int rows = 1 + (numColors - 1) / colorsInRow;
        int cols = colorsInRow;
        JPanel form = new JPanel(new GridLayout(rows, cols, 2, 2));
        form.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        for (Color color : colors) {
            ColorSwatchButton swatch = new ColorSwatchButton(color, clickHandler, 0, 0);
            form.add(swatch);
        }

        new DialogBuilder()
                .title(dialogTitle)
                .parent(window)
                .form(form)
                .notModal()
                .noOKButton()
                .noCancelButton()
                .show();
    }
}
