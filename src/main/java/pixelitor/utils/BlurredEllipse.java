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

package pixelitor.utils;

import com.jhlabs.image.ImageMath;

import java.awt.geom.Point2D;

/**
 * An ellipse-shaped {@link BlurredShape}.
 */
public class BlurredEllipse implements BlurredShape {
    private final double cx;
    private final double cy;
    private final double innerRadiusY;

    // cached squared values
    private final double innerRadiusY2;
    private final double outerRadiusY2;

    private final double invRadiusDifferenceY;
    private final double scaleX;

    // this class assumes that the outer/inner radius ratios are
    // the same for the x and y radii, so it doesn't need the innerRadiusX
    public BlurredEllipse(Point2D center, double innerRadiusY,
                          double outerRadiusX, double outerRadiusY) {
        this.cx = center.getX();
        this.cy = center.getY();

        this.innerRadiusY = innerRadiusY;
        this.innerRadiusY2 = innerRadiusY * innerRadiusY;
        this.outerRadiusY2 = outerRadiusY * outerRadiusY;

        this.invRadiusDifferenceY = 1.0 / (outerRadiusY - innerRadiusY);

        // scale x-coordinates to treat the ellipse as a circle
        this.scaleX = outerRadiusY / outerRadiusX;
    }

    @Override
    public double isOutside(int x, int y) {
        double dx = (x - cx) * scaleX;
        double dy = y - cy;

        double dist2 = dx * dx + dy * dy;

        if (dist2 >= outerRadiusY2) { // outside
            return 1.0;
        } else if (dist2 <= innerRadiusY2) { // innermost region
            return 0.0;
        } else { // between the inner and outer radius
            double distance = Math.sqrt(dist2);
            double ratio = (distance - innerRadiusY) * invRadiusDifferenceY; // a value between 0 and 1
            return ImageMath.smoothStep01(ratio);
        }
    }
}
