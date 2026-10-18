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

/**
 * Rectangular bounds in the complex plane.
 */
public record ComplexBounds(double minRe, double maxRe, double minIm, double maxIm) {
    public ComplexBounds {
        if (!Double.isFinite(minRe) || !Double.isFinite(maxRe)
            || !Double.isFinite(minIm) || !Double.isFinite(maxIm)) {
            throw new IllegalArgumentException(
                "bounds must be finite: re = [%s, %s], im = [%s, %s]"
                    .formatted(minRe, maxRe, minIm, maxIm));
        }
        if (minRe >= maxRe) {
            throw new IllegalArgumentException(
                "minRe (%s) must be less than maxRe (%s)".formatted(minRe, maxRe));
        }
        if (minIm >= maxIm) {
            throw new IllegalArgumentException(
                "minIm (%s) must be less than maxIm (%s)".formatted(minIm, maxIm));
        }
    }

    public double width() {
        return maxRe - minRe;
    }

    public double height() {
        return maxIm - minIm;
    }

    public boolean containsRe(double re) {
        return re >= minRe && re <= maxRe;
    }

    public boolean containsIm(double im) {
        return im >= minIm && im <= maxIm;
    }

    public double clampRe(double re) {
        return Math.clamp(re, minRe, maxRe);
    }

    public double clampIm(double im) {
        return Math.clamp(im, minIm, maxIm);
    }

    /**
     * Returns a uniformly distributed random value in [minRe, maxRe).
     */
    public double randomRe() {
        return minRe + Math.random() * width();
    }

    /**
     * Returns a uniformly distributed random value in [minIm, maxIm).
     */
    public double randomIm() {
        return minIm + Math.random() * height();
    }
}
