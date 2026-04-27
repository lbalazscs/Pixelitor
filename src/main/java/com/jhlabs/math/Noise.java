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

package com.jhlabs.math;

import java.util.Random;

import static com.jhlabs.image.ImageMath.PI;
import static com.jhlabs.image.ImageMath.smoothStep01;

/**
 * Perlin Noise functions.
 */
public class Noise {
    private static final Random randomGenerator = new Random();

    private Noise() {
    }

    public static void reseed(long newSeed) {
        randomGenerator.setSeed(newSeed);
        init();
    }

    // the base size of the permutation/gradient tables
    private static final int B = 0x100; // 256

    // bitmask used as a fast modulo, because x % 256 == x & 255
    // (keeps indices inside the 0–255 range)
    private static final int BM = 0xFF; // 255

    // offset added to input coordinates: guarantees that
    // even negative coordinates produce a positive integer part
    private static final int N = 0x1000; // 4096

    // the permutation table: turns any integer coordinate (ix, iy, iz)
    // into a “random” but repeatable index into the gradient tables
    private static final int[] p = new int[B + B + 2];

    // 1-D gradients: a slope at each grid point
    // (each entry is a random value in [-1, 1])
    private static final float[] g1 = new float[B + B + 2];

    // 2-D gradients: random directions on the unit circle
    // (each pair is a unit vector pointing in a random direction)
    private static final float[] g2x = new float[B + B + 2];
    private static final float[] g2y = new float[B + B + 2];

    // 3-D gradients: random unit vectors in 3D space
    // (each triplet is a normalised random unit vector)
    private static final float[] g3x = new float[B + B + 2];
    private static final float[] g3y = new float[B + B + 2];
    private static final float[] g3z = new float[B + B + 2];

    static {
        init();
    }

    /**
     * Compute turbulence using Perlin noise.
     *
     * @param x       the x value
     * @param y       the y value
     * @param octaves the number of octaves of turbulence
     * @return the turbulence value at (x, y)
     */
    public static double turbulence2(double x, double y, double octaves) {
        double t = 0.0;

        for (double f = 1.0; f <= octaves; f *= 2.0) {
            t += Math.abs(noise2((float) (f * x), (float) (f * y))) / f;
        }
        return t;
    }

    /**
     * Compute smoother turbulence using Perlin noise without absolute value.
     *
     * @param x       the x value
     * @param y       the y value
     * @param octaves the number of octaves of turbulence
     * @return the turbulence value at (x, y)
     */
    public static double turbulence2Smooth(double x, double y, double octaves) {
        double t = 0.0;

        for (double f = 1.0; f <= octaves; f *= 2.0) {
            t += noise2((float) (f * x), (float) (f * y)) / f;
        }
        return t;
    }

    /**
     * Compute turbulence using Perlin noise.
     *
     * @param x       the x value
     * @param y       the y value
     * @param z       the z value
     * @param octaves the number of octaves of turbulence
     * @return the turbulence value at (x, y, z)
     */
    public static float turbulence3(float x, float y, float z, float octaves) {
        float t = 0.0f;

        for (float f = 1.0f; f <= octaves; f *= 2.0f) {
            t += Math.abs(noise3(f * x, f * y, f * z)) / f;
        }
        return t;
    }

    /**
     * Compute 1-dimensional Perlin noise.
     *
     * @param x the x value
     * @return the noise value at x in the range -1..1
     */
    public static float noise1(float x) {
        float t = x + N;
        int bx0 = ((int) t) & BM;
        int bx1 = (bx0 + 1) & BM;
        float rx0 = t - (int) t;
        float rx1 = rx0 - 1.0f;

        float sx = smoothStep01(rx0);

        float u = rx0 * g1[p[bx0]];
        float v = rx1 * g1[p[bx1]];
        return 2.3f * lerp(sx, u, v);
    }

    /**
     * Compute a noise function with a period of 2 PI and values between -1 and 1.
     *
     * @param x the x value
     * @return the noise value at x
     */
    public static float sinLikeNoise1(float x) {
        return 2.0f * noise1(x / PI);
    }

    /**
     * Compute 2-dimensional Perlin noise.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the noise value at (x, y)
     */
    public static float noise2(float x, float y) {
        float t = x + N;
        int bx0 = ((int) t) & BM;
        int bx1 = (bx0 + 1) & BM;
        float rx0 = t - (int) t;
        float rx1 = rx0 - 1.0f;

        t = y + N;
        int by0 = ((int) t) & BM;
        int by1 = (by0 + 1) & BM;
        float ry0 = t - (int) t;
        float ry1 = ry0 - 1.0f;

        int i = p[bx0];
        int j = p[bx1];

        int b00 = p[i + by0];
        int b10 = p[j + by0];
        int b01 = p[i + by1];
        int b11 = p[j + by1];

        float sx = smoothStep01(rx0);
        float sy = smoothStep01(ry0);

        float u = rx0 * g2x[b00] + ry0 * g2y[b00];
        float v = rx1 * g2x[b10] + ry0 * g2y[b10];
        float a = lerp(sx, u, v);

        u = rx0 * g2x[b01] + ry1 * g2y[b01];
        v = rx1 * g2x[b11] + ry1 * g2y[b11];
        float b = lerp(sx, u, v);

        float rv = 1.5f * lerp(sy, a, b);

        assert !Float.isNaN(rv) : String.format(
            "noise2() produced NaN! Input parameters x=%s, y=%s. State: sx=%s, sy=%s, a=%s, b=%s",
            x, y, sx, sy, a, b
        );
        return rv;
    }

