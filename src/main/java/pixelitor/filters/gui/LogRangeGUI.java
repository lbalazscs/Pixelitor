/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Pixelitor;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.text.NumberFormat;
import java.util.Locale;

public class LogRangeGUI extends JPanel implements ParamGUI {
    private final LogZoomParam model;
    private final JLabel valueLabel;
    private final SliderSpinner slider;

    @SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
    private static final NumberFormat format = NumberFormat.getIntegerInstance(
            Pixelitor.SYS_LOCALE == null ? Locale.getDefault() : Pixelitor.SYS_LOCALE);

    public LogRangeGUI(LogZoomParam model) {
        this.model = model;

        setLayout(new GridBagLayout());
        GridBagHelper gbh = new GridBagHelper(this);

        slider = new SliderSpinner(model, SliderSpinner.TextPosition.NONE, true);
        slider.addChangeListener(e -> sliderChanged());

        gbh.addLabelWithControl("Log:", slider);
        valueLabel = new JLabel();
        updateValueLabel();
        gbh.addLabelWithControl("Value:", valueLabel);

        setBorder(BorderFactory.createTitledBorder("Zoom"));
    }

    private void updateValueLabel() {
        long zoomValue = (long) model.getZoomValue();

        valueLabel.setText(format.format(zoomValue) + " %");
    }

    private void sliderChanged() {
        updateValueLabel();
//        model.setValueIsAdjusting(slider.getValueIsAdjusting());
//        model.setValue(slider.getValue(), !slider.getValueIsAdjusting());
    }

    @Override
    public void updateGUI() {
        slider.setValue(model.getValue());
    }

    @Override
    public void setToolTip(String tip) {
        slider.setToolTipText(tip);
    }
}
