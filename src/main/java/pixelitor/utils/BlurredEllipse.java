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

package pixelitor.utils;

import java.awt.geom.Point2D;

/**
 * An ellipse-shaped {@link BlurredShape}
 */
public class BlurredEllipse implements BlurredShape {
    private final Point2D center;

    private final double innerRadiusY;
    private final double outerRadiusX;
    private final double outerRadiusY;

    // cached squared values for circular shape
    private double innerRadius2;
    private double outerRadius2;

    // cached squared values for elliptical shape
    private double innerRadiusX2;
    private double innerRadiusY2;
    private double outerRadiusX2;
    private double outerRadiusY2;

    private final boolean isCircular;
    private final double radiusDifferenceY;

    public BlurredEllipse(Point2D center,
                          double innerRadiusX, double innerRadiusY,
                          double outerRadiusX, double outerRadiusY) {
        this.center = center;
        this.innerRadiusY = innerRadiusY;
        this.outerRadiusX = outerRadiusX;
        this.outerRadiusY = outerRadiusY;

        // This class assumes that the outer/inner radius ratios are the same
        // for the x and y radii => no need to compare the outer radii
        isCircular = innerRadiusX == innerRadiusY;

        if (isCircular) {
            innerRadius2 = innerRadiusX * innerRadiusX;
            outerRadius2 = outerRadiusX * outerRadiusX;
        } else {
            innerRadiusX2 = innerRadiusX * innerRadiusX;
            innerRadiusY2 = innerRadiusY * innerRadiusY;

            outerRadiusX2 = outerRadiusX * outerRadiusX;
            outerRadiusY2 = outerRadiusY * outerRadiusY;
        }

        radiusDifferenceY = outerRadiusY - innerRadiusY;
    }

    @Override
    public double isOutside(int x, int y) {
        double dx = x - center.getX();
        double dy = y - center.getY();

        return isCircular
            ? isOutsideCircle(dx, dy)
            : isOutsideEllipse(dx, dy);
    }

    private double isOutsideCircle(double dx, double dy) {
        double dist2 = dx * dx + dy * dy;
        if (dist2 > outerRadius2) { // outside
            return 1.0;
        } else if (dist2 < innerRadius2) { // innermost region
            return 0.0;
        } else { // between the inner and outer radius
            double distance = Math.sqrt(dist2);
            double ratio = (distance - innerRadiusY) / radiusDifferenceY; // a value between 0 and 1
            double smoothStep = 1 + ratio * ratio * (2 * ratio - 3);
            return 1.0 - smoothStep;
        }
    }

    private double isOutsideEllipse(double dx, double dy) {
        double dx2 = dx * dx;
        double dy2 = dy * dy;

        if (dy2 >= outerRadiusY2 - outerRadiusY2 * dx2 / outerRadiusX2) {  // outside
            return 1.0;
        }
        if (dy2 <= innerRadiusY2 - innerRadiusY2 * dx2 / innerRadiusX2) { // innermost region
            return 0.0;
        } else { // between the inner and outer radius
            return calcEllipseRatio(dy2, dx2);
        }
    }

    private double calcEllipseRatio(double dy2, double dx2) {
        // we are on an ellipse with unknown a and b semi major/minor axes,
        // but we know that a/b = outerRadiusX/outerRadiusY
        double ellipseDistortion = outerRadiusX / outerRadiusY;
        double b = Math.sqrt(ellipseDistortion * ellipseDistortion * dy2 + dx2) / ellipseDistortion;
        // calculate how far away we are between the two ellipses
        double ratio = (b - innerRadiusY) / radiusDifferenceY; // a value between 0 and 1
        double smoothStep = 1 + ratio * ratio * (2 * ratio - 3);
        return 1.0 - smoothStep;
    }
}
