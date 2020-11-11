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

import com.jhlabs.image.ImageMath;

import java.util.Random;

/**
 * Perlin Noise functions
 */
public class Noise implements Function1D, Function2D, Function3D {
    private static final Random randomGenerator = new Random();

    public static void reseed() {
        randomGenerator.setSeed(System.nanoTime());
        init();
    }

    private static final int B = 0x100;
    private static final int BM = 0xff;
    private static final int N = 0x1000;

    static final int[] p = new int[B + B + 2];
    static final float[][] g3 = new float[B + B + 2][3];
    static final float[][] g2 = new float[B + B + 2][2];
    static final float[] g1 = new float[B + B + 2];

    static {
        init();
    }

    @Override
    public float evaluate(float x) {
        return noise1(x);
    }

    @Override
    public float evaluate(float x, float y) {
        return noise2(x, y);
    }

    @Override
    public float evaluate(float x, float y, float z) {
        return noise3(x, y, z);
    }

    /**
     * Compute turbulence using Perlin noise.
     *
     * @param x       the x value
     * @param y       the y value
     * @param octaves number of octaves of turbulence
     * @return turbulence value at (x,y)
     */
    public static float turbulence2(float x, float y, float octaves) {
        float t = 0.0f;

        for (float f = 1.0f; f <= octaves; f *= 2) {
            t += Math.abs(noise2(f * x, f * y)) / f;
        }
        return t;
    }

    /**
     * Same as above but without abs, results in a smoother look
     */
    public static float turbulence2B(float x, float y, float octaves) {
        float t = 0.0f;

        for (float f = 1.0f; f <= octaves; f *= 2) {
            t += noise2(f * x, f * y) / f;
        }
        return t;
    }

    /**
     * Compute turbulence using Perlin noise.
     *
     * @param x       the x value
     * @param y       the y value
     * @param octaves number of octaves of turbulence
     * @return turbulence value at (x,y)
     */
    public static float turbulence3(float x, float y, float z, float octaves) {
        float t = 0.0f;

        for (float f = 1.0f; f <= octaves; f *= 2) {
            t += Math.abs(noise3(f * x, f * y, f * z)) / f;
        }
        return t;
    }


    /**
     * Compute 1-dimensional Perlin noise.
     *
     * @param x the x value
     * @return noise value at x in the range -1..1
     */
    public static float noise1(float x) {
        float t = x + N;
        int bx0 = ((int) t) & BM;
        int bx1 = (bx0 + 1) & BM;
        float rx0 = t - (int) t;
        float rx1 = rx0 - 1.0f;

        float sx = ImageMath.smoothStep01(rx0);

        float u = rx0 * g1[p[bx0]];
        float v = rx1 * g1[p[bx1]];
        return 2.3f * lerp(sx, u, v);
    }

    /**
     * A noise function with a "period" of 2 PI and values between -1 and 1
     */
    public static float sinLikeNoise1(float x) {
        return 2 * noise1(x / ImageMath.PI);
    }

