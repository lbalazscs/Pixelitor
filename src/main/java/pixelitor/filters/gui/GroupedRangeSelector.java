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

package pixelitor.filters.gui;

import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.GridBagLayout;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

/**
 * GUI for a GroupedRangeParam
 */
public class GroupedRangeSelector extends JPanel implements ParamGUI {
    private final int numParams;
    private final GroupedRangeParam model;
    private final GridBagHelper gridBagHelper;

    public GroupedRangeSelector(GroupedRangeParam model) {
        this.model = model;
        numParams = model.getNumParams();

        setLayout(new GridBagLayout());
        gridBagHelper = new GridBagHelper(this);

        addSliderSpinners();

        if(model.isLinkable()) {
            addLinkCheckBox();
        }

        setBorder(BorderFactory.createTitledBorder(model.getName()));
    }

    private void addSliderSpinners() {
        for (int i = 0; i < numParams; i++) {
            RangeParam param = model.getRangeParam(i);
            // doesn't call param.createGUI because we don't want another border
            SliderSpinner slider = new SliderSpinner(param, NONE, AddDefaultButton.YES);
            slider.setupTicks();
            gridBagHelper.addLabelWithControl(param.getName() + ":", slider, i);
        }
    }

    private void addLinkCheckBox() {
        JCheckBox linkedCB = new JCheckBox();
        linkedCB.setModel(model.getCheckBoxModel());
        gridBagHelper.addLabelWithControl("Linked:", linkedCB, numParams);
        linkedCB.addActionListener(e -> model.setLinked(linkedCB.isSelected()));
    }

    @Override
    public void updateGUI() {
        // can be empty
    }

    @Override
    public void setToolTip(String tip) {
        // TODO
    }
}
