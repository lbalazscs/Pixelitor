/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.Themes;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import static java.awt.Color.BLACK;
import static java.awt.Color.GRAY;

/**
 * Renders a histogram for a specific color channel.
 */
public class HistogramPainter extends JComponent {
    private static final int MAX_BAR_HEIGHT = 100;
    private static final int PREFERRED_WIDTH = HistogramsPanel.NUM_BINS + 1;
    public static final int PREFERRED_HEIGHT = MAX_BAR_HEIGHT + 2;

    private int[] frequencies = null;
    private int maxFrequency = 0;
    private final Color channelColor;
    private final boolean isLuminance;

    public HistogramPainter(Color channelColor, boolean isLuminance) {
        this.channelColor = channelColor;
        this.isLuminance = isLuminance;
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

    public void clearData() {
        maxFrequency = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // the start of the actual histogram
        int offsetX = (getWidth() - PREFERRED_WIDTH) / 2;
        int offsetY = (getHeight() - PREFERRED_HEIGHT) / 2;
        assert offsetY == 0; // it should always be tighly packed vertically

        // draw background
        if (isLuminance) {
            if (!Themes.getCurrent().isDark()) {
                g.setColor(GRAY);
                g.fillRect(offsetX, offsetY, PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        // draw border
        g.setColor(BLACK);
        g.drawRect(offsetX, offsetY, PREFERRED_WIDTH, PREFERRED_HEIGHT);

        if (maxFrequency == 0 || frequencies == null) {
            return; // no image
        }

        int baseY = 1 + MAX_BAR_HEIGHT + offsetY;
        int x = offsetX;
        g.setColor(channelColor);
        
        for (int i = 0; i < HistogramsPanel.NUM_BINS; i++) {
            x++;
            int frequency = frequencies[i];
            if (frequency > 0) {
                int barHeight = (int) (MAX_BAR_HEIGHT * ((double) frequency / maxFrequency));
                g.drawLine(x, baseY - barHeight, x, baseY);
            }
        }
    }
}
