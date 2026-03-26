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
 * This filter applies a marbling effect to an image, displacing pixels by random amounts.
 */
public class MarbleFilter extends TransformFilter {
    private static final int TABLE_SIZE = 256;
    private final float[] sinTable;
    private final float[] cosTable;
    private final float scale;
    private final float time;

    /**
     * Constructs a MarbleFilter.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy.
     * @param interpolation the interpolation method.
     * @param scale         the X scale of the effect.
     * @param amount        the amount of the effect.
     * @param turbulence    the turbulence of the effect (in the range [0, 1]).
     * @param time          the time of the effect.
     */
    public MarbleFilter(String filterName, int edgeAction, int interpolation, float scale, float amount, float turbulence, float time) {
        super(filterName, edgeAction, interpolation);

        this.scale = scale;
        this.time = time;

        sinTable = new float[TABLE_SIZE];
        cosTable = new float[TABLE_SIZE];

        float angleMultiplier = ImageMath.TWO_PI * turbulence / TABLE_SIZE;
        for (int i = 0; i < TABLE_SIZE; i++) {
            float angle = i * angleMultiplier;

            sinTable[i] = (float) (-amount * Math.sin(angle));
            cosTable[i] = (float) (amount * Math.cos(angle));
        }

        // balance the arrays around 0 so that the pixels
        // are not shifted predominantly in one direction
        float sinAverage = average(sinTable);
        float cosAverage = average(cosTable);
        for (int i = 0; i < TABLE_SIZE; i++) {
            sinTable[i] -= sinAverage;
            cosTable[i] -= cosAverage;
        }
    }

    private static float average(float[] input) {
        float sum = 0.0f;
        for (float v : input) {
            sum += v;
        }
        return sum / input.length;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float noise = Noise.noise3(x / scale, y / scale, time); // mostly between -1 and 1 but not distributed uniformly
        int displacement = PixelUtils.clamp((int) (127.0f * (1.0f + noise)));

        out[0] = x + sinTable[displacement];
        out[1] = y + cosTable[displacement];
    }
}
