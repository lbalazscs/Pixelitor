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
    private int color = 0xffffffff;
    private int randomness = 25;
    //    private int width, height;
    private int centerX, centerY;
    //    private long seed = 371;
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
     * Set the amount of sparkle.
     *
     * @param amount the amount
     * @min-value 0
     * @max-value 1
     */
    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setNumRays(int numRays) {
        this.numRays = numRays;
    }

    /**
     * Set the radius of the effect.
     *
     * @param radius the radius
     * @min-value 0
     */
    public void setRadius(int radius) {
        this.radius = radius;
    }

    @Override
    public void setDimensions(int width, int height) {
        centerX = (int) (width * relativeCenterX);
        centerY = (int) (height * relativeCenterY);
        super.setDimensions(width, height);
        rayLengths = new float[numRays];
        for (int i = 0; i < numRays; i++) {
            rayLengths[i] = radius + randomness / 100.0f * radius * (float) random.nextGaussian();
        }
        power = (100 - amount) / 50.0;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        float dx = x - centerX;
        float dy = y - centerY;
        float distance = dx * dx + dy * dy;
        float angle = (float) atan2(dy, dx);

        float d = (angle + PI) / TWO_PI * numRays;
        int i = (int) d;

        float f = d - i;

        float length = lerp(f, rayLengths[i % numRays], rayLengths[(i + 1) % numRays]);
        float g = length * length / (distance + 0.0001f);

        if (amount != 50) { // if amount = 50 then power = 1, but safer to compare ints
            g = (float) powQuick(g, power);
        }

        f -= 0.5f;
//			f *= amount/50.0f;
        f = 1 - f * f;
        f *= g;

        f = clamp01(f);
        if (lightOnly) {
            return mixColors(f, 0, color);
        } else {
            return mixColors(f, rgb, color);
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

    @Override
    public String toString() {
        return "Stylize/Sparkle...";
    }
}
