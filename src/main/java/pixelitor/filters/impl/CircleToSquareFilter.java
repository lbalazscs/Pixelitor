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

package pixelitor.filters.impl;

import net.jafama.FastMath;
import pixelitor.filters.CircleToSquare;
import pixelitor.utils.CustomShapes;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/**
 * The implementation of the {@link CircleToSquare} filter.
 * Distorts a circle into a square
 */
public class CircleToSquareFilter extends CenteredTransformFilter {
    private float radiusX = 500;
    private float radiusY = 500;
    private float amount = 1.0f;

    public CircleToSquareFilter() {
        super(CircleToSquare.NAME);
    }

    public void setRadiusX(float radius) {
        radiusX = radius;
    }

    public void setRadiusY(float radius) {
        radiusY = radius;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public Shape[] getAffectedAreaShapes() {
        Shape rect = new Rectangle2D.Double(cx - radiusX, cy - radiusY, 2 * radiusX, 2 * radiusY);
        Shape ellipse = CustomShapes.createEllipse(cx, cy, radiusX, radiusY);
        return new Shape[]{rect, ellipse};
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

        double sdx, sdy, sXDist, sYDist;
        if (radiusX == radiusY) {
            sdx = dx;
            sdy = dy;
            sXDist = xDist;
            sYDist = yDist;
        } else {
            // if the coordinates are stretched, then it becomes an
            // ellipse-to-rectangle distortion
            sdx = dx / radiusX;
            sdy = dy / radiusY;
            sXDist = xDist / radiusX;
            sYDist = yDist / radiusY;
        }

        double angle;
        if (sXDist >= sYDist) { // we want to move from a vertical line  to the circle
            angle = FastMath.atan2(sdy, sXDist);
        } else { // move from horizontal line
            //noinspection SuspiciousNameCombination
            angle = FastMath.atan2(sdx, sYDist);
        }

        double magnificationInverse = FastMath.cos(angle);

        double transformedX = cx + dx * magnificationInverse;
        double transformedY = cy + dy * magnificationInverse;

        if (amount == 1.0f) {
            out[0] = (float) transformedX;
            out[1] = (float) transformedY;
        } else {
            out[0] = (float) (x + amount * (transformedX - x));
            out[1] = (float) (y + amount * (transformedY - y));
        }
    }
}
