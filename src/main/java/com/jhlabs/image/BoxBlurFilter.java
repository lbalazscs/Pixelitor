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

import pixelitor.utils.ProgressTracker;

import java.awt.image.BufferedImage;

/**
 * A filter which performs a box blur on an image. The horizontal and vertical blurs can be specified separately
 * and a number of iterations can be given which allows an approximation to Gaussian blur.
 */
public class BoxBlurFilter extends AbstractBufferedImageOp {
    private final float hRadius;
    private final float vRadius;
    private final int iterations;
    private final boolean premultiplyAlpha;

    /**
     * Constructs a BoxBlurFilter.
     *
     * @param filterName the name of the filter
     * @param hRadius    the horizontal radius of blur
     * @param vRadius    the vertical radius of blur
     * @param iterations the number of times to iterate the blur
     */
    public BoxBlurFilter(String filterName, float hRadius, float vRadius, int iterations, boolean premultiplyAlpha) {
        super(filterName);

        if (hRadius < 0 || vRadius < 0 || iterations <= 0) {
            throw new IllegalArgumentException("hRadius = %.2f, vRadius = %.2f, iterations = %d"
                .formatted(hRadius, vRadius, iterations));
        }

        this.hRadius = hRadius;
        this.vRadius = vRadius;
        this.iterations = iterations;
        this.premultiplyAlpha = premultiplyAlpha;
    }

    public BoxBlurFilter(String filterName, float hRadius, float vRadius, int iterations) {
        this(filterName, hRadius, vRadius, iterations, true);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        // the fractional blur is not included in the progress tracking,
        // as it should be executed only for tweening
        pt = createProgressTracker(iterations * (width + height));

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] inPixels = new int[width * height];
        int[] outPixels = new int[width * height];
        getRGB(src, 0, 0, width, height, inPixels);

        boolean premultiply = premultiplyAlpha && !src.isAlphaPremultiplied();
        if (premultiply) {
            ImageMath.premultiply(inPixels, 0, inPixels.length);
        }

        for (int i = 0; i < iterations; i++) {
            // horizontal pass + transpose
            blur(inPixels, outPixels, width, height, hRadius, pt);

            // vertical pass + transpose back
            blur(outPixels, inPixels, height, width, vRadius, pt);
        }

        if (hasFractionalRadius()) {
            blurFractional(inPixels, outPixels, width, height, hRadius);
            blurFractional(outPixels, inPixels, height, width, vRadius);
        }

        if (premultiply) {
            ImageMath.unpremultiply(inPixels, 0, inPixels.length);
        }
        setRGB(dst, 0, 0, width, height, inPixels);

        finishProgressTracker();

        return dst;
    }

    private boolean hasFractionalRadius() {
        return (hRadius - (int) hRadius > 0.001f) || (vRadius - (int) vRadius > 0.001f);
    }

    /**
     * Blurs and transposes a block of ARGB pixels.
     *
     * @param in     the input pixels
     * @param out    the output pixels
     * @param width  the width of the pixel array
     * @param height the height of the pixel array
     * @param radius the radius of blur
     * @param pt     the progress tracker
     */
    private static void blur(int[] in, int[] out, int width, int height, float radius, ProgressTracker pt) {
        int widthMinus1 = width - 1;
        int r = (int) radius;
        int tableSize = 2 * r + 1; // the blur window width

        if (tableSize < 0) {
            throw new IllegalArgumentException(String.format("tableSize is negative, radius = %.2f", radius));
        }

        // precomputed lookup table for divisions
        int[] divide = new int[256 * tableSize];
        for (int i = 0; i < divide.length; i++) {
            divide[i] = i / tableSize;
        }

        int inIndex = 0;

        for (int y = 0; y < height; y++) {
            int outIndex = y;
            int ta = 0, tr = 0, tg = 0, tb = 0;

            // prime the running sum
            for (int i = -r; i <= r; i++) {
                int rgb = in[inIndex + ImageMath.clamp(i, 0, widthMinus1)];
                ta += rgb >>> 24;
                tr += (rgb >> 16) & 0xFF;
                tg += (rgb >> 8) & 0xFF;
                tb += rgb & 0xFF;
            }

            // slide across the row
            for (int x = 0; x < width; x++) {
                out[outIndex] = (divide[ta] << 24) | (divide[tr] << 16) | (divide[tg] << 8) | divide[tb];

                int i1 = Math.min(x + r + 1, widthMinus1); // incoming pixel
                int i2 = Math.max(x - r, 0); // outgoing pixel

                int rgb1 = in[inIndex + i1];
                int rgb2 = in[inIndex + i2];

                // update the running sum
                ta += (rgb1 >>> 24) - (rgb2 >>> 24);
                tr += ((rgb1 & 0xFF_00_00) - (rgb2 & 0xFF_00_00)) >> 16;
                tg += ((rgb1 & 0xFF_00) - (rgb2 & 0xFF_00)) >> 8;
                tb += (rgb1 & 0xFF) - (rgb2 & 0xFF);
                outIndex += height; // transpose: rotates the output 90°
            }
            inIndex += width;

            pt.unitDone();
        }
    }

    /**
     * Applies a fractional blur to the image.
     *
     * @param in     the input pixels
     * @param out    the output pixels
     * @param width  the width of the pixel array
     * @param height the height of the pixel array
     * @param radius the fractional radius of blur
     */
    private static void blurFractional(int[] in, int[] out, int width, int height, float radius) {
        float frac = radius - (int) radius;

        // skip fractional blurring if negligible or if there aren't enough pixels to blur
        if (frac <= 0.001f || width < 2) {
            int inIndex = 0;
            for (int y = 0; y < height; y++) {
                int outIndex = y;
                for (int x = 0; x < width; x++) {
                    out[outIndex] = in[inIndex++];
                    outIndex += height;
                }
            }
            return;
        }

        float f = 1.0f / (1.0f + 2.0f * frac);
        int inIndex = 0;

        for (int y = 0; y < height; y++) {
            int outIndex = y;

            out[outIndex] = in[inIndex];
            outIndex += height;

            for (int x = 1; x < width - 1; x++) {
                int i = inIndex + x;
                int rgb1 = in[i - 1];
                int rgb2 = in[i];
                int rgb3 = in[i + 1];

                int a1 = rgb1 >>> 24;
                int r1 = (rgb1 >> 16) & 0xFF;
                int g1 = (rgb1 >> 8) & 0xFF;
                int b1 = rgb1 & 0xFF;

                int a2 = rgb2 >>> 24;
                int r2 = (rgb2 >> 16) & 0xFF;
                int g2 = (rgb2 >> 8) & 0xFF;
                int b2 = rgb2 & 0xFF;

                int a3 = rgb3 >>> 24;
                int r3 = (rgb3 >> 16) & 0xFF;
                int g3 = (rgb3 >> 8) & 0xFF;
                int b3 = rgb3 & 0xFF;

                int a = (int) ((a2 + (a1 + a3) * frac) * f);
                int r = (int) ((r2 + (r1 + r3) * frac) * f);
                int g = (int) ((g2 + (g1 + g3) * frac) * f);
                int b = (int) ((b2 + (b1 + b3) * frac) * f);

                out[outIndex] = (a << 24) | (r << 16) | (g << 8) | b;
                outIndex += height;
            }

            out[outIndex] = in[inIndex + width - 1];
            inIndex += width;
        }
    }
}
