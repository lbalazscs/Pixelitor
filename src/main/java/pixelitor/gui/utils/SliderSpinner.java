/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.ParamGUI;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ResetButton;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.util.Hashtable;

import static java.awt.BorderLayout.*;
import static java.awt.Color.GRAY;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A GUI Component that allows users to select a value within a range.
 * It consists of a JSlider and a JSpinner synchronized with each other,
 * so changing the value in one component will also update the other.
 * It optionally includes a {@link ResetButton} for resetting the value to its default.
 */
public class SliderSpinner extends JPanel implements ChangeListener, ParamGUI {
    private final JLabel label;
    private final TextPosition textPosition;
    private final int orientation;
    private final JPanel spinnerPanel;

    /**
     * The possible positions of the label of a SliderSpinner.
     */
    public enum TextPosition {
        BORDER(true),
        WEST(false),
        NORTH(false),
        NONE(false),
        NONE_TICKS(true);

        private final boolean ticks;

        TextPosition(boolean ticks) {
            this.ticks = ticks;
        }

        boolean addTicks() {
            return ticks;
        }
    }

    public static final int HORIZONTAL = JSlider.HORIZONTAL;
    public static final int VERTICAL = JSlider.VERTICAL;

    private final JSlider slider;
    private final JSpinner spinner;
    private ResetButton resetButton;
    private final RangeParam model;

    private Color leftColor;
    private Color rightColor;
    private final boolean colorsUsed;

    private boolean sliderMoved = false;
    private boolean spinnerMoved = false;

    public SliderSpinner(RangeParam model, TextPosition position, boolean addResetButton) {
        this(model, position, addResetButton, HORIZONTAL);
    }

    public SliderSpinner(RangeParam model, TextPosition position, boolean addResetButton, int orientation) {
        this(model, null, null, position, addResetButton, orientation);
    }

    public SliderSpinner(RangeParam model, Color leftColor, Color rightColor, int orientation) {
        this(model, leftColor, rightColor, TextPosition.BORDER, true, orientation);
    }

    public SliderSpinner(RangeParam model, Color leftColor, Color rightColor) {
        this(model, leftColor, rightColor, TextPosition.BORDER, true, HORIZONTAL);
    }

    private SliderSpinner(RangeParam model,
                          Color leftColor, Color rightColor,
                          TextPosition textPosition,
                          boolean addResetButton,
                          int orientation) {
        super(new BorderLayout());

        this.textPosition = textPosition;
        this.orientation = orientation;
        this.model = model;

        this.leftColor = leftColor;
        this.rightColor = rightColor;
        colorsUsed = leftColor != null;

        if (textPosition == TextPosition.BORDER) {
            if (leftColor != null && rightColor != null) {
                Border gradientBorder = new GradientBorder(leftColor, rightColor);
                setBorder(createTitledBorder(gradientBorder, model.getName()));
            } else {
                setBorder(createTitledBorder(model.getName()));
                this.leftColor = GRAY;
                this.rightColor = GRAY;
            }
        }

        slider = createSlider(model);
        if (textPosition.addTicks()) {
            setupTicks();
        }

        spinner = createSpinner(model);

        if (textPosition == TextPosition.WEST) {
            label = new JLabel(model.getName() + ": ");
            add(label, WEST);
        } else if (textPosition == TextPosition.NORTH) {
            label = new JLabel(model.getName() + ": ");
            add(label, NORTH);
        } else {
            label = null;
        }

        spinnerPanel = new JPanel(new FlowLayout(LEFT));
        add(slider, CENTER);
        spinnerPanel.add(spinner);

        if (addResetButton) {
            createResetButton(model);
            spinnerPanel.add(resetButton);
        }
        add(spinnerPanel, orientation == HORIZONTAL ? EAST : SOUTH);

//        showTicksAsFloat();
    }

    public static SliderSpinner from(RangeParam model) {
        return new SliderSpinner(model, TextPosition.NONE, false);
    }

    private JSlider createSlider(RangeParam model) {
        JSlider s = new JSlider(model);
        s.setOrientation(orientation);
        s.addChangeListener(this);
        return s;
    }

