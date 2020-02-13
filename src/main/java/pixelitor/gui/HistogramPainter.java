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

package pixelitor.gui;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import static java.awt.Color.BLACK;

/**
 * This component calculates and paints a histogram.
 */
public class HistogramPainter extends JComponent {
    private static final int PREFERRED_HEIGHT = 100;

    private int[] values = null;
    private int maxValue = 0;
    private final Color color;

    public HistogramPainter(Color color) {
        this.color = color;

        setPreferredSize(new Dimension(257, PREFERRED_HEIGHT + 2));
    }

    /**
     * Sets the values array describing the statistical values in a channel
     */
    public void updateData(int[] values) {
        this.values = values;
        maxValue = 0;
        for (int value : values) {
            if (maxValue < value) {
                maxValue = value;
            }
        }
    }

    public void allViewsClosed() {
        maxValue = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(BLACK);
        g.drawRect(0, 0, 257, 102);

        if (maxValue == 0) { // no image
            return;
        }
        if (values == null) {
            return;
        }

        int x = 0;

        g.setColor(color);
        for (int i = 0; i < 256; i++) {
            x++;
            int value = values[i];
            if (value > 0) {
                int lineHeight = (int) (PREFERRED_HEIGHT * ((double) value / maxValue));
                int yTop = 1 + PREFERRED_HEIGHT - lineHeight;
                int yBottom = PREFERRED_HEIGHT + 1;
                g.drawLine(x, yTop, x, yBottom);
            }
        }
    }
}
