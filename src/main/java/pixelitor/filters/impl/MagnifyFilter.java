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

import com.jhlabs.image.ImageMath;
import pixelitor.filters.Magnify;
import pixelitor.utils.BlurredShape;

import java.awt.geom.Point2D;

/**
 * The implementation of the {@link Magnify} filter.
 */
public class MagnifyFilter extends CenteredTransformFilter {
    private final double radiusRatio;
    private final BlurredShape shape;
    private final boolean invert;

    /**
     * Constructs a MagnifyFilter with specific magnification and shape parameters.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param center        the effect's center (in pixels).
     * @param magnification the magnification factor (percentage).
     * @param innerRadiusX  the inner horizontal radius where the magnification is at full strength.
     * @param innerRadiusY  the inner vertical radius where the magnification is at full strength.
     * @param outerRadiusX  the outer horizontal radius where the effect fades out.
     * @param outerRadiusY  the outer vertical radius where the effect fades out.
     * @param shapeType     the type of the shape.
     * @param invert        if true, applies the magnification effect to the area outside the shape instead of inside.
     */
    public MagnifyFilter(String filterName, int edgeAction, int interpolation, Point2D center,
                         double magnification, float innerRadiusX, float innerRadiusY,
                         float outerRadiusX, float outerRadiusY, int shapeType, boolean invert) {
        super(filterName, edgeAction, interpolation, center);

        this.invert = invert;

        this.radiusRatio = 1.0 / magnification;
        this.shape = BlurredShape.create(shapeType, center,
            innerRadiusX, innerRadiusY, outerRadiusX, outerRadiusY);
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
