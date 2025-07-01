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

import pixelitor.filters.GlassTiles;

import static net.jafama.FastMath.tan;

/**
 * The implementation of the {@link GlassTiles} filter.
 *
 * Inspired by the Paint.net tile effect
 */
public class TilesFilter extends RotatingEffectFilter {
    private double sizeX;
    private double sizeY;
    private double curvatureX;
    private double curvatureY;
    private double phaseAngleX;
    private double phaseAngleY;

    public TilesFilter(String filterName) {
        super(filterName);
    }

    @Override
    protected void coreTransformInverse(double x, double y, double[] out) {
        out[0] = x + curvatureX * tan(x * sizeX - phaseAngleX);
        out[1] = y + curvatureY * tan(y * sizeY - phaseAngleY);
    }

    public void setSizeX(double size, double shiftX) {
        // for some reason the effect looks nice only
        // with the reduced double => float precision
        sizeX = (float) (Math.PI / size);

        phaseAngleX = shiftX / sizeX;
    }

    public void setSizeY(double size, double shiftY) {
        sizeY = (float) (Math.PI / size);

        phaseAngleY = shiftY / sizeY;
    }

    public void setCurvatureX(double curvature) {
        curvatureX = curvature * curvature / 10.0;
    }

    public void setCurvatureY(double curvature) {
        curvatureY = curvature * curvature / 10.0;
    }
}
