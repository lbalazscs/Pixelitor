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

import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

/**
 * Contains an AbstractAngleSelectorComponent and a SliderSpinner
 */
public class AngleSelector extends JPanel implements ParamGUI {
    private boolean userChangedSpinner = true;
    private final SliderSpinner sliderSpinner;
    private final AbstractAngleSelectorComponent selectorGUI;

    public AngleSelector(AngleParam angleParam) {
        setLayout(new BorderLayout(10, 0));
        selectorGUI = angleParam.getAngleSelectorComponent();
        add(selectorGUI, BorderLayout.WEST);

        sliderSpinner = createSliderSpinner(angleParam, selectorGUI);
        add(sliderSpinner, BorderLayout.CENTER);

        setBorder(BorderFactory.createTitledBorder(angleParam.getName()));

        Dimension preferredSize = getPreferredSize();
        Dimension sliderPreferredSize = sliderSpinner.getPreferredSize();
        setPreferredSize(new Dimension(sliderPreferredSize.width, preferredSize.height));
    }

    private SliderSpinner createSliderSpinner(AngleParam angleParam, AbstractAngleSelectorComponent asc) {
        RangeParam spinnerModel = angleParam.createRangeParam();
        spinnerModel.addChangeListener(e -> {
            if (userChangedSpinner) {
                boolean trigger = !spinnerModel.getValueIsAdjusting();

                int value = spinnerModel.getValue();
                angleParam.setValueInDegrees(value, trigger);
            }
        });

        SliderSpinner retVal = new SliderSpinner(spinnerModel, NONE, AddDefaultButton.YES);

        retVal.setResettable(angleParam);
        int maxAngleInDegrees = angleParam.getMaxAngleInDegrees();
        if (maxAngleInDegrees == 360) {
            retVal.setupTicks(180, 90);
        } else if (maxAngleInDegrees == 90) {
            retVal.setupTicks(15, 0);
        }
        angleParam.addChangeListener(e -> {
            asc.repaint();
            userChangedSpinner = false;
            spinnerModel.setValue(angleParam.getValueInDegrees());
            userChangedSpinner = true;
        });
        return retVal;
    }

    @Override
    public void updateGUI() {
        // nothing to do
    }

    @Override
    public void setEnabled(boolean enabled) {
        selectorGUI.setEnabled(enabled);
        sliderSpinner.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    @Override
    public void setToolTip(String tip) {
        // TODO should have some generic tooltip
    }
}

