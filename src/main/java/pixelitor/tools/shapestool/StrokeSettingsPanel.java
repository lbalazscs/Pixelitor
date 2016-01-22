/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.tools.shapestool;

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.tools.ShapeType;
import pixelitor.tools.StrokeType;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.GridBagLayout;

import static pixelitor.tools.ShapeType.KIWI;
import static pixelitor.tools.StrokeType.BASIC;
import static pixelitor.tools.StrokeType.SHAPE;
import static pixelitor.tools.StrokeType.ZIGZAG;

public class StrokeSettingsPanel extends JPanel {
    public StrokeSettingsPanel(RangeParam strokeWidthParam,
                               EnumParam<BasicStrokeCap> capParam,
                               EnumParam<BasicStrokeJoin> joinParam,
                               EnumParam<StrokeType> strokeTypeParam,
                               BooleanParam dashedParam, EnumParam<ShapeType> shapeTypeParam) {
//        super(PixelitorWindow.getInstance(), "Stroke Settings", "Close");

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JComponent strokeWidthGUI = strokeWidthParam.createGUI();
        add(strokeWidthGUI);

        JPanel capJoinPanel = createCapJoinPanel(capParam, joinParam);
        add(capJoinPanel);

        JPanel strokeTypePanel = createStrokeTypePanel(strokeTypeParam, shapeTypeParam, dashedParam);
        add(strokeTypePanel);
    }

    private static JPanel createCapJoinPanel(
            EnumParam<BasicStrokeCap> capParam,
            EnumParam<BasicStrokeJoin> joinParam) {
        JPanel capJoinPanel = new JPanel();
        capJoinPanel.setBorder(BorderFactory.createTitledBorder("Line Endpoints"));
        capJoinPanel.setLayout(new GridBagLayout());

        GridBagHelper gbh = new GridBagHelper(capJoinPanel);

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

        return capJoinPanel;
    }

    private static JPanel createStrokeTypePanel(EnumParam<StrokeType> strokeTypeParam,
                                         EnumParam<ShapeType> shapeTypeParam,
                                         BooleanParam dashedParam) {
        JPanel strokeTypePanel = new JPanel();
        strokeTypePanel.setBorder(BorderFactory.createTitledBorder("Stroke Type"));

        strokeTypePanel.setLayout(new GridBagLayout());

        shapeTypeParam.selectAndSetAsDefault(KIWI);

        Utils.setupEnableOtherIf(strokeTypeParam, shapeTypeParam,
                strokeType -> strokeType == SHAPE);

        Utils.setupDisableOtherIf(strokeTypeParam, dashedParam,
                strokeType -> strokeType != BASIC
                        && strokeType != ZIGZAG
                        && strokeType != SHAPE);

        GridBagHelper gbh = new GridBagHelper(strokeTypePanel);
        gbh.addLabelWithControl("Line Type:", strokeTypeParam.createGUI());
        gbh.addLabelWithControl("Shape:", shapeTypeParam.createGUI());
        gbh.addLabelWithControl("Dashed:", dashedParam.createGUI());

        return strokeTypePanel;
    }
}
