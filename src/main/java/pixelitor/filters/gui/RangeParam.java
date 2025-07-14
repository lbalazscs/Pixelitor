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

package pixelitor.filters.gui;

import com.jhlabs.image.ImageMath;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.layers.Filterable;
import pixelitor.utils.Rnd;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.Dimension;
import java.io.Serial;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizeMode.ALLOW_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.BORDER;

/**
 * A numeric parameter with minimum, maximum, and default values.
 * Primarily used as the model for a {@link SliderSpinner}, but it
 * can also be used as a model of a simple JSlider.
 */
public class RangeParam extends AbstractFilterParam implements BoundedRangeModel {
    private int minValue;
    private int maxValue;
    private double defaultValue;
    private int decimalPlaces = 0;

    // Stored as double to support animation interpolation
    private double value;

    private boolean adjusting;
    private final boolean addResetButton;
    private final SliderSpinner.LabelPosition labelPosition;

    private ChangeEvent changeEvent = null;
    private final EventListenerList listenerList = new EventListenerList();

    // Image-size dependent range adjustment
    private boolean adjustRangeToCanvasSize = false;
    private double maxToCanvasSizeRatio;

    public RangeParam(String name, int min, double def, int max) {
        this(name, min, def, max, true, BORDER);
    }

    public RangeParam(String name, int min, double def, int max, boolean addResetButton,
                      SliderSpinner.LabelPosition position) {
        this(name, min, def, max, addResetButton, position, ALLOW_RANDOMIZE);
    }

    public RangeParam(String name, int min, double def, int max, boolean addResetButton,
                      SliderSpinner.LabelPosition position, RandomizeMode randomizeMode) {
        super(name, randomizeMode);

        minValue = min;
        maxValue = max;
        defaultValue = def;
        value = def;
        assert checkInvariants();

        this.addResetButton = addResetButton;
        labelPosition = position;
    }

    @Override
    public JComponent createGUI() {
        var sliderSpinner = new SliderSpinner(this, labelPosition, addResetButton);
        paramGUI = sliderSpinner;
        guiCreated();

        return action == null
            ? sliderSpinner
            : new ParamGUIWithAction(sliderSpinner, action);
    }

    /**
     * Sets up the automatic enabling of another {@link FilterSetting}
     * when this parameter's value is non-zero.
     * Typically used when this is a randomness slider, and the other
     * is a "reseed randomness" button.
     */
    public void setupEnableOtherIfNotZero(FilterSetting other) {
        other.setEnabled(getValue() != 0);
        addChangeListener(e ->
            other.setEnabled(getValue() != 0));
    }

    /**
     * Sets up the automatic disabling of another {@link FilterSetting}
     * when this parameter's value matches a condition.
     */
    public void setupDisableOtherIf(FilterSetting other, IntPredicate condition) {
        other.setEnabled(true);
        addChangeListener(e ->
            other.setEnabled(!condition.test(getValue())));
    }

    /**
     * Ensures that this {@link RangeParam}'s value stays
     * greater than or equal to another {@link RangeParam}'s value.
     */
    public void ensureHigherValueThan(RangeParam other) {
        // if the value is not higher, then make it equal
        linkWith(other, () -> other.getValue() > getValue());
    }

    /**
     * Links this parameter's value with another {@link RangeParam}
     * based on a condition. When the condition is true,
     * the parameters will synchronize their values.
     */
    public void linkWith(RangeParam other, BooleanSupplier condition) {
        addChangeListener(e -> {
            if (condition.getAsBoolean()) {
                other.setValueNoTrigger(getValueAsDouble());
            }
        });
        other.addChangeListener(e -> {
            if (condition.getAsBoolean()) {
                setValueNoTrigger(other.getValueAsDouble());
            }
        });
    }

