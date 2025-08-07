/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.RangeWithColorsParam;
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

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.EAST;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.BorderLayout.WEST;
import static java.awt.Color.GRAY;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A GUI Component for selecting a value within a range.
 * It consists of a JSlider and a JSpinner synchronized with each other,
 * allowing users to select values either by sliding or direct numeric
 * input.
 * It optionally includes a {@link ResetButton} for restoring the default value.
 */
public class SliderSpinner extends JPanel implements ChangeListener, ParamGUI {
    private JLabel label;
    private final LabelPosition labelPosition;
    private final JPanel controlPanel;

    /**
     * The possible positions of the label of a SliderSpinner.
     */
    public enum LabelPosition {
        BORDER(true),
        WEST(false),
        NORTH(false),
        NONE(false),
        NONE_WITH_TICKS(true);

        private final boolean showTicks;

        LabelPosition(boolean showTicks) {
            this.showTicks = showTicks;
        }

        boolean shouldShowTicks() {
            return showTicks;
        }
    }

    public static final int HORIZONTAL = JSlider.HORIZONTAL;
    public static final int VERTICAL = JSlider.VERTICAL;
    private final int orientation;

    private final JSlider slider;
    private final JSpinner spinner;
    private ResetButton resetButton;
    private final RangeParam model;

    private Color startColor;
    private Color endColor;
    private final boolean hasGradient;

    private boolean isSliderAdjusting = false;
    private boolean isSpinnerAdjusting = false;

    public SliderSpinner(RangeParam model, LabelPosition position, boolean addResetButton) {
        this(model, position, addResetButton, HORIZONTAL);
    }

    public SliderSpinner(RangeParam model, LabelPosition position, boolean addResetButton, int orientation) {
        this(model, null, null, position, addResetButton, orientation);
    }

    public SliderSpinner(RangeParam model, Color startColor, Color endColor, int orientation) {
        this(model, startColor, endColor, LabelPosition.BORDER, true, orientation);
    }

    public SliderSpinner(RangeParam model, Color startColor, Color endColor) {
        this(model, startColor, endColor, LabelPosition.BORDER, true, HORIZONTAL);
    }

    private SliderSpinner(RangeParam model,
                          Color startColor, Color endColor,
                          LabelPosition labelPosition,
                          boolean addResetButton,
                          int orientation) {
        super(new BorderLayout());

        this.model = model;
        this.labelPosition = labelPosition;
        this.orientation = orientation;

        this.startColor = startColor;
        this.endColor = endColor;
        assert (startColor == null) == (endColor == null);
        hasGradient = startColor != null;

        if (labelPosition == LabelPosition.BORDER) {
            if (hasGradient) {
                Border gradientBorder = new GradientBorder(startColor, endColor);
                setBorder(createTitledBorder(gradientBorder, model.getName()));
            } else {
                setBorder(createTitledBorder(model.getName()));
                this.startColor = GRAY;
                this.endColor = GRAY;
            }
        }

        if (labelPosition == LabelPosition.WEST) {
            label = new JLabel(model.getName() + ": ");
            add(label, WEST);
        } else if (labelPosition == LabelPosition.NORTH) {
            label = new JLabel(model.getName() + ": ");
            add(label, NORTH);
        } else {
            label = null;
        }

        slider = createSlider(model);
        if (labelPosition.shouldShowTicks()) {
            setupTicks();
        }
        add(slider, CENTER);

        spinner = createSpinner(model);
        controlPanel = new JPanel(new FlowLayout(LEFT));
        controlPanel.add(spinner);

        if (addResetButton) {
            createResetButton(model);
            controlPanel.add(resetButton);
        }
        add(controlPanel, orientation == HORIZONTAL
            ? EAST
            : SOUTH);

//        showTicksAsFloat();
    }

    public static SliderSpinner from(RangeParam model) {
        return new SliderSpinner(model, LabelPosition.NONE, false);
    }

    private JSlider createSlider(RangeParam model) {
        JSlider s = new JSlider(model);
        s.setOrientation(orientation);
        s.addChangeListener(this);
        return s;
    }

    private JSpinner createSpinner(RangeParam model) {
        assert model.checkInvariants();

        int decimalPlaces = model.getDecimalPlaces();
        JSpinner s = new JSpinner(createSpinnerModel(model, decimalPlaces));

        if (decimalPlaces > 0) {
            configureDecimalFormat(model, s, decimalPlaces);
        }

        s.addChangeListener(this);
        return s;
    }

