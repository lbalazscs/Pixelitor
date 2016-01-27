/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import net.jafama.FastMath;

public class SliceFilter extends TransformFilter {
    private double shiftHorizontal;
    private double shiftVertical;
    private int offset;
    private int size;

    public SliceFilter(String filterName) {
        super(filterName);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
//        x += offset * Math.signum(ImageMath.sinLikeTriangle(y / (double) size + shift));
//        y += offset * Math.signum(ImageMath.sinLikeTriangle(x / (double)size + shift));

        x += offset * Math.signum(FastMath.cos(y / (double) size - shiftVertical));
        y += offset * Math.signum(FastMath.cos(x / (double) size - shiftHorizontal));


        out[0] = x;
        out[1] = y;
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
}
