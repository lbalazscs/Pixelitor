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

package pixelitor.minigui;

import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.utils.RailwayTrackStroke;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

public class RailwayStrokeTester extends JFrame {
    private Stroke testedStroke;

    private final RangeParam railDistanceModel = new RangeParam("Rail Distance", 1, 20, 100);
    private final RangeParam railWidthModel = new RangeParam("Rail Width", 1, 10, 50);
    private final RangeParam crosstieLengthModel = new RangeParam("Crosstie Length", 1, 60, 200);
    private final RangeParam crosstieWidthModel = new RangeParam("Crosstie Width", 1, 20, 50);
    private final RangeParam crosstieSpacingModel = new RangeParam("Crosstie Spacing", 1, 80, 200);

    private JComboBox<ShapeType> shapeSelector;
    private final DrawingPanel drawingPanel;

    private RailwayStrokeTester() {
        setTitle("Railway Track Stroke Tester");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        drawingPanel = new DrawingPanel();
        add(drawingPanel, BorderLayout.CENTER);

        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.WEST);

        updateStroke();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel paramsPanel = new JPanel();
        paramsPanel.setLayout(new GridBagLayout());
        paramsPanel.setBorder(new TitledBorder("Stroke Parameters"));
        GridBagHelper gbh = new GridBagHelper(paramsPanel, 5);

        crosstieLengthModel.ensureHigherValueThan(railDistanceModel);
        gbh.addFullRow(railDistanceModel);
        gbh.addFullRow(railWidthModel);
        gbh.addFullRow(crosstieLengthModel);
        gbh.addFullRow(crosstieWidthModel);
        gbh.addFullRow(crosstieSpacingModel);

        // Shape selector
        JPanel shapePanel = new JPanel();
        shapePanel.setBorder(new TitledBorder("Shape"));
        shapeSelector = new JComboBox<>(ShapeType.values());
        shapePanel.add(shapeSelector);

        ParamAdjustmentListener listener = this::updateStroke;
        railDistanceModel.setAdjustmentListener(listener);
        railWidthModel.setAdjustmentListener(listener);
        crosstieLengthModel.setAdjustmentListener(listener);
        crosstieWidthModel.setAdjustmentListener(listener);
        crosstieSpacingModel.setAdjustmentListener(listener);

        shapeSelector.addActionListener(e -> updateStroke());

        panel.add(paramsPanel, BorderLayout.NORTH);
        panel.add(shapePanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateStroke() {
        float railDistance = railDistanceModel.getValueAsFloat();
        float railWidth = railWidthModel.getValueAsFloat();
        float crosstieLength = crosstieLengthModel.getValueAsFloat();
        float crosstieWidth = crosstieWidthModel.getValueAsFloat();
        float crosstieSpacing = crosstieSpacingModel.getValueAsFloat();

        testedStroke = new RailwayTrackStroke(
            railDistance,
            railWidth,
            crosstieLength,
            crosstieWidth,
            crosstieSpacing);

        drawingPanel.repaint();
    }

    private class DrawingPanel extends JPanel {
        private static final int PANEL_WIDTH = 600;
        private static final int PANEL_HEIGHT = 400;
        private static final int MARGIN = 50;

        public DrawingPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            // Draw shape with railway stroke
            Shape shape = createSelectedShape();
            g2.setColor(Color.BLACK);
            g2.setStroke(testedStroke);
            g2.draw(shape);

            g2.setStroke(new BasicStroke(3));
            g2.setColor(Color.RED);
            g2.draw(shape);

            g2.dispose();
        }

        private Shape createSelectedShape() {
            int width = getWidth() - 2 * MARGIN;
            int height = getHeight() - 2 * MARGIN;

            ShapeType selectedType = (ShapeType) shapeSelector.getSelectedItem();
            Shape shape = selectedType.createShape(MARGIN, MARGIN, width, height);
            return shape;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");
            } catch (Exception e) {
                e.printStackTrace();
            }

            new RailwayStrokeTester();
        });
    }
}
