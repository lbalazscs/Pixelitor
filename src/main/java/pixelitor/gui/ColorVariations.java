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

package pixelitor.gui;

import pixelitor.FgBgColors;
import pixelitor.gui.utils.DialogBuilder;

import javax.swing.*;
import java.awt.Color;
import java.awt.FlowLayout;

public class ColorVariations extends JPanel {
    private final boolean fg;

    private ColorVariations(boolean fg) {
        this.fg = fg;
        setLayout(new FlowLayout());

        Color color;
        if (fg) {
            color = FgBgColors.getFG();
        } else {
            color = FgBgColors.getBG();
        }

        Color darker = color.darker();
        Color darker2 = darker.darker();
        Color darker3 = darker2.darker();
        Color brighter = color.brighter();
        Color brighter2 = brighter.brighter();
        Color brighter3 = brighter2.brighter();

        addButton(darker3);
        addButton(darker2);
        addButton(darker);
        addButton(color);
        addButton(brighter);
        addButton(brighter2);
        addButton(brighter3);
    }

    private void addButton(Color c) {
        add(new ColorSwatchButton(c, fg));
    }

    public static void showInDialog(PixelitorWindow pw, boolean fg) {
        ColorVariations colorVariations = new ColorVariations(fg);

        String type = fg ? "Foreground" : "Background";
        String title = type + " Color Variations";

        new DialogBuilder()
                .title(title)
                .parent(pw)
                .form(colorVariations)
                .notModal()
                .noOKButton()
                .noCancelButton()
                .noGlobalKeyChange()
                .show();
    }

}


