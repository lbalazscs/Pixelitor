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

import com.jhlabs.image.TransformFilter;

import java.awt.geom.Point2D;

/**
 * Abstract superclass for transform filters with a center
 */
public abstract class CenteredTransformFilter extends TransformFilter {
    // center coordinates in pixels
    protected final double cx;
    protected final double cy;

    /**
     * Constructs a CenteredTransformFilter.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param center        the effect's center (in pixels).
     */
    protected CenteredTransformFilter(String filterName, int edgeAction, int interpolation, Point2D center) {
        super(filterName, edgeAction, interpolation);

        cx = center.getX();
        cy = center.getY();
    }
}
