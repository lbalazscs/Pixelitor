/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
 * A color history. There are separate histories
 * for the foreground, background and filter colors.
 */
public class ColorHistory {
    private final Type type;

    private enum Type {
        FG("Foreground Color History") {
        }, BG("Background Color History") {
        }, FLT("Filter Color History") {
            @Override
            public String getHelpText() {
                return "Filter Color History: " + ColorSwatchClickHandler.FILTER_HTML_HELP;
            }
        };

        final String title;

        Type(String title) {
            this.title = title;
        }

        public String getDialogTitle() {
            return title;
        }

        public String getHelpText() {
            return title + ": " + ColorSwatchClickHandler.STANDARD_HTML_HELP;
        }
    }

    public static final ColorHistory FOREGROUND = new ColorHistory(Type.FG);
    public static final ColorHistory BACKGROUND = new ColorHistory(Type.BG);
    public static final ColorHistory FILTER = new ColorHistory(Type.FLT);

    private static final int MAX_SIZE = 200;

    private final List<Color> colors;
    private final String dialogTitle;

    private ColorHistory(Type type) {
        this.type = type;
        dialogTitle = type.getDialogTitle();
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
        var panel = new JPanel(new GridLayout(rows, cols, 2, 2));
        panel.setBorder(createEmptyBorder(2, 2, 2, 2));
        for (Color color : colors) {
            panel.add(new ColorSwatchButton(color, clickHandler, 0, 0));
        }

        Messages.showInStatusBar(type.getHelpText());

        new DialogBuilder()
            .title(dialogTitle)
            .owner(window)
            .content(panel)
            .notModal()
            .noOKButton()
            .noCancelButton()
            .show();
    }
}
