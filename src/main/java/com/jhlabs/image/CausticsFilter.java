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
import pixelitor.ThreadPool;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A filter that simulates underwater caustics.
 * This can be animated to get a bottom-of-the-swimming-pool effect.
 */
public class CausticsFilter extends WholeImageFilter {
    private final float scale;
    private final int brightness;
    private final float amount;
    private final float turbulence;
    private final float dispersion;
    private final float time;
    private final int samples;
    private final int bgColor;

    /**
     * Constructs a new {@link CausticsFilter}.
     *
     * @param filterName the name of the filter
     * @param scale      the scale of the texture (in the range 1..300+)
     * @param brightness the brightness (in the range [0, 1])
     * @param amount     the amount of the effect (in the range [0, 1])
     * @param turbulence the turbulence of the texture (in the range [0, 1])
     * @param dispersion the color dispersion (in the range [0, 1])
     * @param time       the time, used to animate the effect
     * @param samples    the number of samples per pixel. More samples means better quality, but slower rendering
     * @param bgColor    the background color
     */
    public CausticsFilter(String filterName, float scale, int brightness, float amount,
                          float turbulence, float dispersion, float time, int samples, int bgColor) {
        super(filterName);
        this.scale = scale;
        this.brightness = brightness;
        this.amount = amount;
        this.turbulence = turbulence;
        this.dispersion = dispersion;
        this.time = time;
        this.samples = samples;
        this.bgColor = bgColor;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        int[] pixels = new int[width * height];

        // initialize all pixels to the background color
        Arrays.fill(pixels, bgColor);

        int v = Math.max(1, brightness / samples); // brightness per sample

        float rs = 1.0f / scale;
        float d = 0.95f;

        pt = createProgressTracker(height);

        Future<?>[] rowFutures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable rowTask = () -> processRow(width, height, pixels, v, rs, d, finalY);
            rowFutures[y] = ThreadPool.submit(rowTask);
        }
        ThreadPool.waitFor(rowFutures, pt);

        finishProgressTracker();

        return pixels;
    }

    private void processRow(int width, int height, int[] pixels, int v, float rs, float d, int y) {
        Random random = ThreadLocalRandom.current();
        float focus = 0.1f + amount;
        float scaleFocus = scale * focus;

        if (dispersion > 0) {
            // precalculate dispersed scales
            float sfc0 = scaleFocus * 1; // channel 0 (blue)
            float sfc1 = scaleFocus * (1 + dispersion); // channel 1 (green)
            float sfc2 = scaleFocus * (1 + 2 * dispersion); // channel 2 (red)

            for (int x = 0; x < width; x++) {
                for (int sample = 0; sample < samples; sample++) {
                    float sx = x + random.nextFloat();
                    float sy = y + random.nextFloat();
                    float nx = sx * rs;
                    float ny = sy * rs;
                    float xDisplacement = evaluate(nx - d, ny) - evaluate(nx + d, ny);
                    float yDisplacement = evaluate(nx, ny + d) - evaluate(nx, ny - d);

                    applyDispersionChannel(pixels, width, height, sx, sy, v, sfc0, xDisplacement, yDisplacement, 0);
                    applyDispersionChannel(pixels, width, height, sx, sy, v, sfc1, xDisplacement, yDisplacement, 8);
                    applyDispersionChannel(pixels, width, height, sx, sy, v, sfc2, xDisplacement, yDisplacement, 16);
                }
            }
        } else {
            for (int x = 0; x < width; x++) {
                for (int sample = 0; sample < samples; sample++) {
                    float sx = x + random.nextFloat();
                    float sy = y + random.nextFloat();
                    float nx = sx * rs;
                    float ny = sy * rs;
                    float xDisplacement = evaluate(nx - d, ny) - evaluate(nx + d, ny);
                    float yDisplacement = evaluate(nx, ny + d) - evaluate(nx, ny - d);

                    applyBasicEffect(pixels, width, height, sx, sy, v, scaleFocus, xDisplacement, yDisplacement);
                }
            }
        }
    }

    private static void applyDispersionChannel(int[] pixels, int width, int height, float sx, float sy, int v,
                                               float sfc, float xDisplacement, float yDisplacement, int shift) {
        float srcX = sx + sfc * xDisplacement;
        float srcY = sy + sfc * yDisplacement;

        if (srcX >= 0 && srcX < width - 1 && srcY >= 0 && srcY < height - 1) {
            int i = ((int) srcY) * width + (int) srcX;
            int mask = ~(0xFF << shift);
            pixels[i] = (pixels[i] & mask) | (Math.min(((pixels[i] >> shift) & 0xFF) + v, 255) << shift);
        }
    }

    private static void applyBasicEffect(int[] pixels, int width, int height, float sx, float sy, int v, float scaleFocus, float xDisplacement, float yDisplacement) {
        float srcX = sx + scaleFocus * xDisplacement;
        float srcY = sy + scaleFocus * yDisplacement;

        if (srcX >= 0 && srcX < width - 1 && srcY >= 0 && srcY < height - 1) {
            int i = ((int) srcY) * width + (int) srcX;
            int rgb = pixels[i];

            int r = Math.min(((rgb >> 16) & 0xFF) + v, 255);
            int g = Math.min(((rgb >> 8) & 0xFF) + v, 255);
            int b = Math.min((rgb & 0xFF) + v, 255);

            pixels[i] = 0xFF_00_00_00 | (r << 16) | (g << 8) | b;
        }
    }

    private static float turbulence2(float x, float y, float time, float octaves) {
        float f = 1.0f;

        // to prevent "cascading" effects
        x += 371;
        y += 529;

        float value = 0.0f;
        float lacunarity = 2.0f;
        for (int i = 0; i < (int) octaves; i++) {
            value += Noise.noise3(x, y, time) / f;
            x *= lacunarity;
            y *= lacunarity;
            f *= 2;
        }

        float remainder = octaves - (int) octaves;
        if (remainder != 0) {
            value += remainder * Noise.noise3(x, y, time) / f;
        }

        return value;
    }

    private float evaluate(float x, float y) {
        float xt = x + time;
        float tt = x - time;
        return turbulence2(xt, y, tt, turbulence);
    }
}
