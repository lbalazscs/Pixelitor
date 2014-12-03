/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.gui;

import com.jhlabs.image.ImageMath;
import pixelitor.utils.Utils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.Rectangle;

/**
 * A GUIParam for selecting an angle
 */
public class AngleParam extends AbstractGUIParam {
    private double angleInRadians; // as returned form Math.atan2, this is between -PI and PI
    private double defaultInRadians = 0.0;

    private transient ChangeEvent changeEvent = null;
    private final EventListenerList listenerList = new EventListenerList();

    public AngleParam(String name, double defaultValue) {
        super(name);

        dontTrigger = true;
        setValueInRadians(defaultValue);
        dontTrigger = false;

        defaultInRadians = defaultValue;
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

    public void setValueInRadians(double r) {
        if (angleInRadians != r) {
            angleInRadians = r;
            fireStateChanged();
        }
        if (!dontTrigger) { // trigger even if this angle was already set, because we had drag events, and now we have mouse up
            if (adjustmentListener != null) { // it is null when used in shape tools effects
                adjustmentListener.paramAdjusted();
            }
        }
    }

    public void setValueInRadians(double r, boolean trigger) {
        dontTrigger = !trigger;
        setValueInRadians(r);
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
//
//        if (angleInRadians <= 0) {
//            return -angleInRadians;
//        } else {
//            return Math.PI * 2 - angleInRadians;
//        }
    }

    @Override
    public boolean isSetToDefault() {
        return (angleInRadians == defaultInRadians);
    }

    @Override
    public JComponent createGUI() {
        return new AngleSelector(this);
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
    public void reset(boolean triggerAction) {
        if (!triggerAction) {
            dontTrigger = true;
        }
        setValueInRadians(defaultInRadians);
        dontTrigger = false;
    }

    @Override
    public void randomize() {
        dontTrigger = true;
        double r = Math.random();
        setValueInRadians((r * 2 * Math.PI - Math.PI));
        dontTrigger = false;
    }

    public AbstractAngleSelectorComponent getAngleSelectorComponent() {
        return new AngleSelectorComponent(this);
    }

    public int getMaxAngleInDegrees() {
        return 360;
    }

    public RangeParam createRangeParam() {
        RangeParam rangeParam = new RangeParam(getName(), 0, getMaxAngleInDegrees(), getValueInDegrees()) {
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
        private double angle;

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
}