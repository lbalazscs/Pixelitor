/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.WeaveFilter;
import pixelitor.colors.Colors;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Defines the rendering logic for a single cell in the grid.
 * Implementations of this interface are responsible for
 * drawing a representation of a specific integer value.
 */
public interface GridCellPainter {
    /**
     * Paints the visual representation for a cell's value.
     *
     * @param g        The Graphics2D context to draw on. The coordinate system is translated
     *                 so that (0,0) is the top-left corner of the cell.
     * @param cellSize The width and height of the cell to paint within.
     */
    void paint(Graphics2D g, int cellSize);

    static List<GridCellPainter> createForPixelMask() {
        // painter for value 0: black
        GridCellPainter painter0 = (g, cellSize) ->
            Colors.fillWith(Color.BLACK, g, cellSize, cellSize);

        // painter for value 1: white
        GridCellPainter painter1 = (g, cellSize) ->
            Colors.fillWith(Color.WHITE, g, cellSize, cellSize);

        return List.of(painter0, painter1);
    }

    /**
     * Creates a list of painters for the Weave filter's grid editor.
     */
    static List<GridCellPainter> createForWeave() {
        // colors from WeaveFilter
        Color HOR_THREAD_COLOR = new Color(WeaveFilter.H_THREAD_COLOR, true);
        Color VER_THREAD_COLOR = new Color(WeaveFilter.V_THREAD_COLOR, true);

        // the thread thickness is a fixed percentage of the cell size
        // corresponding to the default settings of the Weave filter
        double threadThicknessRatio = 0.62;

        // painter for value 0: vertical thread is on top
        GridCellPainter painter0 = (g, cellSize) -> {
            int thickness = (int) (cellSize * threadThicknessRatio);
            int halfThickness = thickness / 2;
            int center = cellSize / 2;

            // draw horizontal thread at the bottom
            g.setColor(HOR_THREAD_COLOR);
            g.fillRect(0, center - halfThickness, cellSize, thickness);

            // draw vertical thread on top
            g.setColor(VER_THREAD_COLOR);
            g.fillRect(center - halfThickness, 0, thickness, cellSize);
        };

        // painter for value 1: horizontal thread is on top
        GridCellPainter painter1 = (g, cellSize) -> {
            int thickness = (int) (cellSize * threadThicknessRatio);
            int halfThickness = thickness / 2;
            int center = cellSize / 2;

            // draw vertical thread at the bottom
            g.setColor(VER_THREAD_COLOR);
            g.fillRect(center - halfThickness, 0, thickness, cellSize);

            // draw horizontal thread on top
            g.setColor(HOR_THREAD_COLOR);
            g.fillRect(0, center - halfThickness, cellSize, thickness);
        };

        return List.of(painter0, painter1);
    }
}