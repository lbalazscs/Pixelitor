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

import com.jhlabs.image.ImageMath;
import pixelitor.filters.Magnify;
import pixelitor.utils.BlurredShape;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * The implementation of the {@link Magnify} filter.
 */
public class MagnifyFilter extends CenteredTransformFilter {
    private double magnification;

    private float innerRadiusX;
    private float innerRadiusY;
    private float outerRadiusX;
    private float outerRadiusY;

    private double radiusRatio;

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

    public void setMagnification(double magnification) {
        this.magnification = magnification;
    }

    // must be called after the radius arguments!
    public void setShape(int type) {
        shape = BlurredShape.create(type, new Point2D.Double(cx, cy),
            innerRadiusX, innerRadiusY, outerRadiusX, outerRadiusY);
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
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

        if (outside == 1.0) { // 100% outside
            out[0] = x;
            out[1] = y;
        } else if (outside == 0.0) { // 100% inside
            out[0] = (float) (radiusRatio * x + (1 - radiusRatio) * cx);
            out[1] = (float) (radiusRatio * y + (1 - radiusRatio) * cy);
        } else { // transition between the inner and outer radius
            double movedX = radiusRatio * x + (1 - radiusRatio) * cx;
            double movedY = radiusRatio * y + (1 - radiusRatio) * cy;

            out[0] = (float) ImageMath.lerp(outside, movedX, x);
            out[1] = (float) ImageMath.lerp(outside, movedY, y);
        }
    }
}
