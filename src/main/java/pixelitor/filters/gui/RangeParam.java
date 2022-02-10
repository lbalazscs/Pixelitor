/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.Drawable;
import pixelitor.utils.Rnd;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.Dimension;
import java.io.Serial;
import java.util.function.BooleanSupplier;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

/**
 * Represents an integer value with a minimum, a maximum and a default.
 * Suitable as the model of a JSlider (but usually used as a model of
 * an entire SliderSpinner)
 */
public class RangeParam extends AbstractFilterParam implements BoundedRangeModel {
    private int minValue;
    private int maxValue;
    private double defaultValue;
    private int decimalPlaces = 0;

    // Not stored as an int in order to enable animation interpolations
    private double value;

    private boolean adjusting;
    private final boolean addResetButton;
    private final SliderSpinner.TextPosition textPosition;

    private ChangeEvent changeEvent = null;
    private final EventListenerList listenerList = new EventListenerList();
    private boolean adjustMaxAccordingToImage = false;
    private double maxToImageSizeRatio;

    private String presetKey;

    public RangeParam(String name, int min, double def, int max) {
        this(name, min, def, max, true, BORDER);
    }

    public RangeParam(String name, int min, double def, int max, boolean addResetButton,
                      SliderSpinner.TextPosition position) {
        this(name, min, def, max, addResetButton, position, ALLOW_RANDOMIZE);
    }

    public RangeParam(String name, int min, double def, int max, boolean addResetButton,
                      SliderSpinner.TextPosition position, RandomizePolicy randomizePolicy) {
        super(name, randomizePolicy);

        assert min < max : name + ": min (" + min + ") >= max (" + max + ')';
        assert def >= min : name + ": def (" + def + ") < min (" + min + ')';
        assert def <= max : name + ": def (" + def + ") > max (" + max + ')';

        minValue = min;
        maxValue = max;
        defaultValue = def;
        value = def;
        this.addResetButton = addResetButton;
        textPosition = position;
    }

    @Override
    public JComponent createGUI() {
        var sliderSpinner = new SliderSpinner(this, textPosition, addResetButton);
        paramGUI = sliderSpinner;
        guiCreated();

        if (action != null) {
            return new ParamGUIWithAction(sliderSpinner, action);
        }

        return sliderSpinner;
    }

    /**
     * Sets up the automatic enabling of another {@link FilterSetting}
     * when the value of this one is not zero.
     * Typically used when this is a randomness slider, and the other
     * is a "reseed randomness" button.
     */
    public void setupEnableOtherIfNotZero(FilterSetting other) {
        other.setEnabled(getValue() != 0, EnabledReason.APP_LOGIC);
        addChangeListener(e ->
            other.setEnabled(getValue() != 0, EnabledReason.APP_LOGIC));
    }

    /**
     * Makes sure that this {@link RangeParam} always has a higher
     * or equal value than the given other {@link RangeParam}
     */
    public void ensureHigherValueThan(RangeParam other) {
        // if the value is not higher, then make it equal
        linkWith(other, () -> other.getValue() > getValue());
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(int dp) {
        decimalPlaces = dp;
    }

    public RangeParam withDecimalPlaces(int dp) {
        setDecimalPlaces(dp);
        return this;
    }

    /**
     * Synchronizes the value of this object with the value of another
     * {@link RangeParam} if the given condition evaluates to true.
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
     * Synchronizes the value of this object with the value of another
     * {@link RangeParam} so that there is a constant multiplier between the values.
     */
    public void scaledLinkWith(RangeParam other, double multiplier) {
        addChangeListener(e -> other.setValueNoTrigger(
            getValueAsDouble() * multiplier));
        other.addChangeListener(e -> setValueNoTrigger(
            other.getValueAsDouble() / multiplier));
    }

    @Override
    public boolean isSetToDefault() {
        return Math.abs(getValueAsDouble() - defaultValue) < 0.005;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    /**
     * Resets to the default value.
     *
     * @param trigger should be true if called from a GUI component
     */
    @Override
    public void reset(boolean trigger) {
        setValue(defaultValue, trigger);
    }

    /**
     * Returns the value of a percentage parameter as a float ratio
     */
    public float getPercentageValF() {
        return getValueAsFloat() / 100.0f;
    }

    /**
     * Returns the value of a percentage parameter as a double ratio
     */
    public double getPercentageValD() {
        return getValueAsDouble() / 100.0;
    }

    /**
     * Int values measured in degrees are transformed to radians
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

    @Override
    public int getValue() {
        return (int) value;
    }

    public boolean isZero() {
        return getValue() == 0;
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
        if (paramGUI != null) {
            // while loading a smart filter, it can happen that
            // the value is out of range (if the range is adjusted
            // to the canvas size, but this adjustment didn't happen yet)
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

    @Override
    public void addChangeListener(ChangeListener x) {
        assert x != null;
        listenerList.add(ChangeListener.class, x);
    }

    @Override
    public void removeChangeListener(ChangeListener x) {
        listenerList.remove(ChangeListener.class, x);
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

    @Override
    public void updateOptions(Drawable dr, boolean changeValue) {
        if (adjustMaxAccordingToImage) {
            Dimension size = dr.getComp().getCanvas().getSize();
            double defaultToMaxRatio = defaultValue / maxValue;
            maxValue = (int) (maxToImageSizeRatio * Math.max(size.width, size.height));
            if (maxValue <= minValue) { // can happen with very small images
                maxValue = minValue + 1;
            }

            // make sure that the tic/label for max value is painted, see issue #91
            maxValue += (4 - (maxValue - minValue) % 4);

            defaultValue = (int) (defaultToMaxRatio * maxValue);
            if (defaultValue > maxValue) {
                defaultValue = maxValue;
            }
            if (defaultValue < minValue) {
                defaultValue = minValue;
            }
            if (changeValue) {
                value = defaultValue;
            }
        }
    }

    public RangeParam withAdjustedRange(double ratio) {
        maxToImageSizeRatio = ratio;
        adjustMaxAccordingToImage = true;
        return this;
    }

    @Override
    public boolean canBeAnimated() {
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
        String defaultAsString = formatAsString(defaultValue, decimalPlaces);
        return super.getResetToolTip() + " to " + defaultAsString;
    }

    private static String formatAsString(double d, int decimalPlaces) {
        return switch (decimalPlaces) {
            case 0 -> String.valueOf((int) d);
            case 1 -> format("%.1f", d);
            case 2 -> format("%.2f", d);
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public Object getParamValue() {
        return value;
    }

    @Override
    public String getPresetKey() {
        if (presetKey != null) {
            return presetKey;
        }
        return getName();
    }

    public void setPresetKey(String presetKey) {
        this.presetKey = presetKey;
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
        private SliderSpinner.TextPosition textPosition = BORDER;
        private RandomizePolicy randomizePolicy = ALLOW_RANDOMIZE;

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

        public Builder textPosition(SliderSpinner.TextPosition textPosition) {
            this.textPosition = textPosition;
            return this;
        }

        public Builder randomizePolicy(RandomizePolicy randomizePolicy) {
            this.randomizePolicy = randomizePolicy;
            return this;
        }

        public RangeParam build() {
            RangeParam rp = new RangeParam(name, min, def, max,
                addResetButton, textPosition, randomizePolicy);
            rp.setDecimalPlaces(decimalPlaces);
            return rp;
        }
    }

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
            return formatAsString(value, decimalPlaces);
        }

        @Override
        public String toString() {
            return format("%s[value=%.2f]",
                getClass().getSimpleName(), value);
        }
    }
}
