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

import pixelitor.filters.GlassTiles;

import static net.jafama.FastMath.tan;

/**
 * The implementation of the {@link GlassTiles} filter.
 *
 * Inspired by the Paint.net tile effect
 */
public class TilesFilter extends RotatingEffectFilter {
    private final double freqX;
    private final double freqY;
    private final double curvatureX;
    private final double curvatureY;
    private final double phaseAngleX;
    private final double phaseAngleY;

    /**
     * Constructs a new TilesFilter.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param angle         the rotation angle of the tiles (in radians).
     * @param sizeX         the horizontal size of the tiles.
     * @param shiftX        the horizontal phase shift/movement of the tiles.
     * @param sizeY         the vertical size of the tiles.
     * @param shiftY        the vertical phase shift/movement of the tiles.
     * @param curvatureXVal the horizontal curvature intensity.
     * @param curvatureYVal the vertical curvature intensity.
     */
    public TilesFilter(String filterName, int edgeAction, int interpolation, double angle,
                       double sizeX, double shiftX,
                       double sizeY, double shiftY,
                       double curvatureXVal, double curvatureYVal) {
        super(filterName, edgeAction, interpolation, angle);

        // for some reason the effect looks nice only
        // with the reduced double => float precision
        this.freqX = (float) (Math.PI / sizeX);
        this.freqY = (float) (Math.PI / sizeY);

        this.phaseAngleX = shiftX / this.freqX;
        this.phaseAngleY = shiftY / this.freqY;

        this.curvatureX = (curvatureXVal * curvatureXVal) / 10.0;
        this.curvatureY = (curvatureYVal * curvatureYVal) / 10.0;
    }

    @Override
    protected void coreTransformInverse(double x, double y, double[] out) {
        out[0] = x + curvatureX * tan(x * freqX - phaseAngleX);
        out[1] = y + curvatureY * tan(y * freqY - phaseAngleY);
    }
}
