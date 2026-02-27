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

package pixelitor.colors;

import pixelitor.colors.palette.ColorSwatchButton;
import pixelitor.colors.palette.ColorSwatchClickHandler;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.LinkedHashSet;
import java.util.SequencedSet;

import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * A FIFO collection of recently used foreground, background and filter colors.
 */
public class ColorHistory {
    public static final ColorHistory INSTANCE = new ColorHistory();

    private static final int MAX_HISTORY_CAPACITY = 200;
    private static final int SWATCHES_PER_ROW = 10;

    private final SequencedSet<Color> colors;

    private ColorHistory() {
        // provides O(1) add/contains and maintains insertion order
        colors = new LinkedHashSet<>();
    }

    /**
     * Adds a color to the history, respecting capacity and avoiding duplicates.
     */
    private void add(Color newColor) {
        colors.remove(newColor); // remove duplicates
        boolean added = colors.add(newColor);
        assert added;

        if (colors.size() > MAX_HISTORY_CAPACITY) {
            colors.removeFirst();
        }
    }

    public static void remember(Color newColor) {
        INSTANCE.add(newColor);
    }

    /**
     * Shows a modeless dialog with the color history.
     */
    public void showDialog(Window window,
                           ColorSwatchClickHandler clickHandler,
                           boolean filterMode) {
        assert window != null;
        assert clickHandler != null;

        JPanel swatchPanel = createSwatchPanel(clickHandler);

        String helpText = filterMode
            ? ColorSwatchClickHandler.FILTER_HTML_HELP
            : ColorSwatchClickHandler.STANDARD_HTML_HELP;
        Messages.showStatusMessage("Color History: " + helpText);

        new DialogBuilder()
            .title("Color History")
            .owner(window)
            .content(swatchPanel)
            .modeless()
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
            swatchPanel.add(new ColorSwatchButton(color, clickHandler));
        }
        return swatchPanel;
    }
}
