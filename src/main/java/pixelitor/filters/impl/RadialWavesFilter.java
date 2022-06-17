/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import java.awt.image.BufferedImage;

/**
 * The implementation of the {@link RadialWaves} filter.
 *
 * Radial waves in a polar coordinate system
 */
public class RadialWavesFilter extends CenteredTransformFilter {
    private int angularDivision;
    private double radialAmplitude;
    private double phase;
    private double zoom;
    private int waveType;
    private int maxSize;

    public RadialWavesFilter() {
        super(RadialWaves.NAME);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        maxSize = Math.max(src.getWidth(), src.getHeight());
        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double dx = x - cx;
        double dy = y - cy;
        double r = Math.sqrt(dx * dx + dy * dy);
        double angle = FastMath.atan2(dy, dx);

//        double angularWL = 1.0 / angularDivision;
        double nr = angle * angularDivision - phase;

        double fr = WaveType.wave(nr, waveType);

        r += fr * radialAmplitude * r / maxSize;

        double zoomedR = r / zoom;
        double u = zoomedR * FastMath.cos(angle);
        double v = zoomedR * FastMath.sin(angle);

        out[0] = (float) (u + cx);
        out[1] = (float) (v + cy);
    }

    public void setPhase(double phase) {
        this.phase = phase;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public void setWaveType(int waveType) {
        this.waveType = waveType;
    }

    public void setAngularDivision(int angularDivision) {
        this.angularDivision = angularDivision;
    }

    public void setRadialAmplitude(double radialAmplitude) {
        this.radialAmplitude = radialAmplitude;
    }
}
