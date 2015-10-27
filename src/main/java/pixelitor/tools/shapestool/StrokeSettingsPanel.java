/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.tools.ShapeType;
import pixelitor.tools.StrokeType;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.GridLayout;

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
        capJoinPanel.setLayout(new GridLayout(2, 2, 0, 0));

        capJoinPanel.add(new JLabel("Endpoint Cap:", JLabel.RIGHT));

        JComponent capSelector = capParam.createGUI();

        capSelector.setToolTipText("The shape of the line endpoints");
        capParam.setToolTip("The shape of the line endpoints");

        capJoinPanel.add(capSelector);

        capJoinPanel.add(new JLabel("Corner Join:", JLabel.RIGHT));
        JComponent joinSelector = joinParam.createGUI();

        joinParam.setToolTip("The way lines connect at the corners");
        capJoinPanel.add(joinSelector);
        return capJoinPanel;
    }

    private static JPanel createStrokeTypePanel(EnumParam<StrokeType> strokeTypeParam,
                                         EnumParam<ShapeType> shapeTypeParam,
                                         BooleanParam dashedParam) {
        JPanel strokeTypePanel = new JPanel();
        strokeTypePanel.setBorder(BorderFactory.createTitledBorder("Stroke Type"));

        strokeTypePanel.setLayout(new GridLayout(3, 2, 0, 0));

        shapeTypeParam.selectAndSetAsDefault(KIWI);

        Utils.setupEnableOtherIf(strokeTypeParam, shapeTypeParam,
                strokeType -> strokeType == SHAPE);

        Utils.setupDisableOtherIf(strokeTypeParam, dashedParam,
                strokeType -> strokeType != BASIC
                        && strokeType != ZIGZAG
                        && strokeType != SHAPE);

        strokeTypePanel.add(new JLabel("Line Type:", JLabel.RIGHT));
        strokeTypePanel.add(strokeTypeParam.createGUI());

        strokeTypePanel.add(new JLabel("Shape:", JLabel.RIGHT));
        strokeTypePanel.add(shapeTypeParam.createGUI());

        strokeTypePanel.add(new JLabel("Dashed:", JLabel.RIGHT));
        strokeTypePanel.add(dashedParam.createGUI());

        return strokeTypePanel;
    }
}
