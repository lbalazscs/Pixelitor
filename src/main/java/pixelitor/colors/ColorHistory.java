/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import java.util.ArrayDeque;
import java.util.Queue;

import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * The recently used foreground, background and filter colors in a FIFO queue.
 */
public class ColorHistory {
    public static final ColorHistory INSTANCE = new ColorHistory();

    private static final int MAX_HISTORY_CAPACITY = 200;
    private static final int SWATCHES_PER_ROW = 10;

    private final Queue<Color> colors;

    private ColorHistory() {
        colors = new ArrayDeque<>();
    }

    private void add(Color newColor) {
        int newRGB = newColor.getRGB();
        for (Color color : colors) {
            if (color.getRGB() == newRGB) {
                return;
            }
        }
        colors.add(newColor);
        if (colors.size() > MAX_HISTORY_CAPACITY) {
            colors.remove(); // Remove oldest color
        }
    }

    public static void remember(Color newColor) {
        INSTANCE.add(newColor);
    }

    public void showDialog(Window window,
                           ColorSwatchClickHandler clickHandler,
                           boolean filterMode) {
        assert window != null;
        assert clickHandler != null;

        JPanel swatchPanel = createSwatchPanel(clickHandler);

        Messages.showStatusMessage("Color History: " + (filterMode ?
            ColorSwatchClickHandler.FILTER_HTML_HELP :
            ColorSwatchClickHandler.STANDARD_HTML_HELP));

        new DialogBuilder()
            .title("Color History")
            .owner(window)
            .content(swatchPanel)
            .notModal()
            .noOKButton()
            .noCancelButton()
            .show();
    }

    private JPanel createSwatchPanel(ColorSwatchClickHandler clickHandler) {
        int numColors = colors.size();
        int numRows = 1 + (numColors - 1) / SWATCHES_PER_ROW;

        JPanel swatchPanel = new JPanel(new GridLayout(numRows, SWATCHES_PER_ROW, 2, 2));
        swatchPanel.setBorder(createEmptyBorder(2, 2, 2, 2));
        for (Color color : colors) {
            swatchPanel.add(new ColorSwatchButton(color, clickHandler, 0, 0));
        }
        return swatchPanel;
    }
}
