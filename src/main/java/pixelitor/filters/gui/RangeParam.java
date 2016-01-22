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

import com.jhlabs.image.ImageMath;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.Rectangle;
import java.util.Random;

import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

/**
 * Represents an integer value with a minimum, a maximum and a default.
 * Suitable as the model of a JSlider (but usually used as a model of
 * an entire SliderSpinner)
 */
public class RangeParam extends AbstractFilterParam implements BoundedRangeModel, RangeBasedOnImageSize {
    private int minValue;
    private int maxValue;
    private int defaultValue;

    /**
     * This is not stored as an int in order to enable animation interpolations
     * However setValue accepts only int as the argument
     * because it implements BoundedRangeModel
     * There is also a setValueAsDouble method, but this is only used programmatically
     */
    private double value;

    private boolean adjusting;
    private final AddDefaultButton addDefaultButton;
    private final SliderSpinner.TextPosition textPosition;

    private ChangeEvent changeEvent = null;
    private final EventListenerList listenerList = new EventListenerList();
    private boolean adjustMaxAccordingToImage = false;
    private double maxToImageSizeRatio;

    public RangeParam(String name, int minValue, int defaultValue, int maxValue) {
        this(name, minValue, defaultValue, maxValue, AddDefaultButton.YES, BORDER);
    }

    public RangeParam(String name, int minValue, int defaultValue, int maxValue, AddDefaultButton addDefaultButton, SliderSpinner.TextPosition position) {
        this(name, minValue, defaultValue, maxValue, addDefaultButton, position, ALLOW_RANDOMIZE);
    }

    public RangeParam(String name, int minValue, int defaultValue, int maxValue, AddDefaultButton addDefaultButton, SliderSpinner.TextPosition position, RandomizePolicy randomizePolicy) {
        super(name, randomizePolicy);

        assert minValue < maxValue : name + ": minValue (" + minValue + ") >= maxValue (" + maxValue + ')';
        assert defaultValue >= minValue : name + ": defaultValue (" + defaultValue + ") < minValue (" + minValue + ')';
        assert defaultValue <= maxValue : name + ": defaultValue (" + defaultValue + ") > maxValue (" + maxValue + ')';

        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.addDefaultButton = addDefaultButton;
        this.textPosition = position;
    }

    @Override
    public JComponent createGUI() {
        SliderSpinner sliderSpinner = new SliderSpinner(this, textPosition, addDefaultButton);
        paramGUI = sliderSpinner;
        setParamGUIEnabledState();
        return sliderSpinner;
    }

    @Override
    public boolean isSetToDefault() {
        return (getValue() == defaultValue);
    }

    public int getDefaultValue() {
        return defaultValue;
    }

    /**
     * Resets to the default value.
     * @param triggerAction should be true if called from a GUI component
     */
    @Override
    public void reset(boolean triggerAction) {
        setValue(defaultValue, triggerAction);
    }

    /**
     * This class can be used to manage non-integer values by multiplying them with 100
     */
    public float getValueAsPercentage() {
        return (getValueAsFloat()) / 100.0f;
    }

    /**
     * Int values measured in grades are transformed to radians
     */
    public float getValueInRadians() {
        return (float) Math.toRadians(getValueAsDouble());
    }

    @Override
    public int getNrOfGridBagCols() {
        if (textPosition == NONE) {
            return 2;
        }
        return 1;
    }

    @Override
    public void randomize() {
        if (randomizePolicy.allowRandomize()) {
            int range = maxValue - minValue;
            Random rnd = new Random();
            int newValue = minValue + rnd.nextInt(range);

            setValueNoTrigger(newValue);
        }
    }

    public void increaseValue() {
        if (value < maxValue) {
            setValue((int) value + 1);
        }
    }

    public void decreaseValue() {
        if (value > minValue) {
            setValue((int) value - 1);
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

    public float getValueAsFloat() {
        return (float) value;
    }

    public double getValueAsDouble() {
        return value;
    }

    @Override
    public void setValue(int n) {
        setValue(n, true);
    }

    public void setValueNoTrigger(int n) {
        setValue(n, false);
    }

    public void setValue(int n, boolean trigger) {
// these assertions cannot be made because of swing bugs
//        assert n <= maxValue : getName() + ": n (" + n + ") > maxValue (" + maxValue + ')';
//        assert n >= minValue : getName() + ": n (" + n + ") < minValue (" + minValue + ')';

        if (n > maxValue) {
            n = maxValue;
        }
        if (n < minValue) {
            n = minValue;
        }

        if (value != n) {
            value = n;
            fireStateChanged();
            if (!adjusting) {
                if (trigger) {
                    if (adjustmentListener != null) {
                        adjustmentListener.paramAdjusted();
                    }
                }
            }
        }
    }

    /**
     * This is only used programmatically while tweening, therefore
     * it never triggers the filter or the GUI
     */
    public void setValueAsDouble(double d) {
        value = d;
    }

    @Override
    public void setValueIsAdjusting(boolean b) {
        if (!b) {
            if (adjusting) {
                if (adjustmentListener != null) {
                    adjustmentListener.paramAdjusted();
                }
            }
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
    public void considerImageSize(Rectangle bounds) {
        if(adjustMaxAccordingToImage) {
            double defaultToMaxRatio = ((double) defaultValue) / ((double) maxValue);
            maxValue = (int) (maxToImageSizeRatio * Math.max(bounds.width, bounds.height));
            if(maxValue <= minValue) { // can happen with very small (for example 1x1) images
                maxValue = minValue + 1;
            }
            defaultValue = (int) (defaultToMaxRatio * maxValue);
            if(defaultValue > maxValue) {
                defaultValue = maxValue;
            }
            if(defaultValue < minValue) {
                defaultValue = minValue;
            }
            value = defaultValue;
        }
    }

    @Override
    public RangeParam adjustRangeToImageSize(double ratio) {
        maxToImageSizeRatio = ratio;
        this.adjustMaxAccordingToImage = true;
        return this;
    }

    @Override
    public boolean canBeAnimated() {
        return true;
    }

    @Override
    public ParamState copyState() {
        return new RPState(value);
    }

    @Override
    public void setState(ParamState state) {
        double doubleValue = ((RPState) state).getValue();
        value = doubleValue;
    }

    public RangeParam setRandomizePolicy(RandomizePolicy randomizePolicy) {
        this.randomizePolicy = randomizePolicy;
        return this;
    }

    private static class RPState implements ParamState {
        final double value;

        public RPState(double value) {
            this.value = value;
        }

        @Override
        public RPState interpolate(ParamState endState, double progress) {
            RPState rpEndState = (RPState) endState;
            double interpolated = ImageMath.lerp(progress, value, rpEndState.value);
            return new RPState(interpolated);
        }

        public double getValue() {
            return value;
        }
    }

    @Override
    public String toString() {
        return String.format("%s[name = '%s', value = %.2f]",
                getClass().getSimpleName(), getName(), value);
    }
}
