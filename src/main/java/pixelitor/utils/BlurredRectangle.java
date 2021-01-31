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

    @Override
    public double isOutside(int x, int y) {
        if (!outerRect.contains(x, y)) { // outside
            return 1.0;
        } else if (innerRect.contains(x, y)) { // innermost region
            return 0.0;
        } else { // between the inner and outer radius
            double xDist; // horizontal distance from the inner region
            double yDist; // vertical distance from the inner region
            double minInnerX = cx - innerRadiusX;
            double maxInnerX = cx + innerRadiusX;
            double minInnerY = cy - innerRadiusY;
            double maxInnerY = cy + innerRadiusY;
            if (x >= maxInnerX) {
                xDist = x - cx - innerRadiusX;
                double xRatio = xDist / maxXDist;
                if (y <= minInnerY) { // top right corner
                    yDist = cy - y - innerRadiusY;
                    double yRatio = yDist / maxYDist;
                    return Math.max(xRatio, yRatio);
                } else if (y > maxInnerY) { // bottom right corner
                    yDist = y - cy - innerRadiusY;
                    double yRatio = yDist / maxYDist;
                    return Math.max(xRatio, yRatio);
                } else { // right part
                    return xRatio;
                }
            } else if (x <= minInnerX) {
                xDist = cx - x - innerRadiusX;
                double xRatio = xDist / maxXDist;
                if (y <= minInnerY) { // top left corner
                    yDist = cy - y - innerRadiusY;
                    double yRatio = yDist / maxYDist;
                    return Math.max(xRatio, yRatio);
                } else if (y > maxInnerY) { // bottom left corner
                    yDist = y - cy - innerRadiusY;
                    double yRatio = yDist / maxYDist;
                    return Math.max(xRatio, yRatio);
                } else { // left part
                    return xRatio;
                }
            }

            if (y > cy) { // bottom part
                yDist = y - cy - innerRadiusY;
            } else { // top part
                yDist = cy - y - innerRadiusY;
            }
            return yDist / maxYDist;
        }
    }
}
