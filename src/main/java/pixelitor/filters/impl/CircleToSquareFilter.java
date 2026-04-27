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

import pixelitor.filters.CircleToSquare;
import pixelitor.utils.CustomShapes;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The implementation of the {@link CircleToSquare} filter.
 * Distorts a circle into a square, or an ellipse into a rectangle.
 */
public class CircleToSquareFilter extends CenteredTransformFilter {
    private final float radiusX;
    private final float radiusY;
    private final float amount;

    /**
     * Constructs a {@link CircleToSquareFilter}.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param center        the effect's center (in pixels).
     * @param radiusX       the horizontal radius of the transformation area.
     * @param radiusY       the vertical radius of the transformation area.
     * @param amount        the intensity of the distortion.
     */
    public CircleToSquareFilter(String filterName, int edgeAction, int interpolation, Point2D center,
                                float radiusX, float radiusY, float amount) {
        super(filterName, edgeAction, interpolation, center);

        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.amount = amount;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double dx = x - cx;
        double dy = y - cy;

        double xDist = Math.abs(dx);
        double yDist = Math.abs(dy);

        if (xDist > radiusX || yDist > radiusY) { // out of the affected area
            out[0] = x;
            out[1] = y;
            return;
        }

        // we need the amount by which a pixel is pushed outward, but
        // since this in an inverse mapping, we calculate its inverse
        double magnificationInverse = calcMagnificationInverse(xDist, yDist);

        double srcX = cx + dx * magnificationInverse;
        double srcY = cy + dy * magnificationInverse;

        if (amount == 1.0f) { // the default value of the slider
            out[0] = (float) srcX;
            out[1] = (float) srcY;
        } else {
            out[0] = interpolate(x, srcX);
            out[1] = interpolate(y, srcY);
        }
    }

    private double calcMagnificationInverse(double xDist, double yDist) {
        double scaledXDist, scaledYDist;
        if (radiusX == radiusY) {
            scaledXDist = xDist;
            scaledYDist = yDist;
        } else {
            // normalize the ellipse to a circle; only the angle matters, not the radius
            scaledXDist = xDist / radiusX;
            scaledYDist = yDist / radiusY;
        }

        double maxScaledDist = Math.max(scaledXDist, scaledYDist);
        double distSq = scaledXDist * scaledXDist + scaledYDist * scaledYDist;

        // close to an axis, the return value is ≈ 1, close to a diagonal it's ≈ 0.7
        return distSq == 0.0 ? 1.0 : maxScaledDist / Math.sqrt(distSq);
    }

    private float interpolate(int original, double transformed) {
        return (float) (original + amount * (transformed - original));
    }

    public Shape[] getAffectedAreaShapes() {
        Shape rect = new Rectangle2D.Double(cx - radiusX, cy - radiusY, 2 * radiusX, 2 * radiusY);
        Shape ellipse = CustomShapes.createEllipse(cx, cy, radiusX, radiusY);
        return new Shape[]{rect, ellipse};
    }
}
