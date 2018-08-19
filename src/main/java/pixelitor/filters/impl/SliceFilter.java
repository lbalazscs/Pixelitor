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
    protected double transformX(double ii, double jj) {
        return (float) (ii + calcShift(jj, shiftVertical));
    }

    @Override
    protected double transformY(double ii, double jj) {
        return (jj + calcShift(ii, shiftHorizontal));
    }

    private double calcShift(double coord, double shift) {
//        return offset * Math.signum(FastMath.cos(coord / (double) size - shift));
        double mod = ImageMath.mod(coord + shift, 2 * size) - size;
        if (mod >= 0) {
            return offset;
        }
        return -offset;
    }

    public void setShiftHorizontal(double t) {
        this.shiftHorizontal = t * 2 * Math.PI;
    }

    public void setShiftVertical(double t) {
        this.shiftVertical = t * 2 * Math.PI;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setSize(int size) {
        this.size = size;
    }

//    public void setAngle(double angle) {
//        this.angle = angle;
//        sin = Math.sin(angle);
//        cos = Math.cos(angle);
//    }
}