    /**
     * Links this parameter's value with another {@link RangeParam}
     * using a constant multiplier. Changes to either parameter will
     * update the other maintaining the multiplier relationship.
     */
    public void scaledLinkWith(RangeParam other, double multiplier) {
        addChangeListener(e -> other.setValueNoTrigger(
            getValueAsDouble() * multiplier));
        other.addChangeListener(e -> setValueNoTrigger(
            other.getValueAsDouble() / multiplier));
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(int dp) {
        assert dp >= 0 && dp <= 2 : "dp = " + dp;
        decimalPlaces = dp;
    }

    public RangeParam withDecimalPlaces(int dp) {
        setDecimalPlaces(dp);
        return this;
    }

    @Override
    public boolean hasDefault() {
        return Math.abs(getValueAsDouble() - defaultValue) < 0.005;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void reset(boolean trigger) {
        setValue(defaultValue, trigger);
    }

    /**
     * Returns the value as a percentage.
     */
    public double getPercentage() {
        return getValueAsDouble() / 100.0;
    }

    public String getPercentageStr() {
        return getPercentage() + "";
    }

    /**
     * Returns the value in radians assuming that this {@link RangeParam} represents an angle in degrees.
     */
    public float getValueInRadians() {
        return (float) Math.toRadians(getValueAsDouble());
    }

    @Override
    protected void doRandomize() {
        int range = maxValue - minValue;
        int newValue = minValue + Rnd.nextInt(range);

        setValueNoTrigger(newValue);
    }

    public void increaseValue() {
        int intValue = (int) value;
        if (intValue < maxValue) {
            setValue(intValue + 1);
        }
    }

    public void decreaseValue() {
        int intValue = (int) value;
        if (intValue > minValue) {
            setValue(intValue - 1);
        }
    }

    @Override
    public int getMinimum() {
        return minValue;
    }

    @Override
    public void setMinimum(int newMinimum) {
        minValue = newMinimum;
    }

    @Override
    public int getMaximum() {
        return maxValue;
    }

    @Override
    public void setMaximum(int newMaximum) {
        maxValue = newMaximum;
    }

    public boolean isMaximum() {
        return getValue() == maxValue;
    }

    public boolean isZero() {
        return getValue() == 0;
    }

    @Override
    public int getValue() {
        return (int) value;
    }

    public String getValueStr() {
        return getValue() + "";
    }

    public float getValueAsFloat() {
        return (float) value;
    }

    public double getValueAsDouble() {
        return value;
    }

    // accepts an int so that the class can implement BoundedRangeModel
    @Override
    public void setValue(int n) {
        setValue(n, true);
    }

    public void setValueNoTrigger(double n) {
        setValue(n, false);
    }

    public void setValue(double v, boolean trigger) {
        if (paramGUI != null || trigger) {
            // While loading a smart filter, it can happen that
            // the value is out of range (if the range is adjusted
            // to the canvas size, but this adjustment didn't happen yet).
            // The trigger condition is important, because when used outside
            // the filters paramGUI could be null even if a GUI was created.
            if (v > maxValue) {
                v = maxValue;
            }
            if (v < minValue) {
                v = minValue;
            }
        }

        if (Math.abs(v - value) > 0.001) { // there are max 2 decimal places in the GUI
            value = v;
            fireStateChanged(); // update the GUI, because this is the model of the slider
            if (paramGUI != null) {
                // make sure fractional values are also updated in the spinner
                paramGUI.updateGUI();
            }

            if (!adjusting && trigger && adjustmentListener != null) {
                adjustmentListener.paramAdjusted(); // run the filter
            }
        }
    }

    /**
     * This is only used programmatically while tweening, therefore
     * it never triggers the filter or the GUI
     */
    public void setValueNoGUI(double d) {
        value = d;
    }

    @Override
    public void setValueIsAdjusting(boolean b) {
        if (!b && adjusting && adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
        if (adjusting != b) {
            adjusting = b;
            fireStateChanged();
        }
    }

    @Override
    public boolean getValueIsAdjusting() {
        return adjusting;
    }

    @Override
    public int getExtent() {
        return 0;
    }

    @Override
    public void setExtent(int newExtent) {
        // not used
    }

    @Override
    public void setRangeProperties(int value, int extent, int min, int max, boolean adjusting) {
        // not used
    }

    // using a ChangeListener instead of the ParamAdjustmentListener
    // results in continuous updates while the slider is dragged
    @Override
    public void addChangeListener(ChangeListener x) {
        assert x != null;
        listenerList.add(ChangeListener.class, x);
    }

    @Override
    public void removeChangeListener(ChangeListener x) {
        listenerList.remove(ChangeListener.class, x);
    }

    public void removeAllChangeListeners() {
        for (ChangeListener listener : listenerList.getListeners(ChangeListener.class)) {
            listenerList.remove(ChangeListener.class, listener);
        }
    }

    private void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ChangeListener listener = (ChangeListener) listeners[i + 1];
                listener.stateChanged(changeEvent);
            }
        }
    }

