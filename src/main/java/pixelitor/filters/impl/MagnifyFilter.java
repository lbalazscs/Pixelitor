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
package pixelitor.filters.impl;

import com.jhlabs.image.ImageMath;
import pixelitor.filters.Magnify;
import pixelitor.utils.BlurredShape;
import pixelitor.utils.Shapes;

import java.awt.Shape;
import java.awt.geom.Point2D;
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

    private BlurredShape shape;
    private boolean invert;

    public MagnifyFilter(String filterName) {
        super(filterName);
    }

    public void setInnerRadiusX(float radius) {
        innerRadiusX = radius;
    }

    public void setInnerRadiusY(float radius) {
        innerRadiusY = radius;
    }

    public void setOuterRadiusX(float radius) {
        outerRadiusX = radius;
    }

    public void setOuterRadiusY(float radius) {
        outerRadiusY = radius;
    }

    public void setMagnification(float magnification) {
        this.magnification = magnification;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        radiusRatio = 1 / magnification;

        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double outside = shape.isOutside(x, y);
        if (invert) {
            outside = 1.0 - outside;
        }
        
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
        Shape inner = Shapes.createEllipse(cx, cy, innerRadiusX, innerRadiusY);
        Shape outer = Shapes.createEllipse(cx, cy, outerRadiusX, outerRadiusY);
        return new Shape[]{inner, outer};
    }

    // must be called after the shape arguments!
    public void setShape(int type) {
        shape = BlurredShape.create(type, new Point2D.Double(cx, cy),
                innerRadiusX, innerRadiusY,
                outerRadiusX, outerRadiusY);
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }
}
