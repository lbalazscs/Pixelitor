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

import java.awt.image.BufferedImage;

public class DrosteFilter extends TransformFilter {
    Complex xBounds, yBounds;
    float r1, r2, p1, p2;
    boolean tileBasedOnTransparency;

    public DrosteFilter(String filterName) {
        super(filterName);
    }

    void evaluateDependents() {
        tileBasedOnTransparency = false; // TODO
    }

    public void setRadiusInside(float radiusInside) {
        this.r1 = radiusInside;
    }

    public void setRadiusOutside(float radiusOutside) {
        this.r2 = radiusOutside;
    }

    public void setPeriodicity(float periodicity) {
        this.p1 = periodicity;
        if (p1 == 0.0f) {
            p1 = 0.001f; // Prevent divide by zero
        }
    }

    public void setStrands(float strands) {
        this.p2 = strands;
        if (p2 == 0.0f) {
            p2 = 0.0001f;
        }
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {

    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        evaluateDependents();
        return super.filter(src, dst);
    }

    static class Complex {
        float re;
        float im;
    }
}
