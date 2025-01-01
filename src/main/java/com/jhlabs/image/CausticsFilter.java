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

import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A filter that simulates underwater caustics.
 * This can be animated to get a bottom-of-the-swimming-pool effect.
 */
public class CausticsFilter extends WholeImageFilter {
    private float scale = 32.0f;
    private int brightness = 10;
    private float amount = 1.0f;
    private float turbulence = 1.0f;
    private float dispersion = 0.0f;
    private float time = 0.0f;
    private int samples = 2;
    private int bgColor = 0xff799fff;

    public CausticsFilter(String filterName) {
        super(filterName);
    }

    /**
     * Sets the scale of the texture.
     *
     * @param scale the scale of the texture.
     * @min-value 1
     * @max-value 300+
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    /**
     * Sets the brightness.
     *
     * @param brightness the brightness.
     * @min-value 0
     * @max-value 1
     */
    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    /**
     * Sets the turbulence of the texture.
     *
     * @param turbulence the turbulence of the texture.
     * @min-value 0
     * @max-value 1
     */
    public void setTurbulence(float turbulence) {
        this.turbulence = turbulence;
    }

    /**
     * Sets the amount of the effect.
     *
     * @param amount the amount
     * @min-value 0
     * @max-value 1
     */
    public void setAmount(float amount) {
        this.amount = amount;
    }

    /**
     * Sets the color dispersion.
     *
     * @param dispersion the dispersion
     * @min-value 0
     * @max-value 1
     */
    public void setDispersion(float dispersion) {
        this.dispersion = dispersion;
    }

    /**
     * Sets the time. Use this to animate the effect.
     *
     * @param time the time
     */
    public void setTime(float time) {
        this.time = time;
    }

    /**
     * Sets the number of samples per pixel.
     * More samples means better quality, but slower rendering.
     *
     * @param samples the number of samples
     */
    public void setSamples(int samples) {
        this.samples = samples;
    }

    /**
     * Sets the background color.
     *
     * @param c the color
     */
    public void setBgColor(int c) {
        bgColor = c;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        int[] pixels = new int[width * height];

        // initialize all pixels to the background color
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[index++] = bgColor;
            }
        }

        int v = brightness / samples; // brightness per sample
        if (v == 0) {
            v = 1;
        }

        float rs = 1.0f / scale;
        float d = 0.95f;

        pt = createProgressTracker(height);

        Future<?>[] rowFutures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            int finalV = v;
            Runnable rowTask = () -> processRow(width, height, pixels, finalV, rs, d, finalY);
            rowFutures[y] = ThreadPool.submit(rowTask);
        }
        ThreadPool.waitFor(rowFutures, pt);

        finishProgressTracker();

        return pixels;
    }

    private void processRow(int width, int height, int[] pixels, int v, float rs, float d, int y) {
        Random random = ThreadLocalRandom.current();
        for (int x = 0; x < width; x++) {
            for (int sample = 0; sample < samples; sample++) {
                float sx = x + random.nextFloat();
                float sy = y + random.nextFloat();
                float nx = sx * rs;
                float ny = sy * rs;
                float focus = 0.1f + amount;
                float xDisplacement = evaluate(nx - d, ny) - evaluate(nx + d, ny);
                float yDisplacement = evaluate(nx, ny + d) - evaluate(nx, ny - d);

                if (dispersion > 0) {
                    applyDispersionEffect(pixels, width, height, sx, sy, v, focus, xDisplacement, yDisplacement);
                } else {
                    applyBasicEffect(pixels, width, height, sx, sy, v, focus, xDisplacement, yDisplacement);
                }
            }
        }
    }

    private void applyDispersionEffect(int[] pixels, int width, int height, float sx, float sy, int v, float focus, float xDisplacement, float yDisplacement) {
        for (int channel = 0; channel < 3; channel++) {
            float ca = (1 + channel * dispersion);
            float scaleXFocusXca = scale * focus * ca;
            float srcX = sx + scaleXFocusXca * xDisplacement;
            float srcY = sy + scaleXFocusXca * yDisplacement;

            if (srcX < 0 || srcX >= width - 1 || srcY < 0 || srcY >= height - 1) {
            } else {
                int i = ((int) srcY) * width + (int) srcX;
                int rgb = pixels[i];
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                if (channel == 2) {
                    r += v;
                } else if (channel == 1) {
                    g += v;
                } else {
                    b += v;
                }
                if (r > 255) {
                    r = 255;
                }
                if (g > 255) {
                    g = 255;
                }
                if (b > 255) {
                    b = 255;
                }
                pixels[i] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        }
    }

    private void applyBasicEffect(int[] pixels, int width, int height, float sx, float sy, int v, float focus, float xDisplacement, float yDisplacement) {
        float srcX = sx + scale * focus * xDisplacement;
        float srcY = sy + scale * focus * yDisplacement;

        if (srcX < 0 || srcX >= width - 1 || srcY < 0 || srcY >= height - 1) {
        } else {
            int i = ((int) srcY) * width + (int) srcX;
            int rgb = pixels[i];
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            r += v;
            g += v;
            b += v;
            if (r > 255) {
                r = 255;
            }
            if (g > 255) {
                g = 255;
            }
            if (b > 255) {
                b = 255;
            }
            pixels[i] = 0xff000000 | (r << 16) | (g << 8) | b;
        }
    }

    private static float turbulence2(float x, float y, float time, float octaves) {
        float f = 1.0f;
        int i;

        // to prevent "cascading" effects
        x += 371;
        y += 529;

        float value = 0.0f;
        float lacunarity = 2.0f;
        for (i = 0; i < (int) octaves; i++) {
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

    @Override
    public String toString() {
        return "Texture/Caustics...";
    }
}
