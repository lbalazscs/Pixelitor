/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
public class SliceFilter extends RotatedEffectFilter {
    private double shiftHorizontal;
    private double shiftVertical;
    private int offset;
    private int size;

    public SliceFilter(String filterName) {
        super(filterName);
    }

    @Override
    protected void transformInverseUnRotated(double x, double y, double[] out) {
        out[0] = x + calcShift(y, shiftVertical);
        out[1] = y + calcShift(x, shiftHorizontal);
    }

    private double calcShift(double coord, double shift) {
        double mod = ImageMath.mod(coord - shift, 2 * size) - size;
        if (mod >= 0) {
            return offset;
        }
        return -offset;
    }

    public void setShiftHorizontal(double t) {
        shiftHorizontal = t * size;
    }

    public void setShiftVertical(double t) {
        shiftVertical = t * size;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
