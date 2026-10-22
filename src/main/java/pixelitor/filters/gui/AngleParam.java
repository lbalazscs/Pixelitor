/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A filter parameter for selecting an angle.
 */
public class AngleParam extends AbstractFilterParam {
    // The angles are stored internally in radians.
    // Depending on how the value was set, it can be in [-π, π]
    // (e.g. as returned from Math.atan2 in AngleSelector)
    // or in [0, 2π) (e.g. from normalize() or setValueInDegrees()).
    private double angle;
    private final double defaultAngle;

    private boolean hasBorder = true;

    private List<Runnable> changeListeners;

    public AngleParam(String name, double def, AngleUnit unit) {
        this(name, unit.toRadians(def));
    }

    public AngleParam(String name, double def) {
        super(name, RandomizeMode.ALLOW);
        this.defaultAngle = normalize(def);
        angle = defaultAngle;
    }

    @Override
    public JComponent createGUI() {
        return installGUI(new AngleParamGUI(this));
    }

    public void setValueInDegrees(double degrees, boolean trigger) {
        double r = AngleUnit.INTUITIVE_DEGREES.toRadians(degrees);
        setValue(r, trigger);
    }

    public void setValue(double a, boolean trigger) {
        if (angle != a) {
            angle = a;
            notifyChangeListeners();
        }

        if (trigger) {
            // trigger even if the angle didn't change, because
            // after non-triggering drag events, we can have a
            // triggering mouse up that didn't change the angle
            triggerFilter();
        }
    }

    public int getValueInAtan2Degrees() {
        return (int) Math.toDegrees(angle);
    }

    public double getValueInDegrees() {
        return AngleUnit.RADIANS.toIntuitiveDegrees(angle);
    }

    /**
     * Returns the angle in radians. Depending on how the value was set,
     * it can be in [-π, π] or [0, 2π).
     */
    public double getValueInRadians() {
        return angle;
    }

    /**
     * Returns the value in the range of 0 and 2*π, and in the intuitive direction.
     */
    public double getValueInIntuitiveRadians() {
        return Geometry.atan2ToIntuitive(angle);
    }

    @Override
    public boolean isAtDefault() {
        double diff = Math.abs(normalize(angle) - defaultAngle);
        return diff < 1e-6 || Math.TAU - diff < 1e-6;
    }

    @Override
    public void setEmbedded(FilterParam.Layout layout) {
        if (layout != Layout.DIALOG) {
            hasBorder = false;
        }
    }

    public boolean hasBorder() {
        return hasBorder;
    }

    private void notifyChangeListeners() {
        if (changeListeners != null) {
            changeListeners.forEach(Runnable::run);
        }
    }

    /**
     * Listeners added through this method are discarded when the
     * GUI is disposed. Therefore, this method should not be used
     * to establish permanent dependencies between filter parameters.
     */
    public void addUIChangeListener(Runnable listener) {
        if (changeListeners == null) {
            changeListeners = new ArrayList<>(2);
        }
        changeListeners.add(listener);
    }

    @Override
    public void guiDisposed() {
        super.guiDisposed();
        changeListeners = null;
    }

    @Override
    public void reset(boolean trigger) {
        setValue(defaultAngle, trigger);
    }

    @Override
    protected void doRandomize() {
        setValue(Rnd.nextAngle(), false);
    }

    /**
     * A hook for subclasses to specify their angle selector UI.
     */
    public AbstractAngleSelector getAngleSelector() {
        return new AngleSelector(this);
    }

    /**
     * A hook for subclasses to specify a different max angle.
     */
    public int getMaxAngleInDegrees() {
        return 360;
    }

    public RangeParam asRangeParam() {
        // at this point, the actual value can already differ from the
        // default one => ensure the returned param has the same default
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
        double degrees = ((AngleParamState) state).angle;
        double radians = AngleUnit.INTUITIVE_DEGREES.toRadians(degrees);

        if (updateGUI) {
            setValue(radians, false);
        } else {
            angle = radians;
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {
        setValueInDegrees(Double.parseDouble(savedValue), false);
    }

    @Override
    public String getValueAsString() {
        return String.format(Locale.ROOT, "%.2f", normalize(angle));
    }

    private static double normalize(double a) {
        double r = a % Math.TAU;
        if (r < 0) {
            r += Math.TAU;
        }
        if (r >= Math.TAU || r == 0.0) {
            return 0.0;
        }
        return r;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s[name = '%s', angle = %.2f]",
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
        public String toPresetString() {
            return String.format(Locale.ROOT, "%.2f", angle);
        }
    }
}
