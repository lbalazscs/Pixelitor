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

package pixelitor.gui.utils;

import pixelitor.filters.gui.DefaultButton;
import pixelitor.filters.gui.ParamGUI;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.Resettable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Dictionary;
import java.util.Enumeration;

import static java.awt.Color.GRAY;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A GUI Component consisting of a JSlider, a JSpinner and optionally a default button.
 * The slider and the spinner are synchronized
 */
public class SliderSpinner extends JPanel implements ChangeListener, ParamGUI {
    private final JLabel label;

    // if set to non-null, its reset is called instead of the reset this object
    private Resettable resettableParam;

    public enum TextPosition {
        BORDER, WEST, NORTH, NONE
    }

    private final JSlider slider;
    private final JSpinner spinner;
    private DefaultButton defaultButton;
    private final RangeParam model;

    private Color leftColor;
    private Color rightColor;
    private final boolean colorsUsed;

    private boolean sliderMoved = false;
    private boolean spinnerMoved = false;

    public SliderSpinner(RangeParam model, TextPosition position, boolean addDefaultButton) {
        this(model, null, null, position, addDefaultButton);
    }

    public SliderSpinner(RangeParam model, Color leftColor, Color rightColor) {
        this(model, leftColor, rightColor, TextPosition.BORDER, true);
    }

    private SliderSpinner(RangeParam model,
                          Color leftColor, Color rightColor,
                          TextPosition textPosition, boolean addDefaultButton) {

        setLayout(new BorderLayout());
        this.model = model;

        this.leftColor = leftColor;
        this.rightColor = rightColor;
        colorsUsed = leftColor != null;

        if (textPosition == TextPosition.BORDER) {
            if ((leftColor != null) && (rightColor != null)) {
                Border gradientBorder = new GradientBorder(leftColor, rightColor);
                this.setBorder(createTitledBorder(gradientBorder, model.getName()));
            } else {
                this.setBorder(createTitledBorder(model.getName()));
                this.leftColor = GRAY;
                this.rightColor = GRAY;
            }
        }

        this.slider = createSlider(model);
        if (textPosition == TextPosition.BORDER) {
            setupTicks();
        }

        this.spinner = createSpinner(model);

        if (textPosition == TextPosition.WEST) {
            label = new JLabel(model.getName() + ": ");
            add(label, BorderLayout.WEST);
        } else if (textPosition == TextPosition.NORTH) {
            label = new JLabel(model.getName() + ": ");
            add(label, BorderLayout.NORTH);
        } else {
            label = null;
        }

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        add(this.slider, BorderLayout.CENTER);
        p.add(this.spinner);

        if (addDefaultButton) {
            createDefaultButton(model);
            p.add(defaultButton);
        }
        add(p, BorderLayout.EAST);

//        showTicksAsFloat();
    }

    private JSlider createSlider(RangeParam model) {
        JSlider s = new JSlider(model);
        s.addChangeListener(this);
        return s;
    }

    private JSpinner createSpinner(RangeParam model) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(
                model.getValue(), //initial value
                model.getMinimum(), //min
                model.getMaximum(), //max
                1));
        s.addChangeListener(this);
        return s;
    }

    private void createDefaultButton(RangeParam model) {
        defaultButton = new DefaultButton(resettableParam == null
                ? model : resettableParam);
        if (colorsUsed) {
            defaultButton.setBackground(GRAY);
        }
    }

    public static SliderSpinner simpleFrom(RangeParam model) {
        return new SliderSpinner(model, TextPosition.NONE, false);
    }

    public static SliderSpinner simpleWithDefaultButton(RangeParam model) {
        return new SliderSpinner(model, TextPosition.NONE, true);
    }

    public void setupTicks() {
        int range = model.getMaximum() - model.getMinimum();
        int minorSpacing;
        int majorSpacing;
        if (range == 100) {
            minorSpacing = 0;
            majorSpacing = 25;
//        } else if(range == 499) {
//            minorSpacing = 25;
//            majorSpacing = 100;
        } else if (range >= 7) {
            minorSpacing = (range + 1) / 8;
            majorSpacing = 2 * minorSpacing;
        } else { // dry brush has for example a range of only 5
            minorSpacing = 0;
            majorSpacing = 1;
        }

        setupTicks(majorSpacing, minorSpacing);
    }

    public void setupTicks(int majorSpacing, int minorSpacing) {
        if (majorSpacing > 0) {
            slider.setMajorTickSpacing(majorSpacing);
        }
        if (minorSpacing > 0) {
            slider.setMinorTickSpacing(minorSpacing);
        }

        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
    }


    public void showTicksAsFloat() {
        // TODO throws NullPointerException

        slider.createStandardLabels(10);

        @SuppressWarnings("unchecked")
        Dictionary<Integer, JLabel> labelsDict = slider.getLabelTable();

        Enumeration<Integer> keys = labelsDict.keys();
        while (keys.hasMoreElements()) {
            Integer i = keys.nextElement();
            labelsDict.get(i).setText(String.valueOf(i / 100.0f));
        }
    }

    public int getCurrentValue() {
        return model.getValue();
    }

    public void setValue(int value) {
        model.setValue(value);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        Object o = e.getSource();
        if (o == slider) {
            if (spinnerMoved) {
                return;
            }
            int currentValue = slider.getValue();
            sliderMoved = true;
            spinner.setValue(currentValue);
            sliderMoved = false;
        } else if (o == spinner) {
            if (sliderMoved) {
                return;
            }
            // this gets called even if the slider is modified by the user
            int currentValue = (Integer) spinner.getValue();
            spinnerMoved = true;
            model.setValue(currentValue);
            spinnerMoved = false;
        }

        if (defaultButton != null) {
            defaultButton.updateIcon();
        }
        if (colorsUsed) {
            if (model.isSetToDefault()) {
                defaultButton.setBackground(GRAY);
            } else {
                if (model.getValue() > model.getDefaultValue()) {
                    defaultButton.setBackground(rightColor);
                } else {
                    defaultButton.setBackground(leftColor);
                }
            }
        }
    }

    public void resetToDefaultSettings() {
        model.reset(false);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }


    public RangeParam getModel() {
        return model;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        slider.setEnabled(enabled);
        spinner.setEnabled(enabled);
        if (label != null) {
            label.setEnabled(enabled);
        }
        if (defaultButton != null) {
            defaultButton.setEnabled(enabled && defaultButton.shouldBeEnabled());
        }
    }

    public void setResettable(Resettable param) {
        resettableParam = param;
    }

    @Override
    public void updateGUI() {
        // nothing to do
    }

    public void setSliderName(String name) {
        slider.setName(name);
    }

    @Override
    public void setToolTip(String tip) {
        slider.setToolTipText(tip);
        spinner.setToolTipText(tip);
    }

    public void addChangeListener(ChangeListener listener) {
        slider.addChangeListener(listener);
    }

    // overridden so that AssertJSwing tests find the components easily
    @Override
    public void setName(String name) {
        super.setName(name);
        slider.setName(name);
        spinner.setName(name);
    }
}
