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

import java.awt.geom.Point2D;
import java.util.Random;

import static com.jhlabs.image.ImageMath.PI;
import static com.jhlabs.image.ImageMath.TWO_PI;
import static com.jhlabs.image.ImageMath.clamp01;
import static com.jhlabs.image.ImageMath.lerp;
import static com.jhlabs.image.ImageMath.mixColors;
import static net.jafama.FastMath.atan2;
import static net.jafama.FastMath.powQuick;

public class SparkleFilter extends PointFilter {
    private static final float EPSILON = 0.0001f;

    private final boolean lightOnly;
    private final int numRays;
    private final int amount;
    private final int color;

    private final float cx;
    private final float cy;
    private final float[] rayLengths;
    private final double power;

    private final float rayAngleFactor;

    /**
     * Constructs a new SparkleFilter.
     *
     * @param filterName the name of the filter passed to the superclass
     * @param lightOnly  if true, only the light color is added to a transparent background;
     *                   otherwise it mixes with the original image
     * @param center     the coordinates of the effect center (in pixels)
     * @param radius     the radius of the effect
     * @param numRays    the number of sparkle rays
     * @param amount     the amount of sparkle (in the range [0, 100])
     * @param randomness the level of randomness applied to the lengths of the rays
     * @param color      the ARGB color value of the sparkle effect
     * @param random     the random number generator instance to use for drawing rays
     */
    public SparkleFilter(String filterName,
                         boolean lightOnly,
                         Point2D center,
                         int radius,
                         int numRays,
                         int amount,
                         int randomness,
                         int color,
                         Random random) {
        super(filterName);

        this.lightOnly = lightOnly;
        this.cx = (float) center.getX();
        this.cy = (float) center.getY();
        this.numRays = numRays;
        this.amount = amount;
        this.color = color;

        // make array size numRays + 2 to avoid using the modulo operator in processPixel
        rayLengths = new float[numRays + 2];
        float randomRadius = randomness / 100.0f * radius;
        for (int i = 0; i < numRays; i++) {
            rayLengths[i] = radius + randomRadius * (float) random.nextGaussian();
        }

        rayLengths[numRays] = rayLengths[0];
        rayLengths[numRays + 1] = rayLengths[1];

        power = (100 - amount) / 50.0;
        rayAngleFactor = numRays / TWO_PI;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        float dx = x - cx;
        float dy = y - cy;
        float distSq = dx * dx + dy * dy;
        float angle = (float) atan2(dy, dx);

        float d = (angle + PI) * rayAngleFactor;
        int i = (int) d;

        // safe-guard bounds to protect against edge-case float inaccuracies
        i = Math.clamp(i, 0, numRays);

        float fraction = d - i;
        float length = lerp(fraction, rayLengths[i], rayLengths[i + 1]);
        float intensity = length * length / (distSq + EPSILON);

        if (amount != 50) { // if amount = 50 then power = 1, but safer to compare ints
            intensity = (float) powQuick(intensity, power);
        }

        float offset = fraction - 0.5f;
        float rayWeight = (1.0f - offset * offset) * intensity;

        float mixRatio = clamp01(rayWeight);

        if (lightOnly) {
            // this has the effect of mixing with the
            // transparent version (alpha = 0) of the light color
            int origAlpha = color >>> 24;
            int newAlpha = (int) (mixRatio * origAlpha);
            return (newAlpha << 24) | (color & 0x00_FF_FF_FF);
        } else {
            return mixColors(mixRatio, rgb, color);
        }
    }
}
