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
import static com.jhlabs.image.ImageMath.clamp;
import static com.jhlabs.image.ImageMath.lerp;
import static com.jhlabs.image.ImageMath.mixColors;
import static net.jafama.FastMath.atan2;
import static net.jafama.FastMath.powQuick;

public class SparkleFilter extends PointFilter {

    private int rays = 50;
    private int radius = 25;
    private int amount = 50;
    private int color = 0xffffffff;
    private int randomness = 25;
    //    private int width, height;
    private int centreX, centreY;
    //    private long seed = 371;
    private float[] rayLengths;
    private Random random;

    private float relativeCentreX = 0.5f;
    private float relativeCentreY = 0.5f;
    private boolean lightOnly;

    private double power;

    public SparkleFilter(String filterName) {
        super(filterName);
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setRandomness(int randomness) {
        this.randomness = randomness;
    }

    public int getRandomness() {
        return randomness;
    }

    /**
     * Set the amount of sparkle.
     *
     * @param amount the amount
     * @min-value 0
     * @max-value 1
     * @see #getAmount
     */
    public void setAmount(int amount) {
        this.amount = amount;
    }

    /**
     * Get the amount of sparkle.
     *
     * @return the amount
     * @see #setAmount
     */
    public int getAmount() {
        return amount;
    }

    public void setRays(int rays) {
        this.rays = rays;
    }

    public int getRays() {
        return rays;
    }

    /**
     * Set the radius of the effect.
     *
     * @param radius the radius
     * @min-value 0
     * @see #getRadius
     */
    public void setRadius(int radius) {
        this.radius = radius;
    }

    /**
     * Get the radius of the effect.
     *
     * @return the radius
     * @see #setRadius
     */
    public int getRadius() {
        return radius;
    }

    @Override
    public void setDimensions(int width, int height) {
//        this.width = width;
//        this.height = height;
        centreX = (int) (width * relativeCentreX);
        centreY = (int) (height * relativeCentreY);
        super.setDimensions(width, height);
//        random.setSeed(seed);
        rayLengths = new float[rays];
        for (int i = 0; i < rays; i++) {
            rayLengths[i] = radius + randomness / 100.0f * radius * (float) random.nextGaussian();
        }
        power = (100 - amount) / 50.0;
    }

    @Override
    public int filterRGB(int x, int y, int rgb) {
        float dx = x - centreX;
        float dy = y - centreY;
        float distance = dx * dx + dy * dy;
        float angle = (float) atan2(dy, dx);
        float d = (angle + PI) / (TWO_PI) * rays;
        int i = (int) d;
        float f = d - i;

        if (radius != 0) {
            float length = lerp(f, rayLengths[i % rays], rayLengths[(i + 1) % rays]);
            float g = length * length / (distance + 0.0001f);

            if(amount != 50) { // if amount = 50 then power = 1, but safer to compare ints
                g = (float) powQuick(g, power);
            }

            f -= 0.5f;
//			f *= amount/50.0f;
            f = 1 - f * f;
            f *= g;
        }
        f = clamp(f, 0, 1);
        if (lightOnly) {
            return mixColors(f, 0, color);
        } else {
            return mixColors(f, rgb, color);
        }
    }

    public void setRelativeCentreX(float relativeCentreX) {
        this.relativeCentreX = relativeCentreX;
    }

    public void setRelativeCentreY(float relativeCentreY) {
        this.relativeCentreY = relativeCentreY;
    }

    public float getRelativeCentreX() {
        return relativeCentreX;
    }

    public float getRelativeCentreY() {
        return relativeCentreY;
    }

    public boolean isLightOnly() {
        return lightOnly;
    }

    public void setLightOnly(boolean lightOnly) {
        this.lightOnly = lightOnly;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public String toString() {
		return "Stylize/Sparkle...";
	}


    // the radius does not define the affected area!!
/*

    public Shape[] getAffectedAreaShapes() {
        return new Shape[]{
                new Ellipse2D.Float(centreX - radius, centreY - radius, 2 * radius, 2 * radius)
        };
    }
*/
}
