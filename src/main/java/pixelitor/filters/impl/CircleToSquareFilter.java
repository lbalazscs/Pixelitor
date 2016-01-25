/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Distorts a circle into a square
 */
public class CircleToSquareFilter extends CenteredTransformFilter {
    private float radiusX = 500;
    private float radiusY = 500;
    private float radiusRatio;

    private float amount = 1.0f;

    public CircleToSquareFilter() {
        super(CircleToSquare.NAME);
    }

    public void setRadiusX(float radius) {
        this.radiusX = radius;
    }

    public void setRadiusY(float radius) {
        this.radiusY = radius;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        radiusRatio = radiusX / radiusY;
        return super.filter(src, dst);

    }

    public Shape[] getAffectedAreaShapes() {
        Shape rect = new Rectangle2D.Float(cx - radiusX, cy - radiusY, 2 * radiusX, 2* radiusY);
        Shape ellipse = new Ellipse2D.Float(cx - radiusX, cy - radiusY, 2 * radiusX, 2* radiusY);
        return new Shape[] {rect, ellipse};
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - cx;
        float dy = y - cy;
        float xDist = Math.abs(dx);

        float yDist = Math.abs(dy);
        if ((xDist > radiusX) || (yDist > radiusY)) { // out of the affected area
            out[0] = x;
            out[1] = y;
            return;
        }

        double angle;
        if (xDist >= yDist) { // we want to move from a vertical line  to the circle
            angle = FastMath.atan2(dy, xDist);
        } else { // move from horizontal line
            angle = FastMath.atan2(dx, yDist);
        }

        double magnificationInverse = FastMath.cos(angle);

        // dividing by radiusRatio transforms the circle-to-square transformation
        // into an ellipse-to-rectangle transformation
        float transformedX = cx + (float) (dx * magnificationInverse / radiusRatio);
        float transformedY = cy + (float) (dy * magnificationInverse);

        if (amount == 1.0f) {
            out[0] = transformedX;
            out[1] = transformedY;
        } else {
            out[0] = x + amount * (transformedX - x);
            out[1] = y + amount * (transformedY - y);
        }
    }
}
