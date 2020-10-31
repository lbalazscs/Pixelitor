/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import java.awt.*;
import java.awt.geom.QuadCurve2D;

import static java.awt.BorderLayout.CENTER;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.tools.shapes.ShapeType.KIWI;

/**
 * Stroke configuration used by the shapes tool and by
 * shape filters
 */
public class StrokeSettingsPanel extends JPanel {

    public StrokeSettingsPanel(StrokeParam sp) {
        RangeParam strokeWidthParam = sp.getStrokeWidthParam();

        setLayout(new GridBagLayout());

        JComponent strokeWidthGUI = strokeWidthParam.createGUI("width");
        GridBagConstraints gbc = new GridBagConstraints(
            0, 0, 1, 1,
            1.0, 0,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 0, 0),
            3, 3);
        add(strokeWidthGUI, gbc);

        JPanel capJoinPanel = createCapJoinPanel(sp);
        gbc.gridy = 1;
        add(capJoinPanel, gbc);

        JPanel strokeTypePanel = createStrokeTypePanel(sp);
        gbc.gridy = 2;
        add(strokeTypePanel, gbc);

        JPanel strokePreviewPanel = createStrokePreviewPanel(sp);
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(strokePreviewPanel, gbc);
    }

    private static JPanel createCapJoinPanel(StrokeParam sp) {
        EnumParam<BasicStrokeCap> capParam = sp.getStrokeCapParam();
        EnumParam<BasicStrokeJoin> joinParam = sp.getStrokeJoinParam();

        var p = new JPanel();
        p.setBorder(createTitledBorder("Line Endpoints"));
        p.setLayout(new GridBagLayout());

        var gbh = new GridBagHelper(p);

        JComponent capSelector = capParam.createGUI("cap");

        // Dirty trick: manually set the preferred width so that
        // the layout aligns with the layout in the other panel.
        // Doubling the width is about OK.
        Dimension dim = capSelector.getPreferredSize();
        dim.setSize(dim.getWidth() * 2, dim.getHeight());
        capSelector.setPreferredSize(dim);

        JComponent joinSelector = joinParam.createGUI("join");

        capParam.setToolTip("The shape of the line endpoints");
        joinParam.setToolTip("The way lines connect at the corners");

        gbh.addLabelAndControl(BasicStrokeCap.NAME + ":", capSelector);
        gbh.addLabelAndControl(BasicStrokeJoin.NAME + ":", joinSelector);

        return p;
    }

    private static JPanel createStrokeTypePanel(StrokeParam sp) {
        EnumParam<StrokeType> strokeTypeParam = sp.getStrokeTypeParam();
        BooleanParam dashedParam = sp.getDashedParam();
        EnumParam<ShapeType> shapeTypeParam = sp.getShapeTypeParam()
            .withDefault(KIWI);

        var p = new JPanel();
        p.setBorder(createTitledBorder("Stroke Type"));

        p.setLayout(new GridBagLayout());

        var gbh = new GridBagHelper(p);
        gbh.addLabelAndControl(strokeTypeParam.getName() + ":",
            strokeTypeParam.createGUI("strokeType"));

        gbh.addLabelAndControl(shapeTypeParam.getName() + ":",
            shapeTypeParam.createGUI("shapeType"));

        gbh.addLabelAndControl(dashedParam.getName() + ":",
            dashedParam.createGUI("dashed"));

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

        var p = new JPanel(new BorderLayout());
        p.add(preview, CENTER);
        p.setBorder(createTitledBorder("Stroke Preview"));

        return p;
    }
}