    /**
     * Compute 3-dimensional Perlin noise.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the noise value at (x, y, z)
     */
    public static float noise3(float x, float y, float z) {
        float t = x + N;
        int bx0 = ((int) t) & BM;
        int bx1 = (bx0 + 1) & BM;
        float rx0 = t - (int) t;
        float rx1 = rx0 - 1.0f;

        t = y + N;
        int by0 = ((int) t) & BM;
        int by1 = (by0 + 1) & BM;
        float ry0 = t - (int) t;
        float ry1 = ry0 - 1.0f;

        t = z + N;
        int bz0 = ((int) t) & BM;
        int bz1 = (bz0 + 1) & BM;
        float rz0 = t - (int) t;
        float rz1 = rz0 - 1.0f;

        int i = p[bx0];
        int j = p[bx1];

        int b00 = p[i + by0];
        int b10 = p[j + by0];
        int b01 = p[i + by1];
        int b11 = p[j + by1];

        float sx = smoothStep01(rx0);
        float sy = smoothStep01(ry0);
        float sz = smoothStep01(rz0);

        float u = rx0 * g3x[b00 + bz0] + ry0 * g3y[b00 + bz0] + rz0 * g3z[b00 + bz0];
        float v = rx1 * g3x[b10 + bz0] + ry0 * g3y[b10 + bz0] + rz0 * g3z[b10 + bz0];
        float a = lerp(sx, u, v);

        u = rx0 * g3x[b01 + bz0] + ry1 * g3y[b01 + bz0] + rz0 * g3z[b01 + bz0];
        v = rx1 * g3x[b11 + bz0] + ry1 * g3y[b11 + bz0] + rz0 * g3z[b11 + bz0];
        float b = lerp(sx, u, v);

        float c = lerp(sy, a, b);

        u = rx0 * g3x[b00 + bz1] + ry0 * g3y[b00 + bz1] + rz1 * g3z[b00 + bz1];
        v = rx1 * g3x[b10 + bz1] + ry0 * g3y[b10 + bz1] + rz1 * g3z[b10 + bz1];
        a = lerp(sx, u, v);

        u = rx0 * g3x[b01 + bz1] + ry1 * g3y[b01 + bz1] + rz1 * g3z[b01 + bz1];
        v = rx1 * g3x[b11 + bz1] + ry1 * g3y[b11 + bz1] + rz1 * g3z[b11 + bz1];
        b = lerp(sx, u, v);

        float d = lerp(sy, a, b);

        return 1.5f * lerp(sz, c, d);
    }

    public static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    // returns a non-negative random integer
    private static int random() {
        return randomGenerator.nextInt() & Integer.MAX_VALUE; // removes the sign bit
    }

    private static void init() {
        for (int i = 0; i < B; i++) {
            p[i] = i;

            g1[i] = (float) ((random() % (B + B)) - B) / B;

            // reroll until we get a non-zero 2D vector
            do {
                g2x[i] = (float) ((random() % (B + B)) - B) / B;
                g2y[i] = (float) ((random() % (B + B)) - B) / B;
            } while (g2x[i] == 0.0f && g2y[i] == 0.0f);

            float s2 = (float) Math.sqrt(g2x[i] * g2x[i] + g2y[i] * g2y[i]);
            g2x[i] /= s2;
            g2y[i] /= s2;

            // reroll until we get a non-zero 3D vector
            do {
                g3x[i] = (float) ((random() % (B + B)) - B) / B;
                g3y[i] = (float) ((random() % (B + B)) - B) / B;
                g3z[i] = (float) ((random() % (B + B)) - B) / B;
            } while (g3x[i] == 0.0f && g3y[i] == 0.0f && g3z[i] == 0.0f);

            float s3 = (float) Math.sqrt(g3x[i] * g3x[i] + g3y[i] * g3y[i] + g3z[i] * g3z[i]);
            g3x[i] /= s3;
            g3y[i] /= s3;
            g3z[i] /= s3;
        }

        for (int i = B - 1; i >= 0; i--) {
            int k = p[i];
            int j = random() % B;
            p[i] = p[j];
            p[j] = k;
        }

        for (int i = 0; i < B + 2; i++) {
            p[B + i] = p[i];
            g1[B + i] = g1[i];

            g2x[B + i] = g2x[i];
            g2y[B + i] = g2y[i];

            g3x[B + i] = g3x[i];
            g3y[B + i] = g3y[i];
            g3z[B + i] = g3z[i];
        }
    }
}
