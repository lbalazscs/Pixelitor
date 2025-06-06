/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.image;

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pixelitor.filters.impl.RotatingEffectFilter;

/**
 * A filter which distorts an image by rippling it in the X or Y directions.
 * The amplitude and wavelength of rippling can be specified as well as whether
 * pixels going off the edges are wrapped or not.
 */
public class RippleFilter extends RotatingEffectFilter {
    private float xAmplitude, yAmplitude;
    private float xWavelength, yWavelength;
    private int waveType;

    private double phaseX;
    private double phaseY;

    /**
     * Construct a RippleFilter.
     */
    public RippleFilter(String filterName) {
        super(filterName);

        xAmplitude = 5.0f;
        yAmplitude = 0.0f;
        xWavelength = yWavelength = 16.0f;
    }

    /**
     * Set the amplitude of ripple in the X direction.
     *
     * @param xAmplitude the amplitude (in pixels).
     */
    public void setXAmplitude(float xAmplitude) {
        this.xAmplitude = xAmplitude;
    }

    /**
     * Set the wavelength of ripple in the X direction.
     *
     * @param xWavelength the wavelength (in pixels).
     */
    public void setXWavelength(float xWavelength) {
        this.xWavelength = xWavelength;
    }

    /**
     * Set the amplitude of ripple in the Y direction.
     *
     * @param yAmplitude the amplitude (in pixels).
     */
    public void setYAmplitude(float yAmplitude) {
        this.yAmplitude = yAmplitude;
    }

    /**
     * Set the wavelength of ripple in the Y direction.
     *
     * @param yWavelength the wavelength (in pixels).
     */
    public void setYWavelength(float yWavelength) {
        this.yWavelength = yWavelength;
    }

    /**
     * Set the wave type.
     *
     * @param waveType the type.
     */
    public void setWaveType(int waveType) {
        this.waveType = waveType;
    }

    public void setPhaseX(double phaseX) {
        this.phaseX = phaseX * Math.PI * 2;
    }

    public void setPhaseY(double phaseY) {
        this.phaseY = phaseY * Math.PI * 2;
    }

    @Override
    protected void coreTransformInverse(double x, double y, double[] out) {
        double nx = y / xWavelength;
        double ny = x / yWavelength;
        double fx, fy;
        switch (waveType) {
            case WaveType.SINE:
            default:
                fx = (float) FastMath.sin(nx - phaseY);
                fy = (float) FastMath.sin(ny - phaseX);
                break;
            case WaveType.SAWTOOTH:
                fx = ImageMath.sinLikeSawtooth(nx - phaseY);
                fy = ImageMath.sinLikeSawtooth(ny - phaseX);
                break;
            case WaveType.TRIANGLE:
                fx = ImageMath.sinLikeTriangle(nx - phaseY);
                fy = ImageMath.sinLikeTriangle(ny - phaseX);
                break;
            case WaveType.NOISE:
                fx = Noise.sinLikeNoise1((float) (nx - phaseY));
                fy = Noise.sinLikeNoise1((float) (ny - phaseX));
                break;
        }
        out[0] = x + xAmplitude * fx;
        out[1] = y + yAmplitude * fy;
    }

    @Override
    public String toString() {
        return "Distort/Ripple...";
    }
}
