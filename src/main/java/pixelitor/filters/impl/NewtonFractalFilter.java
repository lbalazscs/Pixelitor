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

import net.jafama.FastMath;

/**
 * Filter implementing Newton's and Halley's root-finding methods for solving zⁿ − 1 = 0 on the complex plane.
 */
public class NewtonFractalFilter extends ComplexPlaneFilter {
    public static final int METHOD_NEWTON = 0;
    public static final int METHOD_HALLEY = 1;

    private static final double CONVERGENCE_TOLERANCE_SQ = 1e-8;

    private final int maxIterations;
    private final int n;
    private final int[][] colors;
    private final int method;

    public NewtonFractalFilter(double xMin, double xMax,
                               double yMin, double yMax,
                               int n,
                               double zoom,
                               double zoomCenterX,
                               double zoomCenterY,
                               int maxIterations,
                               int[][] colors,
                               int method) {
        super("Newton Fractal", xMin, xMax, yMin, yMax, zoom, zoomCenterX, zoomCenterY);

        this.n = n;
        this.maxIterations = maxIterations;
        this.colors = colors;
        this.method = method;
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
            // z_(m+1) = ((n - 1) / n) * z + 1 / (n * z^(n - 1))
            double factor = (double) (n - 1) / n;
            double nextZx = factor * zx + u / denom;
            double nextZy = factor * zy - v / denom;

            if (isConverged(zx, zy, nextZx, nextZy)) {
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

            double nextZx = zx - deltaX;
            double nextZy = zy - deltaY;

            if (isConverged(zx, zy, nextZx, nextZy)) {
                return colors[findClosestRoot(nextZx, nextZy)][step];
            }

            zx = nextZx;
            zy = nextZy;
        }

        return IN_SET_COLOR;
    }

    /**
     * Checks if the iteration step has converged to an actual root of zⁿ − 1 = 0.
     * <p>
     * In Halley's method, z = 0 is an extraneous repelling fixed point where the step size Δz ∝ z
     * becomes arbitrarily small near the origin. Since all actual roots lie on the unit circle (|z| = 1),
     * checking that |nextZ|² > 0.25 prevents false convergence at the origin and allows points
     * near the origin to escape and converge to a true root.
     */
    private static boolean isConverged(double zx, double zy, double nextZx, double nextZy) {
        double diffX = nextZx - zx;
        double diffY = nextZy - zy;
        return diffX * diffX + diffY * diffY < CONVERGENCE_TOLERANCE_SQ
            && (nextZx * nextZx + nextZy * nextZy > 0.25);
    }

    /**
     * Derives the index k ∈ [0, n - 1] of the closest root of unity e^(i * 2πk / n) directly from the angle.
     */
    private int findClosestRoot(double zx, double zy) {
        double angle = FastMath.atan2(zy, zx);
        if (angle < 0.0) {
            angle += 2.0 * Math.PI;
        }
        return (int) Math.round(angle * n / (2.0 * Math.PI)) % n;
    }
}
