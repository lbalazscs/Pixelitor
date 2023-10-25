/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
    private static final int PREFERRED_WIDTH = HistogramsPanel.NUM_BINS + 1;
    public static final int PREFERRED_HEIGHT = MAX_LINE_HEIGHT + 2;

    private int[] frequencies = null;
    private int maxFrequency = 0;
    private final Color color;

    public HistogramPainter(Color color) {
        this.color = color;

        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
    }

    public void updateData(int[] frequencies) {
        this.frequencies = frequencies;
        maxFrequency = 0;
        for (int frequency : frequencies) {
            if (maxFrequency < frequency) {
                maxFrequency = frequency;
            }
        }
    }

    public void allViewsClosed() {
        maxFrequency = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(BLACK);

        int rectX = (getWidth() - PREFERRED_WIDTH) / 2;
        int rectY = (getHeight() - PREFERRED_HEIGHT) / 2;
        g.drawRect(rectX, rectY, PREFERRED_WIDTH, PREFERRED_HEIGHT);

        if (maxFrequency == 0) { // no image
            return;
        }
        if (frequencies == null) {
            return;
        }

        int maxY = 1 + MAX_LINE_HEIGHT + rectY;
        int x = rectX;
        g.setColor(color);
        for (int i = 0; i < HistogramsPanel.NUM_BINS; i++) {
            x++;
            int frequency = frequencies[i];
            if (frequency > 0) {
                int lineHeight = (int) (MAX_LINE_HEIGHT * ((double) frequency / maxFrequency));
                g.drawLine(x, maxY - lineHeight, x, maxY);
            }
        }
    }
}
