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
        out[0] = x + calcShift(y, verticalShift);
        out[1] = y + calcShift(x, horizontalShift);
    }

    private double calcShift(double coord, double shift) {
        double mod = ImageMath.mod(coord - shift, 2 * size) - size;
        return mod >= 0 ? offset : -offset;
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
