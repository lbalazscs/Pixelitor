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

package pixelitor.minigui;

import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

public class GridTester extends JFrame {
    private static final int PADDING = 50;

    private final TestPanel testPanel;
    private final JComboBox<GridType> gridTypeComboBox;

    private enum GridType {
        SQUARE("Square") {
            @Override
            public void renderGrid(Graphics2D g, Color color, int lineWidh, int size, int canvasWidth, int canvasHeight) {
                ImageUtils.renderGrid(g, color, lineWidh, size, canvasWidth, canvasHeight);
            }
        }, BRICK("Brick") {
            @Override
            public void renderGrid(Graphics2D g, Color color, int lineWidh, int size, int canvasWidth, int canvasHeight) {
                ImageUtils.renderBrickGrid(g, color, size, canvasWidth, canvasHeight);
            }
        }, TRIANGLE("Triangle") {
            @Override
            public void renderGrid(Graphics2D g, Color color, int lineWidh, int size, int canvasWidth, int canvasHeight) {
                ImageUtils.renderTriangleGrid(g, color, lineWidh, size, canvasWidth, canvasHeight);
            }
        };

        private final String displayName;

        GridType(String displayName) {
            this.displayName = displayName;
        }

        public abstract void renderGrid(Graphics2D g, Color color, int lineWidh, int size, int canvasWidth, int canvasHeight);

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static class TestPanel extends JPanel {
        private GridType currentGridType = GridType.SQUARE;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            int canvasWidth = getWidth() - 2 * PADDING;
            int canvasHeight = getHeight() - 2 * PADDING;

            // draw canvas boundary
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawRect(PADDING, PADDING, canvasWidth, canvasHeight);

            // translate to canvas origin
            g2d.translate(PADDING, PADDING);

            currentGridType.renderGrid(g2d, Color.BLUE, 2, 30, canvasWidth, canvasHeight);

            g2d.translate(-PADDING, -PADDING);
        }

        public void setGridType(GridType type) {
            this.currentGridType = type;
            repaint();
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

        testPanel = new TestPanel();
        testPanel.setBackground(Color.WHITE);
        testPanel.setPreferredSize(new Dimension(400, 300));
        updateGridType();

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
            new GridTester().setVisible(true));
    }
}