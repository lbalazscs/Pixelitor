/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.layers;

import javax.swing.*;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.Color;
import java.awt.Graphics;

/**
 *
 */
public class LayerButtonUI extends BasicToggleButtonUI {
    private static final Color selectedColor = new Color(48, 76, 111);

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        Color oldColor = g.getColor();

        g.setColor(selectedColor);
        g.fillRoundRect(0, 0, b.getWidth(), b.getHeight(), 5, 5);
        g.setColor(oldColor);
    }
}
