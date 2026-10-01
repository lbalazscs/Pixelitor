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

import com.jhlabs.image.ImageMath;
import pixelitor.filters.gui.EnumParam;

import java.util.function.DoubleBinaryOperator;

/**
 * A distance metric that gives the concentric patterns of radial filters a non-circular shape.
 */
public enum PolarMetric {
    CIRCLE("Circle", ImageMath::hypot, 1.0),                   // Euclidean (Minkowski p=2)
    SQUIRCLE("Squircle", PolarMetric::calcSquircle, 1.0),      // Minkowski p=4
    SQUARE("Square", PolarMetric::calcSquare, 1.0),            // Chebyshev (Minkowski p=∞)
    DIAMOND("Diamond", PolarMetric::calcDiamond, ImageMath.SQRT_2),      // Manhattan (Minkowski p=1)
    ASTROID("Astroid", PolarMetric::calcAstroid, 2.0),         // Minkowski p=2/3
    STAR("Star", PolarMetric::calcStar, ImageMath.SQRT_2 * 2); // Minkowski p=1/2

    private final String displayName;
    private final DoubleBinaryOperator distFunction;

    // the maximum ratio of the metric to the Euclidean distance
    // (dividing by it normalizes each shape so its closest points
    // to the center sit on the Euclidean circle of the same radius,
    // which keeps wavelengths and ring spacing comparable across shapes)
    private final double circleFitFactor;

    PolarMetric(String displayName, DoubleBinaryOperator distFunction, double circleFitFactor) {
        this.displayName = displayName;
        this.distFunction = distFunction;
        this.circleFitFactor = circleFitFactor;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the function that calculates the distance
     * from the (dx, dy) offset relative to a center point.
     */
    public DoubleBinaryOperator getDistFunction() {
        return distFunction;
    }

    /**
     * Returns the factor by which the distance function's result is divided
     * (or wavelengths are multiplied) so that all shapes look similar.
     */
    public double getCircleFitFactor() {
        return circleFitFactor;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static EnumParam<PolarMetric> asParam() {
        return new EnumParam<>("Shape", PolarMetric.class);
    }

    private static double calcSquare(double dx, double dy) {
        return Math.max(Math.abs(dx), Math.abs(dy));
    }

    private static double calcDiamond(double dx, double dy) {
        return Math.abs(dx) + Math.abs(dy);
    }

    private static double calcSquircle(double dx, double dy) {
        double dx2 = dx * dx;
        double dy2 = dy * dy;
        return Math.sqrt(Math.sqrt(dx2 * dx2 + dy2 * dy2));
    }

    private static double calcAstroid(double dx, double dy) {
        double s = Math.cbrt(dx * dx) + Math.cbrt(dy * dy);
        return s * Math.sqrt(s);
    }

    private static double calcStar(double dx, double dy) {
        double s = Math.sqrt(Math.abs(dx)) + Math.sqrt(Math.abs(dy));
        return s * s;
    }
}
