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

import java.util.Random;

import static com.jhlabs.image.ImageMath.PI;
import static com.jhlabs.image.ImageMath.TWO_PI;
import static com.jhlabs.image.ImageMath.clamp01;
import static com.jhlabs.image.ImageMath.lerp;
import static com.jhlabs.image.ImageMath.mixColors;
import static net.jafama.FastMath.atan2;
import static net.jafama.FastMath.powQuick;

public class SparkleFilter extends PointFilter {
    private int numRays = 50;
    private int radius = 25;
    private int amount = 50;
    private int color = 0xFFFFFFFF;
    private int randomness = 25;
    private int centerX;
    private int centerY;
    private float[] rayLengths;
    private Random random;

    private float relativeCenterX = 0.5f;
    private float relativeCenterY = 0.5f;
    private boolean lightOnly;

    private double power;

    public SparkleFilter(String filterName) {
        super(filterName);
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setRandomness(int randomness) {
        this.randomness = randomness;
    }

    /**
     * Sets the amount of sparkle.
     *
     * @param amount the amount (in the range [0, 100])
     */
    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setNumRays(int numRays) {
        this.numRays = numRays;
    }

    /**
     * Sets the radius of the effect.
     *
     * @param radius the radius
     */
    public void setRadius(int radius) {
        this.radius = radius;
    }

    @Override
    public void setDimensions(int width, int height) {
        centerX = (int) (width * relativeCenterX);
        centerY = (int) (height * relativeCenterY);
        super.setDimensions(width, height);

        // make array size numRays + 2 to avoid using the modulo operator in processPixel
        rayLengths = new float[numRays + 2];
        float randomRadius = randomness / 100.0f * radius;
        for (int i = 0; i < numRays; i++) {
            rayLengths[i] = radius + randomRadius * (float) random.nextGaussian();
        }

        rayLengths[numRays] = rayLengths[0];
        rayLengths[numRays + 1] = rayLengths[1];

        power = (100 - amount) / 50.0;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        float dx = x - centerX;
        float dy = y - centerY;
        float distSq = dx * dx + dy * dy;
        float angle = (float) atan2(dy, dx);

        float d = (angle + PI) / TWO_PI * numRays;
        int i = (int) d;

        // safe-guard bounds to protect against edge-case float inaccuracies
        if (i < 0) {
            i = 0;
        } else if (i > numRays) {
            i = numRays;
        }

        float fraction = d - i;
        float length = lerp(fraction, rayLengths[i], rayLengths[i + 1]);
        float intensity = length * length / (distSq + 0.0001f);

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

    public void setRelativeCenterX(float relativeCenterX) {
        this.relativeCenterX = relativeCenterX;
    }

    public void setRelativeCenterY(float relativeCenterY) {
        this.relativeCenterY = relativeCenterY;
    }

    public void setLightOnly(boolean lightOnly) {
        this.lightOnly = lightOnly;
    }

    public void setRandom(Random random) {
        this.random = random;
    }
}