    /**
     * Updates the parameter's range based on the size
     * of the canvas if range adjustment is enabled.
     */
    @Override
    public void adaptToContext(Filterable layer, boolean changeValue) {
        if (!adjustRangeToCanvasSize) {
            return;
        }
        Dimension size = layer.getComp().getCanvas().getSize();
        double defaultToMaxRatio = defaultValue / maxValue;

        // The maximum value is calculated proportionally to the
        // largest dimension of the canvas while preserving
        // the ratio between default and maximum values.
        maxValue = (int) (maxToCanvasSizeRatio * Math.max(size.width, size.height));
        if (maxValue <= minValue) { // can happen with very small images
            maxValue = minValue + 1;
        }

        // make sure that the tic/label for max value is painted, see issue #91
        maxValue += (4 - (maxValue - minValue) % 4);

        setDefaultValue((int) (defaultToMaxRatio * maxValue));
        if (changeValue) {
            value = defaultValue;
        }
    }

    public RangeParam withAdjustedRange(double ratio) {
        maxToCanvasSizeRatio = ratio;
        adjustRangeToCanvasSize = true;
        return this;
    }

    public void setDefaultValue(double newDefault) {
        defaultValue = Math.clamp(newDefault, minValue, maxValue);
    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    @Override
    public RangeParamState copyState() {
        return new RangeParamState(value, decimalPlaces);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        double newValue = ((RangeParamState) state).getValue();
        if (updateGUI) {
            setValueNoTrigger(newValue);
        } else {
            value = newValue;
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {
        try {
            double v = Double.parseDouble(savedValue);
            setValueNoTrigger(v);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Could not parse " + savedValue);
        }
    }

    @Override
    public String getResetToolTip() {
        return super.getResetToolTip() + " to " + formatDecimal(defaultValue, decimalPlaces);
    }

    private static String formatDecimal(double d, int decimalPlaces) {
        return switch (decimalPlaces) {
            case 0 -> String.valueOf((int) d);
            case 1 -> format("%.1f", d);
            case 2 -> format("%.2f", d);
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public String getParamValue() {
        return String.valueOf(value);
    }

    public boolean checkInvariants() {
        if (maxValue <= minValue) {
            throw new AssertionError("%s: maxValue (%d) <= minValue (%d)".formatted(getName(), maxValue, minValue));
        }
        if (value < minValue) {
            throw new AssertionError("%s: value (%.2f) < minValue (%d)".formatted(getName(), value, minValue));
        }
        if (value > maxValue) {
            throw new AssertionError("%s: value (%.2f) > maxValue (%d)".formatted(getName(), value, maxValue));
        }
        return true;
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', value = %.2f]",
            getClass().getSimpleName(), getName(), value);
    }

    public static class Builder {
        private final String name;
        private int min;
        private double def;
        private int max;
        private boolean addResetButton = true;
        private int decimalPlaces = 0;
        private SliderSpinner.LabelPosition labelPosition = BORDER;
        private RandomizeMode randomizeMode = ALLOW_RANDOMIZE;

        public Builder(String name) {
            this.name = name;
        }

        public Builder min(int min) {
            this.min = min;
            return this;
        }

        public Builder def(double def) {
            this.def = def;
            return this;
        }

        public Builder max(int max) {
            this.max = max;
            return this;
        }

        public Builder withDecimalPlaces(int dp) {
            decimalPlaces = dp;
            return this;
        }

        public Builder addResetButton(boolean addResetButton) {
            this.addResetButton = addResetButton;
            return this;
        }

        public Builder textPosition(SliderSpinner.LabelPosition labelPosition) {
            this.labelPosition = labelPosition;
            return this;
        }

        public Builder randomizePolicy(RandomizeMode randomizeMode) {
            this.randomizeMode = randomizeMode;
            return this;
        }

        public RangeParam build() {
            RangeParam rp = new RangeParam(name, min, def, max,
                addResetButton, labelPosition, randomizeMode);
            rp.setDecimalPlaces(decimalPlaces);
            return rp;
        }
    }

    /**
     * The state of a {@link RangeParam} (with double precision) that
     * can be saved, restored, and interpolated.
     */
    public static class RangeParamState implements ParamState<RangeParamState> {
        @Serial
        private static final long serialVersionUID = 1L;

        final double value;
        final int decimalPlaces;

        public RangeParamState(double value) {
            this(value, 0);
        }

        public RangeParamState(double value, int decimalPlaces) {
            this.value = value;
            this.decimalPlaces = decimalPlaces;
        }

        @Override
        public RangeParamState interpolate(RangeParamState endState, double progress) {
            double interpolated = ImageMath.lerp(progress, value, endState.value);
            return new RangeParamState(interpolated, decimalPlaces);
        }

        public double getValue() {
            return value;
        }

        @Override
        public String toSaveString() {
            return formatDecimal(value, decimalPlaces);
        }

        @Override
        public String toString() {
            return format("%s[value=%.2f]",
                getClass().getSimpleName(), value);
        }
    }
}
