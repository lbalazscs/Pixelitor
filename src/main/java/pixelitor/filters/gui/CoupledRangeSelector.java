/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.gui;

import pixelitor.utils.GUIUtils;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * GUI for a CoupledRangeParam
 */
public class CoupledRangeSelector extends JPanel {
    public CoupledRangeSelector(final CoupledRangeParam model) {
        setLayout(new GridBagLayout());

        RangeParam firstRangeParam = model.getFirstRangeParam();
        RangeParam secondRangeParam = model.getSecondRangeParam();

        final JCheckBox coupledCB = new JCheckBox();
        coupledCB.setModel(model.getCheckBoxModel());


        SliderSpinner slider1 = new SliderSpinner(firstRangeParam, true, SliderSpinner.TextPosition.NONE);
        slider1.setupTicks();
        SliderSpinner slider2 = new SliderSpinner(secondRangeParam, true, SliderSpinner.TextPosition.NONE);
        slider2.setupTicks();
        GridBagHelper.addLabelWithControl(this, firstRangeParam.getName(), 0, slider1);
        GridBagHelper.addLabelWithControl(this, secondRangeParam.getName(), 1, slider2);
        GridBagHelper.addLabelWithControl(this, "Coupled:", 2, coupledCB);


        coupledCB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setCoupled(coupledCB.isSelected());
            }
        });

        setBorder(BorderFactory.createTitledBorder(model.getName()));
    }

    public static void main(String[] args) {
        CoupledRangeParam model = new CoupledRangeParam("HUHU", 0, 100, 50);
        GUIUtils.testJComponent(new CoupledRangeSelector(model));
    }

}
