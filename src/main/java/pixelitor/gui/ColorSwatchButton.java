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

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;

public class ColorSwatchButton extends JButton {
    private static final Dimension size = new Dimension(32, 32);

    public ColorSwatchButton(Color color, boolean fg) {
        setBackground(color);
        setPreferredSize(size);
        setMinimumSize(size);

        addActionListener(e -> {
            if (fg) {
                FgBgColors.setFG(color);
            } else {
                FgBgColors.setBG(color);
            }
        });
    }
}
