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

package pixelitor.filters.impl;

import pixelitor.filters.util.Complex;
import pixelitor.filters.util.ComplexBounds;

import java.awt.geom.Point2D;

/**
 * Newton's and Halley's methods for zⁿ − 1 = 0 with a complex relaxation factor a:
 * z ← z − a·Δz, where Δz is the classic correction (f/f′ for Newton).
 * a = 1 gives the classic methods; the roots attract iff |1 − a| < 1.
 */
public class NewtonFractalFilter extends ComplexPlaneFilter {
    public static final int METHOD_NEWTON = 0;
    public static final int METHOD_HALLEY = 1;

    private static final double CONVERGENCE_TOLERANCE_SQ = 1e-8;

    private final int maxIterations;
    private final int n;
    private final int[][] colors;
    private final int method;

    // the complex relaxation factor
    private final double reA;
    private final double imA;

    // precomputed roots of unity: rootRe[k] = cos(2πk/n), rootIm[k] = sin(2πk/n)
    private final double[] rootRe;
    private final double[] rootIm;

    public NewtonFractalFilter(String filterName,
                               ComplexBounds bounds,
                               int n,
                               double zoom,
                               Point2D zoomCenter,
                               int maxIterations,
                               int[][] colors,
                               int method,
                               Complex a) {
        super(filterName, bounds, zoom, zoomCenter, false);

        this.n = n;
        this.maxIterations = maxIterations;
        this.colors = colors;
        this.method = method;
        this.reA = a.re();
        this.imA = a.im();

        this.rootRe = new double[n];
        this.rootIm = new double[n];
        for (int k = 0; k < n; k++) {
            double angle = (2.0 * Math.PI * k) / n;
            rootRe[k] = Math.cos(angle);
            rootIm[k] = Math.sin(angle);
        }
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        double zx = xStart + x * xMultiplier;
        double zy = yStart + y * yMultiplier;

        return calcIteratedColor(zx, zy);
    }

    private int calcIteratedColor(double zx, double zy) {
        return switch (method) {
            case METHOD_NEWTON -> calcNewton(zx, zy);
            case METHOD_HALLEY -> calcHalley(zx, zy);
            default -> throw new IllegalStateException("Unexpected method: " + method);
        };
    }

    private int calcNewton(double zx, double zy) {
        double invN = 1.0 / n;

        for (int step = 1; step <= maxIterations; step++) {
            if (!Double.isFinite(zx) || !Double.isFinite(zy)) {
                return IN_SET_COLOR;
            }

            // calculate w = z^(n - 1)
            double u = zx;
            double v = zy;
            for (int p = 1; p < n - 1; p++) {
                double nextU = u * zx - v * zy;
                double nextV = u * zy + v * zx;
                u = nextU;
                v = nextV;
            }

            double denom = n * (u * u + v * v);
            if (denom == 0.0) {
                return IN_SET_COLOR; // derivative is zero (singularity at origin)
            }

            // Newton step for f(z) = zⁿ − 1:
            // Δz = f(z) / f'(z) = (zⁿ - 1) / (n * zⁿ⁻¹) = z / n - 1 / (n * zⁿ⁻¹)
            // 1 / (n * zⁿ⁻¹) = (u - i*v) / denom
            double deltaX = zx * invN - u / denom;
            double deltaY = zy * invN + v / denom;

            // relaxed step: a * Δz = (reA + i*imA) * (deltaX + i*deltaY)
            double stepX = reA * deltaX - imA * deltaY;
            double stepY = reA * deltaY + imA * deltaX;

            double nextZx = zx - stepX;
            double nextZy = zy - stepY;

            if (isConverged(zx, zy, deltaX, deltaY)) {
                return colors[findClosestRoot(nextZx, nextZy)][step];
            }

            zx = nextZx;
            zy = nextZy;
        }

        return IN_SET_COLOR;
    }

    private int calcHalley(double zx, double zy) {
        for (int step = 1; step <= maxIterations; step++) {
            if (!Double.isFinite(zx) || !Double.isFinite(zy)) {
                return IN_SET_COLOR;
            }

            // calculate w = zⁿ
            double u = zx;
            double v = zy;
            for (int p = 1; p < n; p++) {
                double nextU = u * zx - v * zy;
                double nextV = u * zy + v * zx;
                u = nextU;
                v = nextV;
            }

            // Halley step for f(z) = zⁿ − 1:
            // Δz = 2 * z * (zⁿ − 1) / ((n + 1) * zⁿ + (n − 1))
            double dx = (n + 1) * u + (n - 1);
            double dy = (n + 1) * v;
            double denom = dx * dx + dy * dy;
            if (denom < 1e-14 || !Double.isFinite(denom)) {
                return IN_SET_COLOR;
            }

            // 2 * z * (zⁿ − 1) = 2 * (zx + i*zy) * ((u - 1) + i*v)
            double um1 = u - 1.0;
            double numX = 2.0 * (zx * um1 - zy * v);
            double numY = 2.0 * (zx * v + zy * um1);

            double deltaX = (numX * dx + numY * dy) / denom;
            double deltaY = (numY * dx - numX * dy) / denom;

            // relaxed step: a * Δz = (reA + i*imA) * (deltaX + i*deltaY)
            double stepX = reA * deltaX - imA * deltaY;
            double stepY = reA * deltaY + imA * deltaX;

            double nextZx = zx - stepX;
            double nextZy = zy - stepY;

            if (isConverged(zx, zy, deltaX, deltaY)) {
                return colors[findClosestRoot(nextZx, nextZy)][step];
            }

            zx = nextZx;
            zy = nextZy;
        }

        return IN_SET_COLOR;
    }

    /**
     * Checks if the iteration has converged to an actual root of zⁿ − 1 = 0.
     * <p>
     * Convergence is tested using the unrelaxed correction Δz rather than the relaxed step a·Δz,
     * so that the tolerance is independent of the relaxation factor a and does not falsely
     * report convergence when |a| is small or zero.
     * <p>
     * In Halley's method, z = 0 is an extraneous repelling fixed point where the step size Δz ∝ z
     * becomes arbitrarily small near the origin. Since all actual roots lie on the unit circle (|z| = 1),
     * checking that |z|² > 0.25 prevents false convergence at the origin and allows points
     * near the origin to escape and converge to a true root.
     */
    private static boolean isConverged(double zx, double zy, double deltaX, double deltaY) {
        return deltaX * deltaX + deltaY * deltaY < CONVERGENCE_TOLERANCE_SQ
            && zx * zx + zy * zy > 0.25;
    }

    /**
     * Finds the index k ∈ [0, n - 1] of the closest root of unity e^(i * 2πk / n).
     * Since all roots lie on the unit circle (|root| = 1), minimizing the Euclidean
     * distance |z - root| is equivalent to maximizing the dot product z · root.
     */
    private int findClosestRoot(double zx, double zy) {
        int best = 0;
        double bestDot = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < n; k++) {
            double dot = zx * rootRe[k] + zy * rootIm[k];
            if (dot > bestDot) {
                bestDot = dot;
                best = k;
            }
        }
        return best;
    }
}