    private JSpinner createSpinner(RangeParam model) {
        assert model.checkInvariants();

        SpinnerNumberModel spinnerModel;
        int decimalPlaces = model.getDecimalPlaces();
        if (decimalPlaces > 0) {
            double stepSize = switch (decimalPlaces) {
                case 1 -> 0.1;
                case 2 -> 0.01;
                default -> throw new IllegalStateException();
            };
            spinnerModel = new SpinnerNumberModel(
                model.getValueAsDouble(), //initial value
                model.getMinimum(), //min
                model.getMaximum(), //max
                stepSize);
        } else {
            spinnerModel = new SpinnerNumberModel(
                model.getValue(), //initial value
                model.getMinimum(), //min
                model.getMaximum(), //max
                1);
        }
        JSpinner s = new JSpinner(spinnerModel);

        if (decimalPlaces > 0) {
            var editor = (JSpinner.NumberEditor) s.getEditor();
            DecimalFormat format = editor.getFormat();
            format.setMinimumFractionDigits(decimalPlaces);
            format.setMaximumFractionDigits(decimalPlaces);

            // make sure that the fractional form is displayed
            editor.getTextField().setValue(model.getValueAsDouble());
        }

        s.addChangeListener(this);
        return s;
    }

    private void createResetButton(RangeParam model) {
        resetButton = new ResetButton(model);
        if (colorsUsed) {
            resetButton.setBackground(GRAY);
        }
    }

    public void addExplicitResetButton(ResetButton resetButton) {
        this.resetButton = resetButton;
        spinnerPanel.add(resetButton);
    }

    public void setupTicks() {
        int max = model.getMaximum();
        int min = model.getMinimum();
        int range = max - min;
        int minorSpacing = 0;
        int majorSpacing = 0;

        if (range == 90) {
            minorSpacing = 0;
            majorSpacing = 15;
        } else if (range == 100) {
            minorSpacing = 0;
            majorSpacing = 25;
        } else if (range == 255) { // Levels, Solarize, etc.
            minorSpacing = 0;
            majorSpacing = 51;
        } else if (min == 1 && max < 702 && range % 100 == 0) {
            // special case for zoom sliders
            setupZoomTicks(max);
            return;
        } else if (range >= 11) {
            if (range % 4 == 0) {
                majorSpacing = range / 4;
                if (range % 8 == 0) {
                    minorSpacing = range / 8;
                }
            } else {
                minorSpacing = (range + 1) / 8;
                majorSpacing = 2 * minorSpacing;
            }
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

    private void setupZoomTicks(int max) {
        Hashtable<Integer, JComponent> labels = slider.createStandardLabels(max + 1);

        // so far the labels table contains only the minimum value
        int percent = 100;
        while (percent <= max) {
            labels.put(percent, new JLabel(String.valueOf(percent)));
            percent = percent + 100;
        }

        slider.setLabelTable(labels);
        slider.setMajorTickSpacing(100);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
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
            spinnerMoved = true;
            if (model.getDecimalPlaces() > 0) {
                double value = (double) spinner.getValue();
                model.setValue(value, true);
            } else {
                int currentValue = (Integer) spinner.getValue();
                model.setValue(currentValue);
            }
            spinnerMoved = false;
        }

        if (resetButton != null) {
            if (colorsUsed) {
                setResetButtonBgColor();
            }
            resetButton.updateIcon();
        }
    }

    private void setResetButtonBgColor() {
        if (model.hasDefault()) {
            resetButton.setBackground(GRAY);
        } else {
            if (model.getValue() > model.getDefaultValue()) {
                resetButton.setBackground(rightColor);
            } else {
                resetButton.setBackground(leftColor);
            }
        }
    }

    public void forceSpinnerValueOnly(int value) {
        boolean oldSliderMoved = sliderMoved;
        sliderMoved = true;
        spinner.setValue(value);
        sliderMoved = oldSliderMoved;
    }

    @Override
    public void updateGUI() {
        // the GUI is already updated, but since this happens through
        // the slider's model, the spinner now has int precision
        if (model.getDecimalPlaces() > 0) {
            double value = model.getValueAsDouble();
            boolean hasFractionalPart = value - (int) value > 0.0001;
            if (hasFractionalPart) {
                sliderMoved = true; // avoid updating the slider
                var editor = (JSpinner.NumberEditor) spinner.getEditor();
                editor.getTextField().setValue(value);
                sliderMoved = false;
            }
        }
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
        if (resetButton != null) {
            resetButton.setEnabled(enabled);
        }
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

    @Override
    public int getNumLayoutColumns() {
        if (textPosition == TextPosition.NONE) {
            return 2;
        }
        return 1;
    }
}