    /**
     * Compute 2-dimensional Perlin noise.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return noise value at (x,y) - a value between -1 and 1, but it can be a bit smaller like -1.0362637
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

        float sx = ImageMath.smoothStep01(rx0);
        float sy = ImageMath.smoothStep01(ry0);

        float[] q = g2[b00];
        float u = rx0 * q[0] + ry0 * q[1];
        q = g2[b10];
        float v = rx1 * q[0] + ry0 * q[1];
        float a = lerp(sx, u, v);

        q = g2[b01];
        u = rx0 * q[0] + ry1 * q[1];
        q = g2[b11];
        v = rx1 * q[0] + ry1 * q[1];
        float b = lerp(sx, u, v);

        float rv = 1.5f * lerp(sy, a, b);
        if (Float.isNaN(rv)) {
            // it seems that this is very rarely, but happening when rendering marble
            System.out.printf("Noise::noise2: failed for x = %.5f, y = %.5f%n", x, y);
            rv = 0.0f;
        }

        return rv;
    }


    /**
     * Compute 3-dimensional Perlin noise.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param y the y coordinate
     * @return noise value at (x,y,z)
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

        t = ImageMath.smoothStep01(rx0);
        float sy = ImageMath.smoothStep01(ry0);
        float sz = ImageMath.smoothStep01(rz0);

        float[] q = g3[b00 + bz0];
        float u = rx0 * q[0] + ry0 * q[1] + rz0 * q[2];
        q = g3[b10 + bz0];
        float v = rx1 * q[0] + ry0 * q[1] + rz0 * q[2];
        float a = lerp(t, u, v);

        q = g3[b01 + bz0];
        u = rx0 * q[0] + ry1 * q[1] + rz0 * q[2];
        q = g3[b11 + bz0];
        v = rx1 * q[0] + ry1 * q[1] + rz0 * q[2];
        float b = lerp(t, u, v);

        float c = lerp(sy, a, b);

        q = g3[b00 + bz1];
        u = rx0 * q[0] + ry0 * q[1] + rz1 * q[2];
        q = g3[b10 + bz1];
        v = rx1 * q[0] + ry0 * q[1] + rz1 * q[2];
        a = lerp(t, u, v);

        q = g3[b01 + bz1];
        u = rx0 * q[0] + ry1 * q[1] + rz1 * q[2];
        q = g3[b11 + bz1];
        v = rx1 * q[0] + ry1 * q[1] + rz1 * q[2];
        b = lerp(t, u, v);

        float d = lerp(sy, a, b);

        return 1.5f * lerp(sz, c, d);
    }

    public static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    private static void normalize2(float[] v) {
        float s = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1]);
        v[0] = v[0] / s;
        v[1] = v[1] / s;
    }

    static void normalize3(float[] v) {
        float s = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] = v[0] / s;
        v[1] = v[1] / s;
        v[2] = v[2] / s;
    }

    private static int random() {
        return randomGenerator.nextInt() & 0x7fffffff;
    }

    private static void init() {
        int i, j;

        for (i = 0; i < B; i++) {
            p[i] = i;

            g1[i] = (float) ((random() % (B + B)) - B) / B;

            for (j = 0; j < 2; j++) {
                g2[i][j] = (float) ((random() % (B + B)) - B) / B;
            }
            normalize2(g2[i]);

            for (j = 0; j < 3; j++) {
                g3[i][j] = (float) ((random() % (B + B)) - B) / B;
            }
            normalize3(g3[i]);
        }

        for (i = B - 1; i >= 0; i--) {
            int k = p[i];
            p[i] = p[j = random() % B];
            p[j] = k;
        }

        for (i = 0; i < B + 2; i++) {
            p[B + i] = p[i];
            g1[B + i] = g1[i];
            for (j = 0; j < 2; j++) {
                g2[B + i][j] = g2[i][j];
            }
            for (j = 0; j < 3; j++) {
                g3[B + i][j] = g3[i][j];
            }
        }
    }

    /**
     * Returns the minimum and maximum of a number of random values
     * of the given function. This is useful for making some stab at
     * normalising the function.
     */
    public static float[] findRange(Function1D f, float[] minmax) {
        if (minmax == null) {
            minmax = new float[2];
        }
        float min = 0, max = 0;
        // Some random numbers here...
        for (float x = -100; x < 100; x = (float) (x + 1.27139)) {
            float n = f.evaluate(x);
            min = Math.min(min, n);
            max = Math.max(max, n);
        }
        minmax[0] = min;
        minmax[1] = max;
        return minmax;
    }

    /**
     * Returns the minimum and maximum of a number of random values
     * of the given function. This is useful for making some stab at
     * normalising the function.
     */
    public static float[] findRange(Function2D f, float[] minmax) {
        if (minmax == null) {
            minmax = new float[2];
        }
        float min = 0, max = 0;
        // Some random numbers here...
        for (float y = -100; y < 100; y = (float) (y + 10.35173)) {
            for (float x = -100; x < 100; x = (float) (x + 10.77139)) {
                float n = f.evaluate(x, y);
                min = Math.min(min, n);
                max = Math.max(max, n);
            }
        }
        minmax[0] = min;
        minmax[1] = max;
        return minmax;
    }
}
