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

import javax.swing.*;
import java.awt.geom.Point2D;
import java.io.Serial;
import java.util.Locale;

/**
 * A {@link FilterParam} that allows selecting a complex number within
 * specified bounds in the complex plane.
 */
public class ComplexNumberParam extends AbstractFilterParam {
    private double re;
    private double im;

    private final double defaultRe;
    private final double defaultIm;

    private final double minRe;
    private final double maxRe;
    private final double minIm;
    private final double maxIm;

    public ComplexNumberParam(String name, double re, double im,
                              double minRe, double maxRe, double minIm, double maxIm) {
        super(name, RandomizeMode.ALLOW);

        if (minRe >= maxRe || minIm >= maxIm) {
            throw new IllegalArgumentException();
        }

        this.re = re;
        this.im = im;
        this.defaultRe = re;
        this.defaultIm = im;
        this.minRe = minRe;
        this.maxRe = maxRe;
        this.minIm = minIm;
        this.maxIm = maxIm;
    }

    @Override
    public JComponent createGUI() {
        var gui = new ComplexNumberParamGUI(this);
        paramGUI = gui;
        syncWithGui();
        paramGUI.updateGUI();
        return gui;
    }

    @Override
    protected void doRandomize() {
        double randomRe = minRe + Math.random() * (maxRe - minRe);
        double randomIm = minIm + Math.random() * (maxIm - minIm);
        setValue(randomRe, randomIm, true, false, false);
    }

    public double getRe() {
        return re;
    }

    public double getIm() {
        return im;
    }

    public Point2D getPoint() {
        return new Point2D.Double(re, im);
    }

    public double getMinRe() {
        return minRe;
    }

    public double getMaxRe() {
        return maxRe;
    }

    public double getMinIm() {
        return minIm;
    }

    public double getMaxIm() {
        return maxIm;
    }

    public void setValue(double re, double im,
                         boolean updateGUI, boolean isAdjusting,
                         boolean trigger) {
        this.re = re;
        this.im = im;
        if (updateGUI && paramGUI != null) {
            paramGUI.updateGUI();
        }
        if (trigger && !isAdjusting && adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
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
    public ComplexNumberParamState copyState() {
        return new ComplexNumberParamState(re, im);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        ComplexNumberParamState s = (ComplexNumberParamState) state;
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

    /**
     * Encapsulates the state of a {@link ComplexNumberParam} as a memento object.
     */
    public record ComplexNumberParamState(double re,
                                          double im) implements ParamState<ComplexNumberParamState> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public ComplexNumberParamState interpolate(ComplexNumberParamState endState, double progress) {
            double interpolatedRe = ImageMath.lerp(progress, re, endState.re);
            double interpolatedIm = ImageMath.lerp(progress, im, endState.im);
            return new ComplexNumberParamState(interpolatedRe, interpolatedIm);
        }

        @Override
        public String toPresetString() {
            return String.format(Locale.ROOT, "%.4f,%.4f", re, im);
        }
    }
}
