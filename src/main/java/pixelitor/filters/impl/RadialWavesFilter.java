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
import pixelitor.filters.RadialWaves;

import java.awt.image.BufferedImage;

/**
 * Radial waves in a polar coordinate system
 */
public class RadialWavesFilter extends CenteredTransformFilter {
    private int angularDivision;
    private double radialAmplitude;

    private float phase;
    private float zoom;
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
        float dx = x - cx;
        float dy = y - cy;
        double r = Math.sqrt(dx * dx + dy * dy);
        double angle = FastMath.atan2(dy, dx);

//        double angularWL = 1.0 / angularDivision;
        double nr = angle * angularDivision - phase;
        double fr;

        switch (waveType) {
            case WaveType.SINE:
                fr = FastMath.sin(nr);
                break;
            case WaveType.SAWTOOTH:
                fr = ImageMath.sinLikeSawtooth(nr);
                break;
            case WaveType.TRIANGLE:
                fr = ImageMath.sinLikeTriangle(nr);
                break;
            case WaveType.NOISE:
                fr = Noise.sinLikeNoise1((float)nr);
                break;
            default:
                throw new IllegalStateException("waveType = " + waveType);
        }

        r += fr * radialAmplitude * r / maxSize;

        double zoomedR = r / zoom;
        float u = (float) (zoomedR * FastMath.cos(angle));
        float v = (float) (zoomedR * FastMath.sin(angle));

        out[0] = (u + cx);
        out[1] = (v + cy);
    }

    public void setPhase(float phase) {
        this.phase = phase;
    }

    public void setZoom(float zoom) {
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
