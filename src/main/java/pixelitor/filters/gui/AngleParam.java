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
import pixelitor.utils.AngleUnit;
import pixelitor.utils.Geometry;
import pixelitor.utils.Rnd;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.io.Serial;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizeMode.ALLOW_RANDOMIZE;

/**
 * A filter parameter for selecting an angle.
 */
public class AngleParam extends AbstractFilterParam {
    // the angles are stored internally in radians
    // between -π and π, as returned form Math.atan2
    private double angle;
    private final double defaultAngle;

    private ChangeEvent changeEvent = null;
    private final EventListenerList listenerList = new EventListenerList();

    public AngleParam(String name, double def, AngleUnit unit) {
        this(name, unit.toRadians(def));
    }

    public AngleParam(String name, double def) {
        super(name, ALLOW_RANDOMIZE);
        this.defaultAngle = def;
        setValue(this.defaultAngle, false);
    }

    @Override
    public JComponent createGUI() {
        paramGUI = new AngleParamGUI(this);
        syncWithGui();
        return (JComponent) paramGUI;
    }

    public void setValueInDegrees(double degrees, boolean trigger) {
        double r = AngleUnit.INTUITIVE_DEGREES.toRadians(degrees);
        setValue(r, trigger);
    }

    public void setValue(double r, boolean trigger) {
        if (angle != r) {
            angle = r;
            fireStateChanged();
        }
        if (trigger && adjustmentListener != null) {
            // trigger even if the angle didn't change, because
            // after non-triggering drag events, we can have a
            // triggering mouse up that didn't change the angle
            adjustmentListener.paramAdjusted();
        }
    }

    public int getValueInNonIntuitiveDegrees() {
        return (int) Math.toDegrees(angle);
    }

    public double getValueInDegrees() {
        return AngleUnit.RADIANS.toIntuitiveDegrees(angle);
//        return Geometry.toIntuitiveDegrees(angle);
    }

    /**
     * Returns the "Math.atan2" radians: the value between -π and π
     */
    public double getValueInRadians() {
        return angle;
    }

    /**
     * Returns the value in the range of 0 and 2*π, and in the intuitive direction
     */
    public double getValueInIntuitiveRadians() {
        return Geometry.atan2ToIntuitive(angle);
    }

    @Override
    public boolean isAtDefault() {
        return angle == defaultAngle;
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
        setValue(defaultAngle, trigger);
    }

    @Override
    protected void doRandomize() {
        // generate a random angle in the range [-π, π]
        double randomAngle = Rnd.nextDouble() * 2 * Math.PI - Math.PI;
        setValue(randomAngle, false);
    }

    /**
     * A hook for subclasses to specify their angle selector UI.
     */
    public AbstractAngleUI getAngleSelectorUI() {
        return new AngleUI(this);
    }

    /**
     * A hook for subclasses to specify a different max angle.
     */
    public int getMaxAngleInDegrees() {
        return 360;
    }

    public RangeParam asRangeParam() {
        // At this point, the actual value can already differ from the
        // default one => ensure the returned param has the same default.
        double defaultInDegrees = Geometry.toIntuitiveDegrees(defaultAngle);
        RangeParam rangeParam = new RangeParam(getName(),
            0, defaultInDegrees, getMaxAngleInDegrees());
        rangeParam.setValueNoTrigger(getValueInDegrees());
        return rangeParam;
    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    @Override
    public AngleParamState copyState() {
        // save the value in degrees to make the interpolation more intuitive
        return new AngleParamState(getValueInDegrees());
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        setValueInDegrees(((AngleParamState) state).angle, false);
    }

    @Override
    public void loadStateFrom(String savedValue) {
        setValueInDegrees(Double.parseDouble(savedValue), false);
    }

    @Override
    public String getValueAsString() {
        double a = angle;
        if (a < 0) {
            a += 2 * Math.PI;
        }
        return format("%.2f", a);
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', angle = %.2f]",
            getClass().getSimpleName(), getName(), angle);
    }

    /**
     * Encapsulates the state of an {@link AngleParam} as a memento object.
     */
    public record AngleParamState(double angle) implements ParamState<AngleParamState> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public AngleParamState interpolate(AngleParamState endState, double progress) {
            double interpolatedAngle = ImageMath.lerp(progress, angle, endState.angle);
            return new AngleParamState(interpolatedAngle);
        }

        @Override
        public String toSaveString() {
            return "%.2f".formatted(angle);
        }
    }
}
