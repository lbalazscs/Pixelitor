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

import java.awt.image.BufferedImage;
import java.awt.image.Kernel;

/**
 * A filter which performs a "smart blur", i.e. a blur that smooths
 * homogeneous regions of an image while preserving edges.
 */
public class SmartBlurFilter extends AbstractBufferedImageOp {
    private final int radius;
    private final int threshold;

    /**
     * Creates a new smart blur filter.
     *
     * @param filterName the name of the filter.
     * @param radius     the blur radius.
     * @param threshold  the threshold used to decide whether neighboring pixels
     *                   are similar enough to be blurred together. Smaller values
     *                   preserve edges more aggressively.
     */
    public SmartBlurFilter(String filterName, int radius, int threshold) {
        super(filterName);

        assert radius >= 0 && threshold >= 0;

        this.radius = radius;
        this.threshold = threshold;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        pt = createProgressTracker(width + height);

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] inPixels = new int[width * height];
        int[] outPixels = new int[width * height];
        getRGB(src, 0, 0, width, height, inPixels);

        Kernel kernel = GaussianFilter.makeKernel(radius);
        thresholdBlurAndTranspose(kernel, inPixels, outPixels, width, height); // horizontal pass
        thresholdBlurAndTranspose(kernel, outPixels, inPixels, height, width); // vertical pass

        setRGB(dst, 0, 0, width, height, inPixels);

        finishProgressTracker();

        return dst;
    }

    /**
     * Applies a 1D threshold-aware blur along image rows, writing the result
     * transposed (column-major) into outPixels. Calling this once horizontally
     * and once vertically (with width/height swapped) produces the full 2D
     * "smart blur". Each channel of each pixel is only blurred with neighbors
     * whose value in that channel is within {@code threshold} of the center
     * pixel's, which is what keeps edges sharp while smoothing flat regions.
     */
    private void thresholdBlurAndTranspose(Kernel kernel, int[] inPixels, int[] outPixels,
                                           int width, int height) {
        float[] matrix = kernel.getKernelData(null);
        int cols2 = kernel.getWidth() / 2;

        for (int y = 0; y < height; y++) {
            int ioffset = y * width;
            int outIndex = y;
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0, a = 0;

                int rgb1 = inPixels[ioffset + x];
                int a1 = rgb1 >>> 24;
                int r1 = (rgb1 >> 16) & 0xFF;
                int g1 = (rgb1 >> 8) & 0xFF;
                int b1 = rgb1 & 0xFF;

                float af = 0, rf = 0, gf = 0, bf = 0;

                for (int col = -cols2; col <= cols2; col++) {
                    float f = matrix[cols2 + col];
                    if (f == 0) {
                        continue;
                    }

                    int ix = x + col;
                    if (ix < 0 || ix >= width) {
                        ix = x;
                    }

                    int rgb2 = inPixels[ioffset + ix];
                    int a2 = rgb2 >>> 24;
                    int r2 = (rgb2 >> 16) & 0xFF;
                    int g2 = (rgb2 >> 8) & 0xFF;
                    int b2 = rgb2 & 0xFF;

                    if (Math.abs(a1 - a2) <= threshold) {
                        a += f * a2;
                        af += f;
                    }
                    if (Math.abs(r1 - r2) <= threshold) {
                        r += f * r2;
                        rf += f;
                    }
                    if (Math.abs(g1 - g2) <= threshold) {
                        g += f * g2;
                        gf += f;
                    }
                    if (Math.abs(b1 - b2) <= threshold) {
                        b += f * b2;
                        bf += f;
                    }
                }

                // the divisors below will never be 0, because
                // a Gaussian kernel center is always positive
                a = a / af;
                r = r / rf;
                g = g / gf;
                b = b / bf;

                int ia = PixelUtils.clamp((int) (a + 0.5));
                int ir = PixelUtils.clamp((int) (r + 0.5));
                int ig = PixelUtils.clamp((int) (g + 0.5));
                int ib = PixelUtils.clamp((int) (b + 0.5));

                outPixels[outIndex] = (ia << 24) | (ir << 16) | (ig << 8) | ib;
                outIndex += height;
            }
            pt.unitDone();
        }
    }
}
