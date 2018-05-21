/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.Magnify;
import pixelitor.utils.BlurredEllipse;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * The implementation of the {@link Magnify} filter.
 */
public class MagnifyFilter extends CenteredTransformFilter {
    private float magnification;

    private float innerRadiusX;
    private float innerRadiusY;
    private float outerRadiusX;
    private float outerRadiusY;

    private float radiusRatio;

    private BlurredEllipse ellipse;

    public MagnifyFilter(String filterName) {
        super(filterName);
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
        // called twice (also in super), but there is no better solution
        calcAbsoluteCenter(src);

        ellipse = new BlurredEllipse(cx, cy, innerRadiusX, innerRadiusY, outerRadiusX, outerRadiusY);
        radiusRatio = 1 / magnification;

        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double outside = ellipse.isOutside(x, y);

        if (outside == 1.0) { // outside
            out[0] = x;
            out[1] = y;
            return;
        } else if (outside == 0.0) { // innermost region
            out[0] = radiusRatio * x + (1 - radiusRatio) * cx;
            out[1] = radiusRatio * y + (1 - radiusRatio) * cy;
            return;
        } else { // between the inner and outer radius
            double simpleX = radiusRatio * x + (1 - radiusRatio) * cx;
            double simpleY = radiusRatio * y + (1 - radiusRatio) * cy;

            out[0] = (float) ImageMath.lerp(outside, simpleX, x);
            out[1] = (float) ImageMath.lerp(outside, simpleY, y);
        }
    }

    public Shape[] getAffectedAreaShapes() {
        Shape inner = new Ellipse2D.Double(cx - innerRadiusX, cy - innerRadiusY, 2 * innerRadiusX, 2 * innerRadiusY);
        Shape outer = new Ellipse2D.Double(cx - outerRadiusX, cy - outerRadiusY, 2 * outerRadiusX, 2 * outerRadiusY);
        return new Shape[] {inner, outer};
    }
}
