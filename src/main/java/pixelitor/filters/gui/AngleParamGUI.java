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

package pixelitor.filters.gui;

import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

/**
 * The GUI for an {@link AngleParam}, which can be
 * an {@link ElevationAngleParam}
 */
public class AngleParamGUI extends JPanel implements ParamGUI {
    private boolean userChangedSpinner = true;
    private final SliderSpinner sliderSpinner;
    private final AbstractAngleUI selectorUI;

    public AngleParamGUI(AngleParam angleParam) {
        setLayout(new BorderLayout(10, 0));

        // the selector UI depends on the specific class
        selectorUI = angleParam.getAngleSelectorUI();
        add(selectorUI, BorderLayout.WEST);

        sliderSpinner = createSliderSpinner(angleParam, selectorUI);
        add(sliderSpinner, BorderLayout.CENTER);

        setBorder(BorderFactory.createTitledBorder(angleParam.getName()));

        setupPreferredSize();
    }

    private void setupPreferredSize() {
        Dimension origPS = getPreferredSize();
        Dimension sliderPS = sliderSpinner.getPreferredSize();
        setPreferredSize(new Dimension(
                sliderPS.width,
                origPS.height));
    }

    private SliderSpinner createSliderSpinner(AngleParam angleParam, AbstractAngleUI asc) {
        RangeParam spinnerModel = angleParam.createRangeParam();
        spinnerModel.addChangeListener(e -> {
            if (userChangedSpinner) {
                boolean trigger = !spinnerModel.getValueIsAdjusting();

                int value = spinnerModel.getValue();
                angleParam.setValueInDegrees(value, trigger);
            }
        });

        SliderSpinner retVal = new SliderSpinner(spinnerModel, NONE, true);

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
        selectorUI.setEnabled(enabled);
        sliderSpinner.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    @Override
    public void setToolTip(String tip) {
        setToolTipText(tip);
        selectorUI.setToolTipText(tip);
        sliderSpinner.setToolTip(tip);
    }
}