    private static SpinnerNumberModel createSpinnerModel(RangeParam model, int decimalPlaces) {
        SpinnerNumberModel spinnerModel;
        if (decimalPlaces > 0) {
            //double stepSize = Math.pow(10, -decimalPlaces);
            double stepSize = switch (decimalPlaces) {
                case 1 -> 0.1;
                case 2 -> 0.01;
                default -> throw new IllegalStateException();
            };
            spinnerModel = new SpinnerNumberModel( // constructor using doubles
                model.getValueAsDouble(),
                model.getMinimum(),
                model.getMaximum(),
                stepSize);
        } else {
            spinnerModel = new SpinnerNumberModel( // constructor using ints
                model.getValue(),
                model.getMinimum(),
                model.getMaximum(),
                1);
        }
        return spinnerModel;
    }

    private static void configureDecimalFormat(RangeParam model, JSpinner s, int decimalPlaces) {
        var editor = (JSpinner.NumberEditor) s.getEditor();
        DecimalFormat format = editor.getFormat();
        format.setMinimumFractionDigits(decimalPlaces);
        format.setMaximumFractionDigits(decimalPlaces);

        // make sure that the fractional form is displayed
        editor.getTextField().setValue(model.getValueAsDouble());
    }

    private void createResetButton(RangeParam model) {
        resetButton = new ResetButton(model);
        if (hasGradient) {
            resetButton.setBackground(GRAY);
        }
    }

    public void addExplicitResetButton(ResetButton resetButton) {
        this.resetButton = resetButton;
        controlPanel.add(resetButton);
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

    public int getValue() {
        return model.getValue();
    }

    public void setValue(int value) {
        model.setValue(value);
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        Object eventSource = event.getSource();
        if (eventSource == slider) {
            if (isSpinnerAdjusting) {
                // the change originally came from the spinner
                return;
            }
            // synchronize the spinner with the slider value
            isSliderAdjusting = true;
            spinner.setValue(slider.getValue());
            isSliderAdjusting = false;
        } else if (eventSource == spinner) {
            if (isSliderAdjusting) {
                // the change originally came from the slider
                return;
            }
            // synchronize the slider with the spinner value
            isSpinnerAdjusting = true;
            if (model.getDecimalPlaces() > 0) {
                // the spinner returns a Double
                model.setValue((Double) spinner.getValue(), true);
            } else {
                // the spinner returns an Integer
                model.setValue((Integer) spinner.getValue());
            }
            isSpinnerAdjusting = false;
        }

        updateResetButton();
    }

    private void updateResetButton() {
        if (resetButton != null) {
            if (hasGradient) {
                updateResetButtonColor();
            }
            resetButton.updateState();
        }
    }

    private void updateResetButtonColor() {
        if (model.isAtDefault()) {
            resetButton.setBackground(GRAY);
        } else {
            if (model.getValue() > model.getDefaultValue()) {
                resetButton.setBackground(endColor);
            } else {
                resetButton.setBackground(startColor);
            }
        }
    }

    public void forceSpinnerValueOnly(int value) {
        boolean oldSliderMoved = isSliderAdjusting;
        isSliderAdjusting = true;
        spinner.setValue(value);
        isSliderAdjusting = oldSliderMoved;
    }

    @Override
    public void updateGUI() {
        // the GUI is already updated, but since this happens through
        // the slider's model, the spinner now has int precision
        if (model.getDecimalPlaces() > 0) {
            double value = model.getValueAsDouble();
            boolean hasFractionalPart = value - (int) value > 0.0001;
            if (hasFractionalPart) {
                isSliderAdjusting = true; // avoid updating the slider
                var editor = (JSpinner.NumberEditor) spinner.getEditor();
                editor.getTextField().setValue(value);
                isSliderAdjusting = false;
            }
        }
    }

    /**
     * Updates the visual appearance of the component based on its model's current state.
     * This includes the title, colors, and gradients.
     */
    public void updateAppearance(boolean revalidate) {
        if (labelPosition == LabelPosition.BORDER) {
            if (model instanceof RangeWithColorsParam rwcParam) {
                this.startColor = rwcParam.getLeftColor();
                this.endColor = rwcParam.getRightColor();
            }

            if (hasGradient) {
                Border gradientBorder = new GradientBorder(startColor, endColor);
                setBorder(createTitledBorder(gradientBorder, model.getName()));
            } else {
                setBorder(createTitledBorder(model.getName()));
            }
        } else if (label != null) {
            label.setText(model.getName() + ": ");
        }

        updateResetButton();

        if (revalidate) {
            revalidate();
            repaint();
        }
    }

    // sets an independently created label reference
    public void setLabel(JLabel label) {
        this.label = label;
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
        if (labelPosition == LabelPosition.NONE) {
            return 2;
        }
        return 1;
    }
}
