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
    private double re;
    private double im;

    private final double defaultRe;
    private final double defaultIm;

    private ComplexBounds bounds;

    private boolean drawCircle;
    private Predicate<Complex> randomizationCriterion;

    public ComplexNumberParam(String name, Complex value, ComplexBounds bounds) {
        super(name, RandomizeMode.ALLOW);

        if (!bounds.contains(value)) {
            throw new IllegalArgumentException();
        }

        this.bounds = bounds;
        this.re = value.re();
        this.im = value.im();
        this.defaultRe = this.re;
        this.defaultIm = this.im;
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

        setValue(c, true, false, false);
    }

    public Complex getValue() {
        return new Complex(re, im);
    }

    public double getRe() {
        return re;
    }

    public double getIm() {
        return im;
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
        this.re = bounds.clampRe(re);
        this.im = bounds.clampIm(im);

        if (paramGUI != null) {
            // a plain updateGUI() doesn't update the bounds
            ((ComplexNumberParamGUI) paramGUI).updateBounds();
        }
    }

    public void setValue(Complex c,
                         boolean updateGUI, boolean isAdjusting,
                         boolean trigger) {
        setValue(c.re(), c.im(), updateGUI, isAdjusting, trigger);
    }

    public void setValue(double re, double im,
                         boolean updateGUI, boolean isAdjusting,
                         boolean trigger) {
        this.re = bounds.clampRe(re);
        this.im = bounds.clampIm(im);
        if (updateGUI && paramGUI != null) {
            paramGUI.updateGUI();
        }
        if (trigger && !isAdjusting) {
            triggerFilter();
        }
    }

    public void setValue(double re, double im) {
        setValue(re, im, true, false, true);
    }

    public void setRe(double re, boolean isAdjusting) {
        setValue(re, this.im, true, isAdjusting, true);
    }

    public void setIm(double im, boolean isAdjusting) {
        setValue(this.re, im, true, isAdjusting, true);
    }

    public void setRe(double re) {
        setRe(re, false);
    }

    public void setIm(double im) {
        setIm(im, false);
    }

    @Override
    public boolean isAtDefault() {
        return re == defaultRe && im == defaultIm;
    }

    @Override
    public void reset(boolean trigger) {
        setValue(defaultRe, defaultIm, true, false, trigger);
    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    @Override
    public Complex copyState() {
        return new Complex(re, im);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        Complex s = (Complex) state;
        setValue(s.re(), s.im(), updateGUI, false, false);
    }

    @Override
    public void loadStateFrom(String savedValue) {
        int commaIndex = savedValue.indexOf(',');
        double newRe = Double.parseDouble(savedValue.substring(0, commaIndex).trim());
        double newIm = Double.parseDouble(savedValue.substring(commaIndex + 1).trim());
        setValue(newRe, newIm, true, false, false);
    }

    @Override
    public String getValueAsString() {
        return String.format(Locale.ROOT, "(%.2f, %.2f)", re, im);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s[name = '%s', re = %.2f, im = %.2f]",
            getClass().getSimpleName(), getName(), re, im);
    }
}
