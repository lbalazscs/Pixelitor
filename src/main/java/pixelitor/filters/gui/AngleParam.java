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

import com.jhlabs.image.ImageMath;
import pixelitor.utils.Utils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.Rectangle;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A filter parameter for selecting an angle.
 */
public class AngleParam extends AbstractFilterParam {
    // as returned form Math.atan2, this is between -PI and PI
    private double angle;
    private double defaultVal = 0.0;

    private ChangeEvent changeEvent = null;
    private final EventListenerList listenerList = new EventListenerList();

    public AngleParam(String name, double defaultValue) {
        super(name, ALLOW_RANDOMIZE);

        setValue(defaultValue, false);

        defaultVal = defaultValue;
    }

    @Override
    public JComponent createGUI() {
        paramGUI = new AngleParamGUI(this);
        setParamGUIEnabledState();
        return (JComponent) paramGUI;
    }

    public void setValueInDegrees(double d, boolean trigger) {
        double degrees = d;
        if (degrees < 0) {
            degrees = -degrees;
        } else {
            degrees = 360 - degrees;
        }
        double r = Math.toRadians(degrees);
        setValue(r, trigger);
    }

    public void setValue(double r, boolean trigger) {
        if (angle != r) {
            angle = r;
            fireStateChanged();
        }
        if (trigger) {
            // trigger even if this angle was already set,
            // because we had drag events, and now we have mouse up
            if (adjustmentListener != null) { // can be null when used in tools
                adjustmentListener.paramAdjusted();
            }
        }
    }

    @SuppressWarnings("unused")
    public int getValueInNonIntuitiveDegrees() {
        return (int) Math.toDegrees(angle);
    }

    public int getValueInDegrees() {
        int degrees = (int) Math.toDegrees(angle);
        if (degrees <= 0) {
            degrees = -degrees;
        } else {
            degrees = 360 - degrees;
        }
        return degrees;
    }

    /**
     * Returns the "Math.atan2" radians: the value between -PI and PI
     */
    public double getValueInRadians() {
        return angle;
    }

    /**
     * Returns the value in the range of 0 and 2*PI, and in the intuitive direction
     */
    public double getValueInIntuitiveRadians() {
        return Utils.atan2AngleToIntuitive(angle);
    }

    @Override
    public boolean isSetToDefault() {
        return angle == defaultVal;
    }

    @Override
    public int getNumGridBagCols() {
        return 1;
    }

    private void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    public void addChangeListener(ChangeListener x) {
        listenerList.add(ChangeListener.class, x);
    }

    @SuppressWarnings("unused")
    public void removeChangeListener(ChangeListener x) {
        listenerList.remove(ChangeListener.class, x);
    }

    @Override
    public void reset(boolean trigger) {
        setValue(defaultVal, trigger);
    }

    @Override
    public void randomize() {
        double random = Math.random();
        setValue(random * 2 * Math.PI - Math.PI, false);
    }

    public AbstractAngleUI getAngleSelectorUI() {
        return new AngleUI(this);
    }

    public int getMaxAngleInDegrees() {
        return 360;
    }

    public RangeParam createRangeParam() {
        return new RangeParam(getName(), 0, getValueInDegrees(), getMaxAngleInDegrees());
    }

    @Override
    public void considerImageSize(Rectangle bounds) {
    }

    @Override
    public boolean canBeAnimated() {
        return true;
    }

    @Override
    public ParamState copyState() {
        // save the degrees so that the interpolation
        // does not confuse the user
        return new APState(getValueInDegrees());
    }

    @Override
    public void setState(ParamState state) {
        setValueInDegrees(((APState) state).angle, false);
    }

    private static class APState implements ParamState {
        private final double angle;

        public APState(double angle) {
            this.angle = angle;
        }

        @Override
        public ParamState interpolate(ParamState endState, double progress) {
            APState apEndState = (APState) endState;
            double interpolatedAngle = ImageMath.lerp(progress, angle, apEndState.angle);
            return new APState(interpolatedAngle);
        }
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', angle = %.2f]",
                getClass().getSimpleName(), getName(), angle);
    }
}