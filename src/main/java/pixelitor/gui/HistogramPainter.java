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
    private static final int MAX_LINE_HEIGHT = 100;
    public static final int PREFERRED_HEIGHT = MAX_LINE_HEIGHT + 2;

    private int[] values = null;
    private int maxValue = 0;
    private final Color color;

    public HistogramPainter(Color color) {
        this.color = color;

        setPreferredSize(new Dimension(257, PREFERRED_HEIGHT));
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

        int rectWidth = HistogramsPanel.HISTOGRAM_RESOLUTION + 1;
        int rectHeight = PREFERRED_HEIGHT;
        int rectX = (getWidth() - rectWidth) / 2;
        int rectY = (getHeight() - rectHeight) / 2;
        g.drawRect(rectX, rectY, rectWidth, rectHeight);

        if (maxValue == 0) { // no image
            return;
        }
        if (values == null) {
            return;
        }

        int maxY = 1 + MAX_LINE_HEIGHT + rectY;
        int x = rectX;
        g.setColor(color);
        for (int i = 0; i < 256; i++) {
            x++;
            int value = values[i];
            if (value > 0) {
                int lineHeight = (int) (MAX_LINE_HEIGHT * ((double) value / maxValue));
                int yTop = maxY - lineHeight;
                int yBottom = maxY;
                g.drawLine(x, yTop, x, yBottom);
            }
        }
    }
}
