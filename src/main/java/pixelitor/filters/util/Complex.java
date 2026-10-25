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

package pixelitor.filters.util;

import com.jhlabs.image.ImageMath;
import pixelitor.filters.gui.ComplexNumberParam;
import pixelitor.filters.gui.ParamState;

import java.io.Serial;
import java.util.Locale;

/**
 * Represents a complex number. It also encapsulates the
 * state of a {@link ComplexNumberParam} as a memento object.
 */
public record Complex(double re,
                      double im) implements ParamState<Complex> {
    @Serial
    private static final long serialVersionUID = 1L;

    public static Complex fromPresetString(String s) {
        int comma = s.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("Invalid complex number: '" + s + "'");
        }
        double re = Double.parseDouble(s.substring(0, comma).trim());
        double im = Double.parseDouble(s.substring(comma + 1).trim());
        if (!Double.isFinite(re) || !Double.isFinite(im)) {
            throw new IllegalArgumentException("Non-finite complex number: '" + s + "'");
        }
        return new Complex(re, im);
    }

    public double distSqFrom(double otherRe, double otherIm) {
        double dRe = otherRe - re;
        double dIm = otherIm - im;
        return dRe * dRe + dIm * dIm;
    }

    @Override
    public Complex interpolate(Complex endState, double progress) {
        // interpolates in Cartesian coordinates, not polar
        double interpolatedRe = ImageMath.lerp(progress, re, endState.re);
        double interpolatedIm = ImageMath.lerp(progress, im, endState.im);
        return new Complex(interpolatedRe, interpolatedIm);
    }

    @Override
    public String toPresetString() {
        return String.format(Locale.ROOT, "%.4f,%.4f", re, im);
    }
}
