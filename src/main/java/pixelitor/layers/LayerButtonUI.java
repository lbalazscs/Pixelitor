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

package pixelitor.layers;

import javax.swing.*;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 *
 */
public class LayerButtonUI extends BasicToggleButtonUI {
    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        Color oldColor = g.getColor();

        Graphics2D g2 = (Graphics2D) g;
        Object oldAA = g2.getRenderingHint(KEY_ANTIALIASING);

        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setColor(LayerButton.SELECTED_COLOR);
        g.fillRoundRect(0, 0, b.getWidth(), b.getHeight(), 10, 10);

        g.setColor(oldColor);
        g2.setRenderingHint(KEY_ANTIALIASING, oldAA);
    }
}
