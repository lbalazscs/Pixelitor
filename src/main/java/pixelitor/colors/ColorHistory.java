/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * The history of the colors used as foreground, background and filter colors.
 */
public class ColorHistory {
    public static final ColorHistory INSTANCE = new ColorHistory();

    private static final int MAX_SIZE = 200;

    private final List<Color> colors;

    private ColorHistory() {
        colors = new ArrayList<>();
    }

    private void add(Color newColor) {
        int newRGB = newColor.getRGB();
        for (Color color : colors) {
            if (color.getRGB() == newRGB) {
                return;
            }
        }
        colors.add(newColor);
        if (colors.size() > MAX_SIZE) {
            colors.remove(0);
        }
    }

    public static void remember(Color c) {
        INSTANCE.add(c);
    }

    public void showDialog(Window window, ColorSwatchClickHandler clickHandler, boolean isFilter) {
        assert window != null;
        assert clickHandler != null;

        int numColors = colors.size();
        int colorsInRow = 10;
        int rows = 1 + (numColors - 1) / colorsInRow;
        var panel = new JPanel(new GridLayout(rows, colorsInRow, 2, 2));
        panel.setBorder(createEmptyBorder(2, 2, 2, 2));
        for (Color color : colors) {
            panel.add(new ColorSwatchButton(color, clickHandler, 0, 0));
        }

        Messages.showInStatusBar("Color History: " + (isFilter ?
            ColorSwatchClickHandler.FILTER_HTML_HELP :
            ColorSwatchClickHandler.STANDARD_HTML_HELP));

        new DialogBuilder()
            .title("Color History")
            .owner(window)
            .content(panel)
            .notModal()
            .noOKButton()
            .noCancelButton()
            .show();
    }
}
