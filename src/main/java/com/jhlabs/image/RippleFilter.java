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
    private final float xAmplitude;
    private final float yAmplitude;
    private final double invXWavelength;
    private final double invYWavelength;
    private final int waveType;
    private final double phaseX;
    private final double phaseY;

    /**
     * Constructs a {@link RippleFilter}.
     *
     * @param filterName    the filter name
     * @param edgeAction    how pixels outside the image are handled
     * @param interpolation the interpolation method
     * @param xAmplitude    the amplitude of ripple in the X direction (in pixels)
     * @param xWavelength   the wavelength of ripple in the X direction (in pixels)
     * @param yAmplitude    the amplitude of ripple in the Y direction (in pixels)
     * @param yWavelength   the wavelength of ripple in the Y direction (in pixels)
     * @param waveType      the wave type
     * @param phaseX        the phase of the ripple in the X direction (as a fraction, where 1.0 = full cycle)
     * @param phaseY        the phase of the ripple in the Y direction (as a fraction, where 1.0 = full cycle)
     */
    public RippleFilter(String filterName, int edgeAction, int interpolation,
                        float xAmplitude, float xWavelength,
                        float yAmplitude, float yWavelength,
                        int waveType, double phaseX, double phaseY) {
        super(filterName, edgeAction, interpolation);

        this.xAmplitude = xAmplitude;
        this.yAmplitude = yAmplitude;
        this.invXWavelength = 1.0 / xWavelength;
        this.invYWavelength = 1.0 / yWavelength;
        this.waveType = waveType;
        this.phaseX = phaseX * Math.PI * 2;
        this.phaseY = phaseY * Math.PI * 2;
    }

    @Override
    protected void coreTransformInverse(double x, double y, double[] out) {
        double nx = y * invXWavelength - phaseY;
        double ny = x * invYWavelength - phaseX;

        double fx, fy;
        switch (waveType) {
            case WaveType.SINE -> {
                fx = FastMath.sin(nx);
                fy = FastMath.sin(ny);
            }
            case WaveType.SAWTOOTH -> {
                fx = ImageMath.sinLikeSawtooth(nx);
                fy = ImageMath.sinLikeSawtooth(ny);
            }
            case WaveType.TRIANGLE -> {
                fx = ImageMath.sinLikeTriangle(nx);
                fy = ImageMath.sinLikeTriangle(ny);
            }
            case WaveType.NOISE -> {
                fx = Noise.sinLikeNoise1((float) nx);
                fy = Noise.sinLikeNoise1((float) ny);
            }
            default -> throw new IllegalStateException("waveType = " + waveType);
        }

        out[0] = x + xAmplitude * fx;
        out[1] = y + yAmplitude * fy;
    }
}
