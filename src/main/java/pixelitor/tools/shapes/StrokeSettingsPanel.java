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

package pixelitor.tools.shapes;

import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.Themes;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.QuadCurve2D;

import static java.awt.BorderLayout.CENTER;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A GUI for configuring stroke settings.
 */
public class StrokeSettingsPanel extends JPanel {
    private static final int PREVIEW_SIZE = 120;

    public StrokeSettingsPanel(StrokeParam strokeParam) {
        super(new GridBagLayout());

        RangeParam strokeWidthParam = strokeParam.getStrokeWidthParam();
        JComponent strokeWidthGUI = strokeWidthParam.createGUI("width");
        GridBagConstraints gbc = new GridBagConstraints(
            0, 0, 1, 1,
            1.0, 0,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 0, 0),
            3, 3);
        add(strokeWidthGUI, gbc);

        JPanel capJoinPanel = createCapJoinPanel(strokeParam);
        gbc.gridy = 1;
        add(capJoinPanel, gbc);

        JPanel strokeTypePanel = createStrokeTypePanel(strokeParam);
        gbc.gridy = 2;
        add(strokeTypePanel, gbc);

        JPanel strokePreviewPanel = createStrokePreviewPanel(strokeParam);
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(strokePreviewPanel, gbc);
    }

    private static JPanel createCapJoinPanel(StrokeParam strokeParam) {
        EnumParam<StrokeCap> capParam = strokeParam.getStrokeCapParam();
        EnumParam<StrokeJoin> joinParam = strokeParam.getStrokeJoinParam();

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("Line Endpoints"));

        var gbh = new GridBagHelper(panel);

        JComponent capSelector = capParam.createGUI("cap");

        // manually set a preferred width to align the layout with the other panel
        Dimension dim = capSelector.getPreferredSize();
        // doubling the width is about right
        dim.setSize(dim.getWidth() * 2, dim.getHeight());
        capSelector.setPreferredSize(dim);

        JComponent joinSelector = joinParam.createGUI("join");

        gbh.addLabelAndControl(StrokeCap.NAME + ":", capSelector);
        gbh.addLabelAndControl(StrokeJoin.NAME + ":", joinSelector);

        return panel;
    }

    private static JPanel createStrokeTypePanel(StrokeParam strokeParam) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("Stroke Type"));
        var gbh = new GridBagHelper(panel);

        gbh.addLabelAndControl(strokeParam.getStrokeTypeParam(), "strokeType");
        gbh.addLabelAndControl(strokeParam.getShapeTypeParam(), "shapeType");
        gbh.addLabelAndControl(strokeParam.getDashedParam(), "dashed");

        return panel;
    }

    private static JPanel createStrokePreviewPanel(StrokeParam strokeParam) {
        JComponent previewer = new StrokePreviewer(strokeParam);
        strokeParam.setPreviewer(previewer);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(previewer, CENTER);
        panel.setBorder(createTitledBorder("Stroke Preview"));

        return panel;
    }

    private static class StrokePreviewer extends JComponent {
        private static final Dimension PREFERRED_DIMENSION = new Dimension(PREVIEW_SIZE, PREVIEW_SIZE);
        private final StrokeParam strokeParam;

        StrokePreviewer(StrokeParam strokeParam) {
            this.strokeParam = strokeParam;
        }

        @Override
        protected void paintComponent(Graphics g) {
            boolean darkTheme = Themes.getActive().isDark();

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            // fill background
            int width = getWidth();
            int height = getHeight();
            g2.setColor(darkTheme ? Color.BLACK : Color.WHITE);
            g2.fillRect(0, 0, width, height);

            // paint the stroke preview
            QuadCurve2D.Double shape = new QuadCurve2D.Double(
                width * 0.1, height * 0.4,
                width * 0.5, height * 0.8,
                width * 0.9, height * 0.4
            );
            g2.setColor(darkTheme ? Color.WHITE : Color.BLACK);
            g2.setStroke(strokeParam.createStroke());
            g2.draw(shape);
        }

        @Override
        public Dimension getMinimumSize() {
            return PREFERRED_DIMENSION;
        }

        @Override
        public Dimension getPreferredSize() {
            return PREFERRED_DIMENSION;
        }
    }
}
