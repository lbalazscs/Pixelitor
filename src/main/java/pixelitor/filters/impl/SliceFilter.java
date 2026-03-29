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
import pixelitor.filters.Slice;

/**
 * The implementation of the {@link Slice} filter.
 */
public class SliceFilter extends RotatingEffectFilter {
    private final double horizontalShift;
    private final double verticalShift;
    private final int offset;
    private final int size;

    /**
     * Constructs a new SliceFilter with the specified parameters.
     *
     * @param filterName         the name of the filter.
     * @param edgeAction         the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation      the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param angle              the effect's rotation angle (in radians).
     * @param offset             the distance pixels are shifted within a slice.
     * @param size               the thickness of each slice in pixels.
     * @param horizontalShiftPct the horizontal shift as a percentage (0.0 to 1.0) of the slice size.
     * @param verticalShiftPct   the vertical shift as a percentage (0.0 to 1.0) of the slice size.
     */
    public SliceFilter(String filterName, int edgeAction, double angle,
                       int offset, int size,
                       double horizontalShiftPct, double verticalShiftPct) {
        super(filterName, edgeAction, TransformFilter.NEAREST_NEIGHBOR, angle);

        this.offset = offset;
        this.size = size;
        this.horizontalShift = horizontalShiftPct * size;
        this.verticalShift = verticalShiftPct * size;
    }

    @Override
    protected void coreTransformInverse(double x, double y, double[] out) {
        out[0] = x + calcDisplacement(y, verticalShift);
        out[1] = y + calcDisplacement(x, horizontalShift);
    }

    private double calcDisplacement(double coord, double phase) {
        // determine which band the coordinate falls into
        int bandIndex = (int) Math.floor((coord - phase) / size);
        // alternate the offset for odd and even bands to create the slice effect
        return (bandIndex & 1) == 1 ? offset : -offset;
    }
}
