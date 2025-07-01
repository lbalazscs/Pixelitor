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

import com.jhlabs.image.WaveType;
import net.jafama.DoubleWrapper;
import net.jafama.FastMath;
import pixelitor.filters.AngularWaves;

/**
 * The implementation of the {@link AngularWaves} filter.
 * Angular waves in a polar coordinate system
 */
public class AngularWavesFilter extends CenteredTransformFilter {
    private double radialWavelength;
    private double phase;
    private double zoom;
    private double amount;
    private int waveType;

    public AngularWavesFilter() {
        super(AngularWaves.NAME);
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

    public void setRadialWavelength(double radialWavelength) {
        this.radialWavelength = radialWavelength;
    }

    public void setPhase(double phase) {
        this.phase = phase;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setWaveType(int waveType) {
        this.waveType = waveType;
    }
}
