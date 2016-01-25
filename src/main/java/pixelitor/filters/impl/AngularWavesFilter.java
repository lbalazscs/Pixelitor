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

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.WaveType;
import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pixelitor.filters.AngularWaves;

/**
 * Angular waves in a polar coordinate system
 */
public class AngularWavesFilter extends CenteredTransformFilter {
    private double radialWL;

    private float phase;
    private float zoom;
    private float amount;
    private int waveType;

    public AngularWavesFilter() {
        super(AngularWaves.NAME);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - cx;
        float dy = y - cy;
        double r = Math.sqrt(dx * dx + dy * dy);
        double angle = FastMath.atan2(dy, dx);

        double na = r / radialWL - phase;
        double fa;

        switch (waveType) {
            case WaveType.SINE:
                fa = FastMath.sin(na);
                break;
            case WaveType.SAWTOOTH:
                fa = ImageMath.sinLikeSawtooth(na);
                break;
            case WaveType.TRIANGLE:
                fa = ImageMath.sinLikeTriangle(na);
                break;
            case WaveType.NOISE:
                fa = Noise.sinLikeNoise1((float)na);
                break;
            default:
                throw new IllegalStateException("waveType = " + waveType);
        }

        angle += fa * amount;

        double zoomedR = r / zoom;
        float u = (float) (zoomedR * FastMath.cos(angle));
        float v = (float) (zoomedR * FastMath.sin(angle));

        out[0] = (u + cx);
        out[1] = (v + cy);
    }

    public void setRadialWL(double radialWL) {
        this.radialWL = radialWL;
    }

    public void setPhase(float phase) {
        this.phase = phase;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void setWaveType(int waveType) {
        this.waveType = waveType;
    }
}
