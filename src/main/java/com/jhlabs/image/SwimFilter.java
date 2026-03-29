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
 * A filter which distorts an image as if it were underwater.
 */
public class SwimFilter extends TransformFilter {
    private final float scale;
    private final float stretch;
    private final float amount;
    private final float time;
    private final float m00;
    private final float m01;
    private final float m10;
    private final float m11;

    /**
     * Constructs a SwimFilter.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param amount        the amount of swim distortion.
     * @param scale         the scale of the distortion.
     * @param stretch       the stretch factor of the distortion.
     * @param angle         the angle of the effect, in radians. Used to animate the distortion
     *                      direction by computing a rotation matrix.
     * @param time          the time offset; use this to animate the effect.
     */
    public SwimFilter(String filterName, int edgeAction, int interpolation,
                      float amount, float scale, float stretch, float angle, float time) {
        super(filterName, edgeAction, interpolation);

        this.amount = amount;
        this.scale = scale;
        this.stretch = stretch;
        this.time = time;

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        this.m00 = cos;
        this.m01 = sin;
        this.m10 = -sin;
        this.m11 = cos;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float nx = m00 * x + m01 * y;
        float ny = m10 * x + m11 * y;
        nx /= scale;
        ny /= scale * stretch;

        float noise3x = Noise.noise3(nx + 0.5f, ny, time);
        float noise3y = Noise.noise3(nx, ny + 0.5f, time);

        out[0] = x + amount * noise3x;
        out[1] = y + amount * noise3y;
    }
}
