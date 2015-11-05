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

import com.jhlabs.image.ImageMath;
import pixelitor.utils.Utils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.Rectangle;

import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A filter parameter for selecting an angle
 */
public class AngleParam extends AbstractFilterParam {
    private double angleInRadians; // as returned form Math.atan2, this is between -PI and PI
    private double defaultInRadians = 0.0;

    private ChangeEvent changeEvent = null;
    private final EventListenerList listenerList = new EventListenerList();

    public AngleParam(String name, double defaultValue) {
        super(name, ALLOW_RANDOMIZE);

        setValueInRadians(defaultValue, false);

        defaultInRadians = defaultValue;
    }

    @Override
    public JComponent createGUI() {
        AngleSelector angleSelector = new AngleSelector(this);
        paramGUI = angleSelector;
        setParamGUIEnabledState();
        return angleSelector;
    }

    public void setValueInDegrees(int d, boolean trigger) {
        int degrees = d;
        if (degrees < 0) {
            degrees = -degrees;
        } else {
            degrees = 360 - degrees;
        }
        double r = Math.toRadians(degrees);
        setValueInRadians(r, trigger);
    }

    public void setValueInRadians(double r, boolean trigger) {
        if (angleInRadians != r) {
            angleInRadians = r;
            fireStateChanged();
        }
        if (trigger) { // trigger even if this angle was already set, because we had drag events, and now we have mouse up
            if (adjustmentListener != null) { // it is null when used in shape tools effects
                adjustmentListener.paramAdjusted();
            }
        }
    }

    public int getValueInNonIntuitiveDegrees() {
        int degrees = (int) Math.toDegrees(angleInRadians);
        return degrees;
    }

    public int getValueInDegrees() {
        int degrees = (int) Math.toDegrees(angleInRadians);
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
        return angleInRadians;
    }

    /**
     * Returns the value in the range of 0 and 2*PI, and in the intuitive direction
     */
    public double getValueInIntuitiveRadians() {
        return Utils.transformAtan2AngleToIntuitive(angleInRadians);
    }

    @Override
    public boolean isSetToDefault() {
        return (angleInRadians == defaultInRadians);
    }

    @Override
    public int getNrOfGridBagCols() {
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

    public void removeChangeListener(ChangeListener x) {
        listenerList.remove(ChangeListener.class, x);
    }

    @Override
    public void reset(boolean trigger) {
        setValueInRadians(defaultInRadians, trigger);
    }

    @Override
    public void randomize() {
        double random = Math.random();
        setValueInRadians((random * 2 * Math.PI - Math.PI), false);
    }

    public AbstractAngleSelectorComponent getAngleSelectorComponent() {
        return new AngleSelectorComponent(this);
    }

    public int getMaxAngleInDegrees() {
        return 360;
    }

    public RangeParam createRangeParam() {
        RangeParam rangeParam = new RangeParam(getName(), 0, getValueInDegrees(), getMaxAngleInDegrees()) {
            // override reset so that the clicking on the default button resets this object
            // this is good because this object has greater precision than the RangeParam
            @Override
            public void reset(boolean triggerAction) {
                if (angleInRadians != defaultInRadians) {
                    AngleParam.this.reset(triggerAction);
                }
            }
        };

        return rangeParam;
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
        return new APState(angleInRadians);
    }

    @Override
    public void setState(ParamState state) {
        angleInRadians = ((APState)state).angle;
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
        return String.format("%s[name = '%s', angleInRadians = %.2f]",
                getClass().getSimpleName(), getName(), angleInRadians);
    }
}