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

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;

import static java.awt.Color.BLACK;

/**
 * This component calculates and paints a histogram.
 */
public class HistogramPainter extends JComponent {
    private static final int PREFERRED_HEIGHT = 100;

    private int[] values = null;
    private int maxValue = 0;
    private Color color = null;
    private final Insets insets;

    public HistogramPainter(Color color) {
        this.color = color;
        insets = getInsets();
        setPreferredSize(new Dimension(256 + insets.left + insets.right, PREFERRED_HEIGHT + insets.top + insets.bottom));
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

    /**
     * Called when all images have been closed
     */
    public void updateWithNothing() {
        maxValue = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (maxValue == 0) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        g.setColor(BLACK);
        g.drawRect(0, 0, width - 1, height - 1);

        if (values == null) {
            return;
        }

        int x = insets.left;

        for (int value : values) {
            int lineHeight = (int) (height * ((double) value / (double) maxValue));
            g.setColor(color);
            g.drawLine(x, (height - lineHeight), x, height);
            x++;
        }
    }
}
