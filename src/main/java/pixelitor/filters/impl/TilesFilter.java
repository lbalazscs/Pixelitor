/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.GlassTiles;

import static net.jafama.FastMath.tan;

/**
 * The implementation of the {@link GlassTiles} filter.
 *
 * Inspired by the Paint.net tile effect
 */
public class TilesFilter extends RotatedEffectFilter {
    private float sizeX;
    private float sizeY;
    private float curvatureX;
    private float curvatureY;
    private float shiftX;
    private float shiftY;

    public TilesFilter(String filterName) {
        super(filterName);
    }

    public void setSizeX(int size) {
        sizeX = (float) (Math.PI / size);
    }

    public void setSizeY(int size) {
        sizeY = (float) (Math.PI / size);
    }

    public void setCurvatureX(float curvature) {
        curvatureX = curvature * curvature / 10.0f;
    }

    public void setCurvatureY(float curvature) {
        curvatureY = curvature * curvature / 10.0f;
    }

    @Override
    protected double transformX(double ii, double jj) {
        return ii + curvatureX * tan(ii * sizeX - shiftX / (double) sizeX);
    }

    @Override
    protected double transformY(double ii, double jj) {
        return jj + curvatureY * tan(jj * sizeY - shiftY / (double) sizeY);
    }

    public void setShiftX(float shiftX) {
        this.shiftX = shiftX;
    }

    public void setShiftY(float shiftY) {
        this.shiftY = shiftY;
    }
}
