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
import pixelitor.filters.AngularWaves;

/**
 * The implementation of the {@link AngularWaves} filter.
 * Angular waves in a polar coordinate system
 */
public class AngularWavesFilter extends CenteredTransformFilter {
    private double radialWL; // Radial Wavelength
    private double phase;
    private double zoom;
    private double amount;
    private int waveType;

    public AngularWavesFilter() {
        super(AngularWaves.NAME);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double dx = x - cx;
        double dy = y - cy;
        double r = Math.sqrt(dx * dx + dy * dy);
        double angle = FastMath.atan2(dy, dx);

        double na = r / radialWL - phase;

        double fa = WaveType.wave(na, waveType);

        angle += fa * amount;

        double zoomedR = r / zoom;
        double u = zoomedR * FastMath.cos(angle);
        double v = zoomedR * FastMath.sin(angle);

        out[0] = (float) (u + cx);
        out[1] = (float) (v + cy);
    }

    public void setRadialWL(double radialWL) {
        this.radialWL = radialWL;
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
