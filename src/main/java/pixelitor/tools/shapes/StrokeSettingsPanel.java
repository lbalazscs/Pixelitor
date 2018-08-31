/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.gui.utils.GridBagHelper;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.QuadCurve2D;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.tools.shapes.ShapeType.KIWI;
import static pixelitor.tools.shapes.StrokeType.BASIC;
import static pixelitor.tools.shapes.StrokeType.SHAPE;
import static pixelitor.tools.shapes.StrokeType.ZIGZAG;

/**
 * Stroke configuration used by the shapes tool and by
 * shape filters
 */
public class StrokeSettingsPanel extends JPanel {

    public StrokeSettingsPanel(StrokeParam sp) {
        RangeParam strokeWidthParam = sp.getStrokeWidthParam();
        EnumParam<BasicStrokeCap> capParam = sp.getStrokeCapParam();
        EnumParam<BasicStrokeJoin> joinParam = sp.getStrokeJoinParam();
        EnumParam<StrokeType> strokeTypeParam = sp.getStrokeTypeParam();
        BooleanParam dashedParam = sp.getDashedParam();
        EnumParam<ShapeType> shapeTypeParam = sp.getShapeTypeParam();

        setLayout(new GridBagLayout());

        JComponent strokeWidthGUI = strokeWidthParam.createGUI();
        GridBagConstraints gbc = new GridBagConstraints(
                0, 0, 1, 1,
                1.0, 0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0),
                3, 3);
        add(strokeWidthGUI, gbc);

        JPanel capJoinPanel = createCapJoinPanel(capParam, joinParam);
        gbc.gridy = 1;
        add(capJoinPanel, gbc);

        JPanel strokeTypePanel = createStrokeTypePanel(strokeTypeParam,
                shapeTypeParam, dashedParam);
        gbc.gridy = 2;
        add(strokeTypePanel, gbc);

        JPanel strokePreviewPanel = createStrokePreviewPanel(sp);
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(strokePreviewPanel, gbc);
    }

    private static JPanel createCapJoinPanel(
            EnumParam<BasicStrokeCap> capParam,
            EnumParam<BasicStrokeJoin> joinParam) {
        JPanel p = new JPanel();
        p.setBorder(createTitledBorder("Line Endpoints"));
        p.setLayout(new GridBagLayout());

        GridBagHelper gbh = new GridBagHelper(p);

        JComponent capSelector = capParam.createGUI();

        // Dirty trick: manually set the preferred width so that
        // the layout aligns with the layout in the other panel.
        // Doubling the width is about OK.
        Dimension dim = capSelector.getPreferredSize();
        dim.setSize(dim.getWidth() * 2, dim.getHeight());
        capSelector.setPreferredSize(dim);

        capParam.setToolTip("The shape of the line endpoints");

        JComponent joinSelector = joinParam.createGUI();
        joinParam.setToolTip("The way lines connect at the corners");

        gbh.addLabelWithControl("Endpoint Cap:", capSelector);
        gbh.addLabelWithControl("Corner Join:", joinSelector);

        return p;
    }

    private static JPanel createStrokeTypePanel(EnumParam<StrokeType> strokeTypeParam,
                                                EnumParam<ShapeType> shapeTypeParam,
                                                BooleanParam dashedParam) {
        JPanel p = new JPanel();
        p.setBorder(createTitledBorder("Stroke Type"));

        p.setLayout(new GridBagLayout());

        shapeTypeParam.selectAndSetAsDefault(KIWI);

        strokeTypeParam.setupEnableOtherIf(shapeTypeParam,
                strokeType -> strokeType == SHAPE);

        strokeTypeParam.setupDisableOtherIf(dashedParam,
                strokeType -> strokeType != BASIC
                        && strokeType != ZIGZAG
                        && strokeType != SHAPE);

        GridBagHelper gbh = new GridBagHelper(p);
        gbh.addLabelWithControl("Line Type:", strokeTypeParam.createGUI());
        gbh.addLabelWithControl("Shape:", shapeTypeParam.createGUI());
        gbh.addLabelWithControl("Dashed:", dashedParam.createGUI());

        return p;
    }

    private static JPanel createStrokePreviewPanel(StrokeParam sp) {
        JComponent preview = new JComponent() {
            final Dimension size = new Dimension(120, 120);

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                int width = getWidth();
                int height = getHeight();
                g2.fillRect(0, 0, width, height);
//                Line2D shape = new Line2D.Double(width * 0.1, height * 0.5, width * 0.9, height * 0.5);
                QuadCurve2D.Double shape = new QuadCurve2D.Double(
                        width * 0.1, height * 0.4,
                        width * 0.5, height * 0.8,
                        width * 0.9, height * 0.4
                );
                g2.setColor(Color.BLACK);
                g2.setStroke(sp.createStroke());
                g2.draw(shape);
            }

            @Override
            public Dimension getMinimumSize() {
                return size;
            }

            @Override
            public Dimension getPreferredSize() {
                return size;
            }
        };
        sp.setPreviewer(preview);

        JPanel p = new JPanel(new BorderLayout());
        p.add(preview, BorderLayout.CENTER);
        p.setBorder(createTitledBorder("Stroke Preview"));

        return p;
    }
}
