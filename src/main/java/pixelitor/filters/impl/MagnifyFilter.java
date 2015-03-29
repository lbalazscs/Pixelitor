/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import com.jhlabs.image.TransformFilter;
import net.jafama.FastMath;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 *
 */
public class MagnifyFilter extends TransformFilter {
    private float centerX;
    private float centerY;
    private float magnification;
    private boolean coupledRadius;
    private float innerRadiusX;
    private float innerRadiusY;
    private float innerRadius2;
    private float innerRadiusX2;
    private float innerRadiusY2;

    private float outerRadiusX;
    private float outerRadiusY;
    private float outerRadius2;
    private float outerRadiusX2;
    private float outerRadiusY2;

    private float yRadiusDifference;

    private float radiusRatio;

    private int cx;
    private int cy;

    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    public void setInnerRadiusX(float radius) {
        this.innerRadiusX = radius;
    }

    public void setInnerRadiusY(float radius) {
        this.innerRadiusY = radius;
    }

    public void setOuterRadiusX(float radius) {
        this.outerRadiusX = radius;
    }

    public void setOuterRadiusY(float radius) {
        this.outerRadiusY = radius;
    }


    public void setMagnification(float magnification) {
        this.magnification = magnification;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        cx = (int) (centerX * src.getWidth());
        cy = (int) (centerY * src.getHeight());

        radiusRatio = 1 / magnification;

        coupledRadius = innerRadiusX == innerRadiusY;
        if(coupledRadius) {
            innerRadius2 = innerRadiusX * innerRadiusX;

            outerRadius2 = outerRadiusX * outerRadiusX;
        } else {
            innerRadiusX2 = innerRadiusX * innerRadiusX;
            innerRadiusY2 = innerRadiusY * innerRadiusY;

            outerRadiusX2 = outerRadiusX * outerRadiusX;
            outerRadiusY2 = outerRadiusY * outerRadiusY;
        }

        yRadiusDifference = outerRadiusY - innerRadiusY;

        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        int dx = x - cx;
        int dy = y - cy;
        int d2 = dx * dx + dy * dy;

        if(coupledRadius) { // circle
            if (d2 > outerRadius2) { // outside
                out[0] = x;
                out[1] = y;
                return;
            } else if(d2 < innerRadius2) { // innermost region
                out[0] = radiusRatio * x + (1 - radiusRatio) * cx;
                out[1] = radiusRatio * y + (1 - radiusRatio) * cy;
                return;
            } else { // between the inner and outer radius
                double distance = Math.sqrt(d2);
                double ratio = (distance - innerRadiusY) / yRadiusDifference; // gives a value between 0 and 1

//                double trigRatio = (FastMath.cos(ratio * Math.PI) + 1.0) / 2.0;
                // 1- smooth step is faster than cosine interpolation
                // http://en.wikipedia.org/wiki/Smoothstep
                // http://www.wolframalpha.com/input/?i=Plot[{%281+%2B+Cos[a+*+Pi]%29%2F2%2C+1+-+3+*+a+*+a+%2B+2+*+a+*+a+*a}%2C+{a%2C+0%2C+1}]
                double trigRatio = 1 + ratio * ratio * (2 * ratio - 3);

                float simpleX = radiusRatio * x + (1 - radiusRatio) * cx;
                float simpleY = radiusRatio * y + (1 - radiusRatio) * cy;

                out[0] = ImageMath.lerp((float)trigRatio, (float)x, simpleX);
                out[1] = ImageMath.lerp((float)trigRatio, (float)y, simpleY);
            }
        } else { // ellipsis
            float dx2 = dx * dx;
            float dy2 = dy * dy;

            if(dy2 >= (outerRadiusY2 - (outerRadiusY2 * dx2) / outerRadiusX2)) {  // outside
                out[0] = x;
                out[1] = y;
                return;
            } if(dy2 <= (innerRadiusY2 - (innerRadiusY2 * dx2) / innerRadiusX2)) { // innermost region
                out[0] = radiusRatio * x + (1 - radiusRatio) * cx;
                out[1] = radiusRatio * y + (1 - radiusRatio) * cy;
            } else { // between the inner and outer radius
                // we are on an ellipse with unknown a and b semi major/minor axes
                // but we know that a/b = outerRadiusX/outerRadiusY
                double ellipseDistortion = outerRadiusX/outerRadiusY;
                double b = Math.sqrt(ellipseDistortion * ellipseDistortion * dy2 + dx2)/ellipseDistortion;
                // now we can calculate how far away we are between the two ellipses
                double ratio = (b - innerRadiusY) / yRadiusDifference; // gives a value between 0 and 1
                double trigRatio = (FastMath.cos(ratio * Math.PI) + 1.0) / 2.0;

                float simpleX = radiusRatio * x + (1 - radiusRatio) * cx;
                float simpleY = radiusRatio * y + (1 - radiusRatio) * cy;

                out[0] = ImageMath.lerp((float)trigRatio, (float)x, simpleX);
                out[1] = ImageMath.lerp((float)trigRatio, (float)y, simpleY);
            }
        }
    }

    public Shape[] getAffectedAreaShapes() {
        Shape inner = new Ellipse2D.Float(cx - innerRadiusX, cy - innerRadiusY, 2 * innerRadiusX, 2 * innerRadiusY);
        Shape outer = new Ellipse2D.Float(cx - outerRadiusX, cy - outerRadiusY, 2 * outerRadiusX, 2 * outerRadiusY);
        return new Shape[] {inner, outer};
    }

}
