/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import java.awt.geom.Rectangle2D;

/**
 * A rectangle-shaped {@link BlurredShape}
 */
public class BlurredRectangle implements BlurredShape {
    private final double cx;
    private final double cy;

    private final double innerRadiusX;
    private final double innerRadiusY;

    private final double maxXDist; // max horizontal distance from the inner region
    private final double maxYDist; // max vertical distance from the inner region

    private final Rectangle2D outerRect;
    private final Rectangle2D innerRect;

    public BlurredRectangle(Point2D center,
                            double innerRadiusX, double innerRadiusY,
                            double outerRadiusX, double outerRadiusY) {
        cx = center.getX();
        cy = center.getY();
        this.innerRadiusX = innerRadiusX;
        this.innerRadiusY = innerRadiusY;

        outerRect = new Rectangle2D.Double(cx - outerRadiusX, cy - outerRadiusY,
            2 * outerRadiusX, 2 * outerRadiusY);
        innerRect = new Rectangle2D.Double(cx - innerRadiusX, cy - innerRadiusY,
            2 * innerRadiusX, 2 * innerRadiusY);

        maxXDist = outerRadiusX - innerRadiusX;
        maxYDist = outerRadiusY - innerRadiusY;
    }

    /**
     * Introducing Explaining variable and performing better encapsulation
     */
    @Override
    public double isOutside(int x, int y) {
        if (!outerRect.contains(x, y)) {
            return 1.0;
        }
        if (innerRect.contains(x, y)) {
            return 0.0;
        }

        // Calculate distances from the inner rectangle
        double horizontalDistanceFromInner = calculateHorizontalDistanceFromInner(x);
        double verticalDistanceFromInner = calculateVerticalDistanceFromInner(y);

        // Calculate ratios based on distances and maximum distances
        double xRatio = horizontalDistanceFromInner / maxXDist;
        double yRatio = verticalDistanceFromInner / maxYDist;

        // Return the maximum ratio as the blending factor
        return Math.max(xRatio, yRatio);
    }

    private double calculateHorizontalDistanceFromInner(int x) {
        double xDist;
        double minInnerX = cx - innerRadiusX;
        double maxInnerX = cx + innerRadiusX;

        if (x >= maxInnerX) {
            xDist = x - maxInnerX;
        } else if (x <= minInnerX) {
            xDist = minInnerX - x;
        } else {
            return 0.0;
        }

        return xDist;
    }

    private double calculateVerticalDistanceFromInner(int y) {
        double yDist;
        double minInnerY = cy - innerRadiusY;
        double maxInnerY = cy + innerRadiusY;

        if (y > maxInnerY) {
            yDist = y - maxInnerY;
        } else {
            yDist = maxInnerY - y;
        }
        return yDist;
    }

}
