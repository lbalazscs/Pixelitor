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

import com.jhlabs.image.WaveType;
import net.jafama.DoubleWrapper;
import net.jafama.FastMath;
import pixelitor.filters.AngularWaves;

import java.awt.geom.Point2D;

/**
 * The implementation of the {@link AngularWaves} filter.
 * Angular waves in a polar coordinate system
 */
public class AngularWavesFilter extends CenteredTransformFilter {
    private final double radialWavelength;
    private final double phase;
    private final double zoom;
    private final double amount;
    private final int waveType;

    /**
     * Constructs an {@link AngularWavesFilter}.
     *
     * @param filterName       the name of the filter.
     * @param edgeAction       the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation    the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param center           the effect's center (in pixels).
     * @param radialWavelength the radial wavelength of the waves.
     * @param phase            the phase offset of the waves, which can be animated over time.
     * @param zoom             the zoom factor applied to the output.
     * @param amount           the angular amplitude of the waves, in radians.
     * @param waveType         the shape of the wave.
     */
    public AngularWavesFilter(String filterName, int edgeAction, int interpolation, Point2D center,
                              double radialWavelength, double phase, double zoom,
                              double amount, int waveType) {
        super(filterName, edgeAction, interpolation, center);

        this.radialWavelength = radialWavelength;
        this.phase = phase;
        this.zoom = zoom;
        this.amount = amount;
        this.waveType = waveType;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        // translate to the center of the effect
        double dx = x - cx;
        double dy = y - cy;

        // convert to polar coordinates
        double r = FastMath.hypot(dx, dy);
        double angle = FastMath.atan2(dy, dx);

        double waveInput = r / radialWavelength - phase;
        double waveValue = WaveType.wave(waveInput, waveType);

        // distort the angle with the wave
        angle += waveValue * amount;

        // optimized sin/cos calculation
        DoubleWrapper cosWrapper = new DoubleWrapper(); // must be local variable (multithreading)
        double sin = FastMath.sinAndCos(angle, cosWrapper);
        double cos = cosWrapper.value;

        // apply zoom and convert back to cartesian coordinates
        double zoomedR = r / zoom;
        double u = zoomedR * cos;
        double v = zoomedR * sin;

        // translate back to the image's coordinate system
        out[0] = (float) (u + cx);
        out[1] = (float) (v + cy);
    }
}
