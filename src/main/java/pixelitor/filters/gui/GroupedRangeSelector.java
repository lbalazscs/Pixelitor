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

package pixelitor.filters.gui;

import pixelitor.utils.GUIUtils;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.awt.GridBagLayout;

/**
 * GUI for a GroupedRangeParam
 */
public class GroupedRangeSelector extends JPanel {
    public GroupedRangeSelector(final GroupedRangeParam model) {
        setLayout(new GridBagLayout());

        GridBagHelper gridBagHelper = new GridBagHelper(this);

        int numParams = model.getNumParams();
        for (int i = 0; i < numParams; i++) {
            RangeParam param = model.getRangeParam(i);
            SliderSpinner slider = new SliderSpinner(param, SliderSpinner.TextPosition.NONE, true);
            slider.setupTicks();
            gridBagHelper.addLabelWithControl(param.getName(), slider, i);
        }

        boolean linkable = model.isLinkable();
        if(linkable) {
            final JCheckBox linkedCB = new JCheckBox();
            linkedCB.setModel(model.getCheckBoxModel());
            gridBagHelper.addLabelWithControl("Linked:", linkedCB, numParams);
            linkedCB.addActionListener(e -> model.setLinked(linkedCB.isSelected()));
        }

        setBorder(BorderFactory.createTitledBorder(model.getName()));
    }

    public static void main(String[] args) {
        GroupedRangeParam model = new GroupedRangeParam("HUHU", 0, 100, 50);
        GUIUtils.testJComponent(new GroupedRangeSelector(model));
    }

}
