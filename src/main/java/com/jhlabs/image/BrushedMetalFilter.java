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

import net.jafama.FastMath;

import java.awt.image.BufferedImage;
import java.util.random.RandomGenerator;

import static com.jhlabs.image.PixelUtils.clamp;

/**
 * A filter which produces an image simulating brushed metal.
 */
public class BrushedMetalFilter extends AbstractBufferedImageOp {
    private final int radius;
    private final float amount;
    private final int color;
    private final float shine;
    
    private RandomGenerator random;

    /**
     * Constructs a {@link BrushedMetalFilter}.
     *
     * @param color  the color of the metal
     * @param radius the radius of the blur in the horizontal direction
     * @param amount the amount of texture/noise to add in the range 0..1
     * @param shine  the amount of shine to add to the range 0..1.
     */
    public BrushedMetalFilter(String filterName, int color, int radius, float amount, float shine) {
        super(filterName);

        this.color = color;
        this.radius = radius;
        this.amount = amount;
        this.shine = shine;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        pt = createProgressTracker(height);

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] inPixels = new int[width];
        int[] outPixels = new int[width];

        int a = color & 0xff000000;
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;

        int[] shineFactors = null;
        if (shine != 0) {
            shineFactors = new int[width];
            for (int x = 0; x < width; x++) {
                int f = (int) (255 * shine * FastMath.sin((double) x / width * Math.PI));
                shineFactors[x] = f;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int tr = r;
                int tg = g;
                int tb = b;
                if (shine != 0) {
                    int f = shineFactors[x];
                    tr += f;
                    tg += f;
                    tb += f;
                }
                int n = (int) (255 * (2 * random.nextDouble() - 1) * amount);
                inPixels[x] = a | (clamp(tr + n) << 16) | (clamp(tg + n) << 8) | clamp(tb + n);
            }

            if (radius != 0) {
                blur(inPixels, outPixels, width, radius);
                setRGB(dst, 0, y, width, 1, outPixels);
            } else {
                setRGB(dst, 0, y, width, 1, inPixels);
            }

            pt.unitDone();
        }

        finishProgressTracker();
        return dst;
    }

    private int random(int x) {
        x += (int) (255 * (2 * random.nextDouble() - 1) * amount);
        if (x < 0) {
            x = 0;
        } else if (x > 0xff) {
            x = 0xff;
        }
        return x;
    }

    /**
     * Return a mod b. This differs from the % operator with respect to negative numbers.
     *
     * @param a the dividend
     * @param b the divisor
     * @return a mod b
     */
    private static int mod(int a, int b) {
        int n = a / b;

        a -= n * b;
        if (a < 0) {
            return a + b;
        }
        return a;
    }

    public static void blur(int[] in, int[] out, int width, int radius) {
        int widthMinus1 = width - 1;
        int r2 = 2 * radius + 1;
        int tr = 0, tg = 0, tb = 0;

        for (int i = -radius; i <= radius; i++) {
            int rgb = in[mod(i, width)];
            tr += (rgb >> 16) & 0xff;
            tg += (rgb >> 8) & 0xff;
            tb += rgb & 0xff;
        }

        for (int x = 0; x < width; x++) {
            out[x] = 0xff000000 | ((tr / r2) << 16) | ((tg / r2) << 8) | (tb / r2);

            int i1 = x + radius + 1;
            if (i1 > widthMinus1) {
                i1 = mod(i1, width);
            }
            int i2 = x - radius;
            if (i2 < 0) {
                i2 = mod(i2, width);
            }
            int rgb1 = in[i1];
            int rgb2 = in[i2];

            tr += ((rgb1 & 0xff0000) - (rgb2 & 0xff0000)) >> 16;
            tg += ((rgb1 & 0xff00) - (rgb2 & 0xff00)) >> 8;
            tb += (rgb1 & 0xff) - (rgb2 & 0xff);
        }
    }

    public void setRandom(RandomGenerator random) {
        this.random = random;
    }

    @Override
    public String toString() {
        return "Texture/Brushed Metal...";
    }
}
