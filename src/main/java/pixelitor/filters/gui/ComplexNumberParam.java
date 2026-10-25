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

import pixelitor.filters.util.Complex;
import pixelitor.filters.util.ComplexBounds;

import javax.swing.*;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * A {@link FilterParam} that allows selecting a complex number within
 * specified bounds in the complex plane.
 */
public class ComplexNumberParam extends AbstractFilterParam {
    private Complex value;
    private final Complex defaultValue;

    private ComplexBounds bounds;

    private boolean drawCircle;
    private Predicate<Complex> randomizationCriterion;

    public ComplexNumberParam(String name, Complex value, ComplexBounds bounds) {
        super(name, RandomizeMode.ALLOW);

        if (!bounds.contains(value)) {
            throw new IllegalArgumentException(
                "value %s is outside bounds %s".formatted(value, bounds));
        }

        this.bounds = bounds;
        this.value = value;
        this.defaultValue = value;
    }

    @Override
    public JComponent createGUI() {
        return installGUI(new ComplexNumberParamGUI(this));
    }

    // when selecting a step multiplier for the Newton fractal,
    // draw a circle centered at (1, 0) with radius 1,
    // and restrict randomization to the interior of that circle
    public ComplexNumberParam forNewton() {
        drawCircle = true;
        randomizationCriterion = c -> c.distSqFrom(1, 0) < 1;
        return this;
    }

    public boolean shouldDrawCircle() {
        return drawCircle;
    }

    @Override
    protected void doRandomize() {
        Complex c = bounds.random();

        if (randomizationCriterion != null) {
            int attempts = 0;
            while (!randomizationCriterion.test(c) && ++attempts < 20) {
                c = bounds.random();
            }
        }

        setValue(c, true, false);
    }

    public Complex getValue() {
        return value;
    }

    public double getRe() {
        return value.re();
    }

    public double getIm() {
        return value.im();
    }

    public ComplexBounds getBounds() {
        return bounds;
    }

    /**
     * Updates the bounds of this parameter without triggering a filter preview.
     * Clamps the current value if it falls outside the new bounds,
     * and updates the GUI if it has already been initialized.
     */
    public void updateBounds(ComplexBounds newBounds) {
        this.bounds = newBounds;
        this.value = new Complex(bounds.clampRe(value.re()), bounds.clampIm(value.im()));

        if (paramGUI != null) {
            // a plain updateGUI() doesn't update the bounds
            ((ComplexNumberParamGUI) paramGUI).updateBounds();
        }
    }

    public void setValue(Complex c, boolean updateGUI, boolean trigger) {
        setValue(c.re(), c.im(), updateGUI, trigger);
    }

    public void setValue(double re, double im, boolean updateGUI, boolean trigger) {
        this.value = new Complex(bounds.clampRe(re), bounds.clampIm(im));
        if (updateGUI && paramGUI != null) {
            paramGUI.updateGUI();
        }
        if (trigger) {
            triggerFilter();
        }
    }

    public void setValue(double re, double im) {
        setValue(re, im, true, true);
    }

    public void setRe(double re) {
        setValue(re, value.im());
    }

    public void setIm(double im) {
        setValue(value.re(), im);
    }

    @Override
    public boolean isAtDefault() {
        return value.re() == defaultValue.re() && value.im() == defaultValue.im();
    }

    @Override
    public void reset(boolean trigger) {
        setValue(defaultValue, true, trigger);
    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    @Override
    public Complex copyState() {
        return value;
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        Complex s = (Complex) state;
        setValue(s, updateGUI, false);
    }

    @Override
    public void loadStateFrom(String savedValue) {
        Complex newValue = Complex.fromPresetString(savedValue);
        setValue(newValue, true, false);
    }

    @Override
    public String getValueAsString() {
        return String.format(Locale.ROOT, "(%.2f, %.2f)", value.re(), value.im());
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s[name = '%s', re = %.2f, im = %.2f]",
            getClass().getSimpleName(), getName(), value.re(), value.im());
    }
}
