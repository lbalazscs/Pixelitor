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

package pixelitor.filters.gui;

import java.awt.Graphics;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

/**
 * Utility class for rendering position indicators (crosshairs and central markers).
 */
public final class CrosshairPainter {
    public static final int MARKER_SIZE = 10;
    private static final int MARKER_OFFSET = MARKER_SIZE / 2;

    private CrosshairPainter() {
    }

    /**
     * Paints both the crosshair and the central square marker.
     */
    public static void paint(Graphics g, int x, int y, int width, int height) {
        drawCrosshair(g, x, y, width, height);
        drawCentralMarker(g, x, y);
    }

    /**
     * Draws a 1px white crosshair with a 1px black outline.
     */
    public static void drawCrosshair(Graphics g, int x, int y, int width, int height) {
        g.setColor(BLACK);
        // black outline for vertical line
        if (x > 0) {
            g.drawLine(x - 1, 0, x - 1, height - 1); // west
        }
        if (x < width - 1) {
            g.drawLine(x + 1, 0, x + 1, height - 1); // east
        }
        // black outline for horizontal line
        if (y > 0) {
            g.drawLine(0, y - 1, width - 1, y - 1); // north
        }
        if (y < height - 1) {
            g.drawLine(0, y + 1, width - 1, y + 1); // south
        }

        g.setColor(WHITE);
        // white center lines
        g.drawLine(x, 0, x, height - 1); // vertical
        g.drawLine(0, y, width - 1, y); // horizontal
    }

    /**
     * Draws the central square marker.
     */
    public static void drawCentralMarker(Graphics g, int x, int y) {
        g.setColor(BLACK);
        g.drawRect(x - MARKER_OFFSET, y - MARKER_OFFSET, MARKER_SIZE, MARKER_SIZE);
        g.setColor(WHITE);
        g.fillRect(x - MARKER_OFFSET + 1, y - MARKER_OFFSET + 1, MARKER_SIZE - 2, MARKER_SIZE - 2);
    }
}
