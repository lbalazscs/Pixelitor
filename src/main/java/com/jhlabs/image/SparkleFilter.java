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

import static com.jhlabs.image.ImageMath.*;
import static net.jafama.FastMath.atan2;
import static net.jafama.FastMath.powQuick;

/**
 * A filter that overlays a sparkle effect on an image: thin light rays
 * radiate from a center point, each with a randomized length, with brightness
 * falling off with distance from the center and tapering within each ray.
 */
public class SparkleFilter extends PointFilter {
    // prevents division by zero for pixels located exactly at the sparkle center (distSq == 0)
    private static final float EPSILON = 0.0001f;

    private static final int MAX_AMOUNT = 100;
    private static final int NEUTRAL_AMOUNT = 50; // amount value where power == 1.0 (pow becomes a no-op)

    private static final double SPIRAL_EFFECT_SCALE = 0.005;

    private final boolean lightOnly;
    private final int numRays;
    private final int amount;
    private final int color;
    private final double spiral;

    private final float cx;
    private final float cy;
    private final float[] rayLengths;
    private final double falloffExponent;

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
     * @param spiral     the amount of spiral twist applied to the rays, in the range [-1, 1]
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
                         double spiral,
                         Random random) {
        super(filterName);

        assert numRays > 0;
        assert radius >= 0;
        assert amount >= 0 && amount <= MAX_AMOUNT;
        assert spiral >= -1.0 && spiral <= 1.0;

        this.lightOnly = lightOnly;
        this.cx = (float) center.getX();
        this.cy = (float) center.getY();
        this.numRays = numRays;
        this.amount = amount;
        this.color = color;
        this.spiral = spiral;

        rayLengths = new float[numRays + 2];
        float randomRadius = randomness / 100.0f * radius;
        for (int i = 0; i < numRays; i++) {
            rayLengths[i] = Math.max(0.0f, radius + randomRadius * (float) random.nextGaussian());
        }
        rayLengths[numRays] = rayLengths[0];
        rayLengths[numRays + 1] = rayLengths[1];

        falloffExponent = (MAX_AMOUNT - amount) / (double) NEUTRAL_AMOUNT;
        rayAngleFactor = numRays / TWO_PI;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        float mixRatio = calcMixRatio(x, y);

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

    private float calcMixRatio(int x, int y) {
        float dx = x - cx;
        float dy = y - cy;
        float distSq = dx * dx + dy * dy;
        float angle = (float) atan2(dy, dx);

        if (spiral != 0.0) {
            double radius = Math.sqrt(distSq);
            double spiralAngleOffset = spiral * radius * SPIRAL_EFFECT_SCALE;
            angle -= (float) spiralAngleOffset;
        }

        // wraps the (possibly spiral-shifted) angle into the [0, numRays) range
        float rayPosition = mod(angle + PI, TWO_PI) * rayAngleFactor;

        int rayIndex = Math.clamp((int) rayPosition, 0, numRays);
        float fraction = rayPosition - rayIndex;
        float rayLength = lerp(fraction, rayLengths[rayIndex], rayLengths[rayIndex + 1]);

        float intensity = rayLength * rayLength / (distSq + EPSILON);
        if (amount != NEUTRAL_AMOUNT) { // if amount = NEUTRAL_AMOUNT then falloffExponent = 1, but safer to compare ints
            intensity = (float) powQuick(intensity, falloffExponent);
        }

        float distFromSegmentMid = fraction - 0.5f;
        float rayWeight = (1.0f - distFromSegmentMid * distFromSegmentMid) * intensity;

        return clamp01(rayWeight);
    }
}
