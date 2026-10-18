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

/**
 * A filter which produces a simulated wood texture.
 */
public class WoodFilter extends PointFilter {
    private final float invScale;
    private final float rings;
    private final float turbulence;
    private final float fibers;
    private final float gain;
    private final float m00;
    private final float m01;
    private final float m10;
    private final float m11;
    private final Colormap colormap;

    /**
     * Constructs a WoodFilter with specified properties.
     *
     * @param filterName the name of the filter
     * @param rings      the rings value (in the range [0, 1])
     * @param scale      the scale of the texture
     * @param stretch    the stretch factor of the texture
     * @param angle      the angle of the texture
     * @param turbulence the turbulence of the texture (in the range [0, 1])
     * @param fibers     the amount of fibers in the texture (in the range [0, 1])
     * @param gain       the gain of the texture (in the range [0, 1])
     * @param colormap   the colormap to be used for the filter
     */
    public WoodFilter(String filterName, float rings, float scale, float stretch, float angle,
                      float turbulence, float fibers, float gain, Colormap colormap) {
        super(filterName);
        this.rings = rings * 50.0f;
        this.invScale = 1.0f / scale;

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        this.m00 = cos;
        this.m01 = sin;

        // x-axis scaling can't be incorporated into m00 and m01, but
        // y-scaling and stretching can be incorporated into m10 and m11
        float invScaleStretch = 1.0f / (scale * stretch);
        this.m10 = -sin * invScaleStretch;
        this.m11 = cos * invScaleStretch;

        this.turbulence = turbulence * 0.1f;
        this.fibers = fibers;
        this.gain = gain;
        this.colormap = colormap;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        float projX = m00 * x + m01 * y;
        float nx = projX * invScale;
        float ny = m10 * x + m11 * y;

        float f = Noise.noise2(nx, ny);
        if (turbulence != 0.0f) {
            f += turbulence * Noise.noise2(nx * 0.05f, ny * 20.0f);
        }
        f = (f * 0.5f) + 0.5f;

        f *= rings;
        f -= (int) f;
        f *= 1.0f - ImageMath.smoothStep(gain, 1.0f, f);

        if (fibers != 0.0f) {
            f += fibers * Noise.noise2(projX, ny * 50.0f);
        }

        return colormap.getColor(f);
    }
}
