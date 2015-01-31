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

import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Contains an AbstractAngleSelectorComponent and a SliderSpinner
 */
public class AngleSelector extends JPanel {
    private boolean userChangedSpinner = true;

    public AngleSelector(final AngleParam angleParam) {
        setLayout(new BorderLayout(10, 0));
        final AbstractAngleSelectorComponent asc = angleParam.getAngleSelectorComponent();
        add(asc, BorderLayout.WEST);

        final RangeParam spinnerModel = angleParam.createRangeParam();
        SliderSpinner sliderSpinner = new SliderSpinner(spinnerModel, SliderSpinner.TextPosition.NONE, true);

        sliderSpinner.setResettable(angleParam);
        int maxAngleInDegrees = angleParam.getMaxAngleInDegrees();
        if (maxAngleInDegrees == 360) {
            sliderSpinner.setupTicks(180, 90);
        } else if (maxAngleInDegrees == 90) {
            sliderSpinner.setupTicks(15, 0);
        }

        add(sliderSpinner, BorderLayout.CENTER);

        angleParam.addChangeListener(e -> {
            asc.repaint();
            userChangedSpinner = false;
            spinnerModel.setValue(angleParam.getValueInDegrees());
            userChangedSpinner = true;
        });
        spinnerModel.addChangeListener(e -> {
            if (userChangedSpinner) {
                boolean trigger = !spinnerModel.getValueIsAdjusting();

                int value = spinnerModel.getValue();
                angleParam.setValueInDegrees(value, trigger);
            }
        });

        setBorder(BorderFactory.createTitledBorder(angleParam.getName()));

        Dimension preferredSize = getPreferredSize();
        Dimension sliderPreferredSize = sliderSpinner.getPreferredSize();
        setPreferredSize(new Dimension(sliderPreferredSize.width, preferredSize.height));
    }
}

