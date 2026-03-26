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
     * @param filterName the name of the filter
     * @param color      the color of the metal
     * @param radius     the radius of the blur in the horizontal direction
     * @param amount     the amount of texture/noise to add (in the range [0, 1])
     * @param shine      the amount of shine to add (in the range [0, 1])
     */
    public BrushedMetalFilter(String filterName, int color, int radius, float amount, float shine) {
        super(filterName);

        this.color = color;
        this.radius = radius;
        this.amount = 255 * amount;
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
        int[] outPixels = radius != 0 ? new int[width] : null;

        int a = color & 0xFF_00_00_00;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // precomputed base colors for each column
        int[] rBase = new int[width];
        int[] gBase = new int[width];
        int[] bBase = new int[width];

        double piOverWidth = Math.PI / width;
        for (int x = 0; x < width; x++) {
            int f = shine != 0 ? (int) (255 * shine * FastMath.sin(x * piOverWidth)) : 0;
            rBase[x] = r + f;
            gBase[x] = g + f;
            bBase[x] = b + f;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int n = (int) ((2 * random.nextFloat() - 1) * amount);
                inPixels[x] = a | (clamp(rBase[x] + n) << 16) | (clamp(gBase[x] + n) << 8) | clamp(bBase[x] + n);
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

    public static void blur(int[] in, int[] out, int width, int radius) {
        int widthMinus1 = width - 1;
        int r2 = 2 * radius + 1;
        int tr = 0, tg = 0, tb = 0;

        for (int i = -radius; i <= radius; i++) {
            int rgb = in[Math.floorMod(i, width)];
            tr += (rgb >> 16) & 0xFF;
            tg += (rgb >> 8) & 0xFF;
            tb += rgb & 0xFF;
        }

        for (int x = 0; x < width; x++) {
            out[x] = 0xFF_00_00_00 | ((tr / r2) << 16) | ((tg / r2) << 8) | (tb / r2);

            int i1 = x + radius + 1;
            if (i1 > widthMinus1) {
                i1 = Math.floorMod(i1, width);
            }
            int i2 = x - radius;
            if (i2 < 0) {
                i2 = Math.floorMod(i2, width);
            }
            int rgb1 = in[i1];
            int rgb2 = in[i2];

            tr += ((rgb1 & 0xFF_00_00) - (rgb2 & 0xFF_00_00)) >> 16;
            tg += ((rgb1 & 0xFF_00) - (rgb2 & 0xFF_00)) >> 8;
            tb += (rgb1 & 0xFF) - (rgb2 & 0xFF);
        }
    }

    public void setRandom(RandomGenerator random) {
        this.random = random;
    }
}
