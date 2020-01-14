/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import net.jafama.FastMath;

import java.awt.geom.Point2D;

/**
 * An ellipse-shaped {@link BlurredShape}
 */
public class BlurredEllipse implements BlurredShape {
    private final Point2D center;

    private final boolean linkedRadius;
    private final double innerRadiusY;
    private double innerRadius2;
    private double innerRadiusX2;
    private double innerRadiusY2;

    private final double outerRadiusX;
    private final double outerRadiusY;
    private double outerRadius2;
    private double outerRadiusX2;
    private double outerRadiusY2;

    private final double yRadiusDifference;

    public BlurredEllipse(Point2D center,
                          double innerRadiusX, double innerRadiusY,
                          double outerRadiusX, double outerRadiusY) {
        this.center = center;
        this.innerRadiusY = innerRadiusY;
        this.outerRadiusX = outerRadiusX;
        this.outerRadiusY = outerRadiusY;

        // This class assumes that the outer/inner radius ratios are the same
        // for the x and y radii => no need to compare the outer radii
        linkedRadius = innerRadiusX == innerRadiusY;

        if (linkedRadius) {
            innerRadius2 = innerRadiusX * innerRadiusX;
            outerRadius2 = outerRadiusX * outerRadiusX;
        } else {
            innerRadiusX2 = innerRadiusX * innerRadiusX;
            innerRadiusY2 = innerRadiusY * innerRadiusY;

            outerRadiusX2 = outerRadiusX * outerRadiusX;
            outerRadiusY2 = outerRadiusY * outerRadiusY;
        }

        yRadiusDifference = outerRadiusY - innerRadiusY;
    }

    @Override
    public double isOutside(int x, int y) {
        double dx = x - center.getX();
        double dy = y - center.getY();
        double dist2 = dx * dx + dy * dy;

        if (linkedRadius) {
            return isOutsideCircle(dist2);
        } else {
            return isOutsideEllipsis(dx, dy);
        }
    }

    private double isOutsideCircle(double dist2) {
        if (dist2 > outerRadius2) { // outside
            return 1.0;
        } else if (dist2 < innerRadius2) { // innermost region
            return 0.0;
        } else { // between the inner and outer radius
            double distance = Math.sqrt(dist2);
            double ratio = (distance - innerRadiusY) / yRadiusDifference; // a value between 0 and 1

//                double trigRatio = (FastMath.cos(ratio * Math.PI) + 1.0) / 2.0;
            // 1- smooth step is faster than cosine interpolation
            // http://en.wikipedia.org/wiki/Smoothstep
            // http://www.wolframalpha.com/input/?i=Plot[{%281+%2B+Cos[a+*+Pi]%29%2F2%2C+1+-+3+*+a+*+a+%2B+2+*+a+*+a+*a}%2C+{a%2C+0%2C+1}]
            double trigRatio = 1 + ratio * ratio * (2 * ratio - 3);
            return 1.0 - trigRatio;
        }
    }

    private double isOutsideEllipsis(double dx, double dy) {
        double dx2 = dx * dx;
        double dy2 = dy * dy;

        if (dy2 >= outerRadiusY2 - outerRadiusY2 * dx2 / outerRadiusX2) {  // outside
            return 1.0;
        }
        if (dy2 <= innerRadiusY2 - innerRadiusY2 * dx2 / innerRadiusX2) { // innermost region
            return 0.0;
        } else { // between the inner and outer radius
            // we are on an ellipse with unknown a and b semi major/minor axes
            // but we know that a/b = outerRadiusX/outerRadiusY
            double ellipseDistortion = outerRadiusX / outerRadiusY;
            double b = Math.sqrt(ellipseDistortion * ellipseDistortion * dy2 + dx2) / ellipseDistortion;
            // calculate how far away we are between the two ellipses
            double ratio = (b - innerRadiusY) / yRadiusDifference; // a value between 0 and 1
            double trigRatio = (FastMath.cos(ratio * Math.PI) + 1.0) / 2.0;

            return 1.0 - trigRatio;
        }
    }
}
