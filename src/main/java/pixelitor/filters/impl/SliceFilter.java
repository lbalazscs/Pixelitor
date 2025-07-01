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

import pixelitor.filters.Slice;

/**
 * The implementation of the {@link Slice} filter.
 */
public class SliceFilter extends RotatingEffectFilter {
    private double horizontalShift;
    private double verticalShift;
    private int offset;
    private int size;

    public SliceFilter(String filterName) {
        super(filterName);
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

    public void setHorizontalShift(double t) {
        horizontalShift = t * size;
    }

    public void setVerticalShift(double t) {
        verticalShift = t * size;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
