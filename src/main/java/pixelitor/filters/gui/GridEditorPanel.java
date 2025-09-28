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

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A component for displaying and editing the 2D integer matrix of a {@link GridParam}.
 */
public class GridEditorPanel extends JPanel {
    private static final Dimension PREFERRED_PANEL_SIZE = new Dimension(120, 120);
    private static final Color GRID_LINE_COLOR = Color.DARK_GRAY;

    private final GridParam gridParam;
    private List<GridCellPainter> painters;

    // dynamically calculated based on panel size and grid dimensions
    private int cellSize;
    private int xOffset;
    private int yOffset;

    public GridEditorPanel(GridParam gridParam, List<GridCellPainter> painters) {
        this.gridParam = Objects.requireNonNull(gridParam);

        setPainters(painters);

        setPreferredSize(PREFERRED_PANEL_SIZE);
        setMinimumSize(PREFERRED_PANEL_SIZE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
        });
    }

    private void handleMousePressed(MouseEvent e) {
        if (!isEnabled() || cellSize == 0) {
            return;
        }

        int gridCols = gridParam.getGridCols();
        int gridRows = gridParam.getGridRows();

        // check if the click is within the visible grid area
        if (e.getX() < xOffset || e.getX() >= xOffset + gridCols * cellSize ||
            e.getY() < yOffset || e.getY() >= yOffset + gridRows * cellSize) {
            return;
        }

        int col = (e.getX() - xOffset) / cellSize;
        int row = (e.getY() - yOffset) / cellSize;

        // updates the model, which will trigger a repaint of the cell
        gridParam.cycleData(row, col);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (gridParam == null || painters == null || painters.isEmpty()) {
            return;
        }

        updateGridMetrics();

        if (cellSize > 0) {
            Graphics2D g2 = (Graphics2D) g;
            paintCells(g2);
            paintGridLines(g2);
        }
    }

    private void updateGridMetrics() {
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        int rows = gridParam.getGridRows();
        int cols = gridParam.getGridCols();

        if (rows <= 0 || cols <= 0) {
            cellSize = 0;
            xOffset = 0;
            yOffset = 0;
            return;
        }

        int cellWidthBasedOnPanel = panelWidth / cols;
        int cellHeightBasedOnPanel = panelHeight / rows;
        cellSize = Math.min(cellWidthBasedOnPanel, cellHeightBasedOnPanel);

        int totalGridWidth = cols * cellSize;
        int totalGridHeight = rows * cellSize;

        xOffset = (panelWidth - totalGridWidth) / 2;
        yOffset = (panelHeight - totalGridHeight) / 2;
    }

    private void paintCells(Graphics2D g2) {
        int[][] data = gridParam.getData(); // get data from model for painting
        int rows = gridParam.getGridRows();
        int cols = gridParam.getGridCols();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int value = data[row][col];
                if (value >= 0 && value < painters.size()) {
                    GridCellPainter painter = painters.get(value);
                    // uses a new Graphics2D for each cell to prevent painters from interfering
                    Graphics2D cellGraphics = (Graphics2D) g2.create();
                    try {
                        cellGraphics.translate(xOffset + col * cellSize, yOffset + row * cellSize);
                        painter.paint(cellGraphics, cellSize);
                    } finally {
                        cellGraphics.dispose();
                    }
                }
            }
        }
    }

    private void paintGridLines(Graphics2D g2) {
        g2.setColor(GRID_LINE_COLOR);
        int rows = gridParam.getGridRows();
        int cols = gridParam.getGridCols();
        int totalWidth = cols * cellSize;
        int totalHeight = rows * cellSize;

        // draw horizontal lines
        for (int i = 0; i <= rows; i++) {
            g2.drawLine(xOffset, yOffset + i * cellSize, xOffset + totalWidth, yOffset + i * cellSize);
        }
        // draw vertical lines
        for (int i = 0; i <= cols; i++) {
            g2.drawLine(xOffset + i * cellSize, yOffset, xOffset + i * cellSize, yOffset + totalHeight);
        }
    }

    /**
     * Repaints the component when the grid model changes.
     */
    public void gridModelChanged() {
        repaint();
    }

    /**
     * Sets a new list of painters for rendering cells and repaints the component.
     */
    public void setPainters(List<GridCellPainter> newPainters) {
        if (newPainters.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.painters = new ArrayList<>(newPainters);
        repaint();
    }
}
