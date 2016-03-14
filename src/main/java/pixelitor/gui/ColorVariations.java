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
import java.awt.Dimension;
import java.awt.FlowLayout;

public class ColorVariations extends JPanel {
    private ColorVariations() {
        setLayout(new FlowLayout());

        Color color = FgBgColors.getFG();
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
        add(new ColorSwitchButton(c));
    }

    public static void showInDialog(PixelitorWindow pw) {
        ColorVariations colorVariations = new ColorVariations();

        new DialogBuilder()
                .title("Color Variations")
                .parent(pw)
                .form(colorVariations)
                .notModal()
                .noOKButton()
                .noCancelButton()
                .noGlobalKeyChange()
                .show();
    }

    private static class ColorSwitchButton extends JButton {
        private static final Dimension size = new Dimension(32, 32);

        public ColorSwitchButton(Color color) {
            setBackground(color);
            setPreferredSize(size);
            setMinimumSize(size);

            addActionListener(e -> FgBgColors.setFG(color));
        }
    }
}


