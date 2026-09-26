/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
 * Renders a histogram for a specific channel (such as an RGB color channel or luminance).
 */
public class HistogramPainter extends JComponent {
    private static final int MAX_BAR_HEIGHT = 100;
    public static final int PREFERRED_WIDTH = HistogramsPanel.NUM_BINS + 2;
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
        if (frequencies == null) {
            return;
        }
        for (int frequency : frequencies) {
            if (frequency > maxFrequency) {
                maxFrequency = frequency;
            }
        }
    }

    public void clearData() {
        frequencies = null;
        maxFrequency = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // the start of the actual histogram
        int offsetX = (getWidth() - PREFERRED_WIDTH) / 2;
        int offsetY = (getHeight() - PREFERRED_HEIGHT) / 2; // should be 0

        // draw background
        if (isLuminance) {
            // the luminance channel is drawn in white, which would be invisible
            // against a light theme's background, so give it a gray backdrop
            // (dark themes are already dark enough for white bars to stand out)
            if (!Themes.getActive().isDark()) {
                g.setColor(GRAY);
                g.fillRect(offsetX, offsetY, PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        // draw border (drawRect strokes (w+1) x (h+1), so subtract 1 to fit bounds)
        g.setColor(BLACK);
        g.drawRect(offsetX, offsetY, PREFERRED_WIDTH - 1, PREFERRED_HEIGHT - 1);

        if (maxFrequency == 0 || frequencies == null) {
            return; // no image (or completely transparent image)
        }

        // the baseline sits at the bottom-most pixel inside the border (y = 100 when offsetY == 0)
        int baseY = offsetY + MAX_BAR_HEIGHT;
        int x = offsetX;
        g.setColor(channelColor);

        for (int i = 0; i < HistogramsPanel.NUM_BINS; i++) {
            x++;
            int frequency = frequencies[i];
            if (frequency > 0) {
                int barHeight = (int) (MAX_BAR_HEIGHT * ((double) frequency / maxFrequency));
                if (barHeight > 0) {
                    // drawLine is endpoint-inclusive: baseY - (baseY - barHeight + 1) + 1 = barHeight pixels
                    g.drawLine(x, baseY - barHeight + 1, x, baseY);
                }
            }
        }
    }
}
