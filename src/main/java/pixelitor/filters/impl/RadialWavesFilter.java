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
import net.jafama.FastMath;
import pixelitor.filters.RadialWaves;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * The implementation of the {@link RadialWaves} filter.
 * <p>
 * Radial waves in a polar coordinate system.
 */
public class RadialWavesFilter extends CenteredTransformFilter {
    private final int angularDivision;
    private final double radialAmplitude;
    private final double phase;
    private final double zoom;
    private final int waveType;

    private int maxSize;

    /**
     * Constructs a new RadialWavesFilter.
     *
     * @param filterName      the name of the filter
     * @param edgeAction      the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT)
     * @param interpolation   the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC)
     * @param center          the effect's center (in pixels)
     * @param angularDivision the number of wave cycles around the center
     * @param radialAmplitude the strength or amount of the wave displacement
     * @param phase           the phase shift of the wave (can be used for animation/time)
     * @param zoom            the zoom level percentage
     * @param waveType        the type of wave to apply
     */
    public RadialWavesFilter(String filterName, int edgeAction, int interpolation, Point2D center,
                             int angularDivision, double radialAmplitude, double phase,
                             double zoom, int waveType) {
        super(filterName, edgeAction, interpolation, center);

        this.angularDivision = angularDivision;
        this.radialAmplitude = radialAmplitude;
        this.phase = phase;
        this.zoom = zoom;
        this.waveType = waveType;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        maxSize = Math.max(src.getWidth(), src.getHeight());
        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        // translate to the center of the effect
        double dx = x - cx;
        double dy = y - cy;

        // convert to polar coordinates
        double r = FastMath.hypot(dx, dy);
        double angle = FastMath.atan2(dy, dx);

        // calculate the wave effect and apply it to the radius
        double waveInput = angle * angularDivision - phase;
        double waveValue = WaveType.wave(waveInput, waveType);
        r += waveValue * radialAmplitude * r / maxSize;

        // apply zoom and convert back to cartesian coordinates
        double zoomedR = r / zoom;
        double u = zoomedR * FastMath.cos(angle);
        double v = zoomedR * FastMath.sin(angle);

        // translate back to the image's coordinate system
        out[0] = (float) (u + cx);
        out[1] = (float) (v + cy);
    }
}
