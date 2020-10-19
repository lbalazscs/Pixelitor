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

package pixelitor.filters.gui;

import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.WEST;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

/**
 * The GUI for an {@link AngleParam}, which can be
 * an {@link ElevationAngleParam}
 */
public class AngleParamGUI extends JPanel implements ParamGUI {
    private boolean userChangedSlider = true;
    private final SliderSpinner sliderSpinner;
    private final AbstractAngleUI angleUI;

    public AngleParamGUI(AngleParam angleParam) {
        setLayout(new BorderLayout(10, 0));

        // the selector UI depends on the specific class
        angleUI = angleParam.getAngleSelectorUI();
        add(angleUI, WEST);

        sliderSpinner = createSliderSpinner(angleParam, angleUI);
        add(sliderSpinner, CENTER);

        setBorder(createTitledBorder(angleParam.getName()));

        setupPreferredSize();
    }

    private void setupPreferredSize() {
        var origPS = getPreferredSize();
        var sliderPS = sliderSpinner.getPreferredSize();
        setPreferredSize(new Dimension(
                sliderPS.width,
                origPS.height));
    }

    private SliderSpinner createSliderSpinner(AngleParam angleParam,
                                              AbstractAngleUI angleUI) {
        var sliderModel = angleParam.createRangeParam();
        sliderModel.addChangeListener(e ->
                sliderModelChanged(angleParam, sliderModel));

        var retVal = new SliderSpinner(sliderModel, NONE, true);
        setupSliderTicks(retVal, angleParam.getMaxAngleInDegrees());

        angleParam.addChangeListener(e ->
                angleParamChanged(angleParam, angleUI, sliderModel));
        return retVal;
    }

    private void sliderModelChanged(AngleParam angleParam, RangeParam sliderModel) {
        if (userChangedSlider) {
            boolean trigger = !sliderModel.getValueIsAdjusting();

            int value = sliderModel.getValue();
            angleParam.setValueInDegrees(value, trigger);
        }
    }

    private void angleParamChanged(AngleParam angleParam,
                                   AbstractAngleUI angleUI,
                                   RangeParam sliderModel) {
        angleUI.repaint();
        try {
            userChangedSlider = false;
            double degrees = angleParam.getValueInDegrees();
            sliderModel.setValue(degrees, true);

            // this is necessary because if a 359->360 change originated from
            // the spinner, then the angle is reset to 0, this resets the slider
            // to 0, but this won't set the spinner to 0, because it sees
            // that the change came from the spinner... See issue #62
            if (degrees == 0) {
                sliderSpinner.forceSpinnerValueOnly(0);
            }
        } finally {
            userChangedSlider = true;
        }
    }

    // necessary because SliderSpinner sets up the ticks only if it has TextPosition.BORDER
    private static void setupSliderTicks(SliderSpinner sliderSpinner, int maxAngleInDegrees) {
        if (maxAngleInDegrees == 360) {
            sliderSpinner.setupTicks(90, 45);
        } else if (maxAngleInDegrees == 90) {
            sliderSpinner.setupTicks(15, 0);
        }
    }

    @Override
    public void updateGUI() {
        // nothing to do
    }

    @Override
    public void setEnabled(boolean enabled) {
        angleUI.setEnabled(enabled);
        sliderSpinner.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    @Override
    public void setName(String name) {
        super.setName(name);

        // help assertj-swing to find the slider
        sliderSpinner.setName(name);
    }

    @Override
    public void setToolTip(String tip) {
        setToolTipText(tip);
        angleUI.setToolTipText(tip);
        sliderSpinner.setToolTip(tip);
    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}

