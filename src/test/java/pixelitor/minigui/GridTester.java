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

package pixelitor.minigui;

import pixelitor.filters.jhlabsproxies.JHPixelate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

public class GridTester extends JFrame {
    private static final int PADDING = 50;
    private static final int MIN_GRID_SIZE = 10;
    private static final int MAX_GRID_SIZE = 100;
    private static final int INITIAL_GRID_SIZE = 30;
    private static final int STROKE_WIDTH = 2;

    private final TestPanel testPanel;
    private final JComboBox<GridType> gridTypeComboBox;
    private final JSlider gridSizeSlider;

    private enum GridType {
        SQUARE("Square") {
            @Override
            public void renderGrid(Graphics2D g, int size, int canvasWidth, int canvasHeight) {
                JHPixelate.renderGrid(g, STROKE_WIDTH, size, canvasWidth, canvasHeight);
            }
        }, BRICK("Brick") {
            @Override
            public void renderGrid(Graphics2D g, int size, int canvasWidth, int canvasHeight) {
                JHPixelate.renderBrickGrid(g, size, canvasWidth, canvasHeight);
            }
        }, TRIANGLE("Triangle") {
            @Override
            public void renderGrid(Graphics2D g, int size, int canvasWidth, int canvasHeight) {
                JHPixelate.renderTriangleGrid(g, size, canvasWidth, canvasHeight);
            }
        }, HEXAGON("Hexagon") {
            @Override
            public void renderGrid(Graphics2D g, int size, int canvasWidth, int canvasHeight) {
                JHPixelate.renderHexagonGrid(g, size, canvasWidth, canvasHeight);
            }
        };

        private final String displayName;

        GridType(String displayName) {
            this.displayName = displayName;
        }

        public abstract void renderGrid(Graphics2D g, int size, int canvasWidth, int canvasHeight);

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static class TestPanel extends JPanel {
        private GridType currentGridType = GridType.SQUARE;
        private int currentGridSize = INITIAL_GRID_SIZE;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            int canvasWidth = getWidth() - 2 * PADDING;
            int canvasHeight = getHeight() - 2 * PADDING;
            if (canvasWidth <= 0 || canvasHeight <= 0) {
                return;
            }

            // draw canvas boundary
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawRect(PADDING, PADDING, canvasWidth, canvasHeight);

            // translate to canvas origin
            g2.translate(PADDING, PADDING);

            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(STROKE_WIDTH));
            currentGridType.renderGrid(g2, currentGridSize, canvasWidth, canvasHeight);

            g2.translate(-PADDING, -PADDING);
        }

        public void setGridType(GridType type) {
            this.currentGridType = type;
            repaint();
        }

        public void setGridSize(int size) {
            if (size >= MIN_GRID_SIZE && size <= MAX_GRID_SIZE) {
                this.currentGridSize = size;
                repaint();
            }
        }
    }

    private GridTester() {
        setTitle("Grid Renderer Test");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel controlPanel = new JPanel();
        gridTypeComboBox = new JComboBox<>(GridType.values());
        gridTypeComboBox.addActionListener(e -> updateGridType());
        controlPanel.add(new JLabel("Grid Type:"));
        controlPanel.add(gridTypeComboBox);

        // space between combo box and slider label
        controlPanel.add(Box.createHorizontalStrut(15));

        gridSizeSlider = new JSlider(JSlider.HORIZONTAL, MIN_GRID_SIZE, MAX_GRID_SIZE, INITIAL_GRID_SIZE);
        gridSizeSlider.setMajorTickSpacing(10);
        gridSizeSlider.setMinorTickSpacing(5);
        gridSizeSlider.setPaintTicks(true);
        gridSizeSlider.setPaintLabels(true);
        gridSizeSlider.addChangeListener(e -> updateGridSize());
        controlPanel.add(gridSizeSlider);

        testPanel = new TestPanel();
        testPanel.setBackground(Color.WHITE);
        testPanel.setPreferredSize(new Dimension(400, 300));
        updateGridType();
        updateGridSize();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                testPanel.repaint();
            }
        });

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(testPanel, BorderLayout.CENTER);
        add(mainPanel);

        pack();
        setLocationRelativeTo(null);
    }

    private void updateGridType() {
        testPanel.setGridType((GridType) gridTypeComboBox.getSelectedItem());
    }

    private void updateGridSize() {
        testPanel.setGridSize(gridSizeSlider.getValue());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
            new GridTester().setVisible(true));
    }
}
