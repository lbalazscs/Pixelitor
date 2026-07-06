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

import pixelitor.progress.ProgressTracker;

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
        thresholdBlur(kernel, inPixels, outPixels, width, height, pt);
        thresholdBlur(kernel, outPixels, inPixels, height, width, pt);

        setRGB(dst, 0, 0, width, height, inPixels);

        finishProgressTracker();

        return dst;
    }

    /**
     * Convolve with a kernel consisting of one row.
     */
    private void thresholdBlur(Kernel kernel, int[] inPixels, int[] outPixels,
                               int width, int height, ProgressTracker pt) {
        float[] matrix = kernel.getKernelData(null);
        int cols = kernel.getWidth();
        int cols2 = cols / 2;

        for (int y = 0; y < height; y++) {
            int ioffset = y * width;
            int outIndex = y;
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0, a = 0;
                int moffset = cols2;

                int rgb1 = inPixels[ioffset + x];
                int a1 = rgb1 >>> 24;
                int r1 = (rgb1 >> 16) & 0xFF;
                int g1 = (rgb1 >> 8) & 0xFF;
                int b1 = rgb1 & 0xFF;

                float af = 0, rf = 0, gf = 0, bf = 0;

                for (int col = -cols2; col <= cols2; col++) {
                    float f = matrix[moffset + col];
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

                    int d;

                    d = a1 - a2;
                    if (d >= -threshold && d <= threshold) {
                        a += f * a2;
                        af += f;
                    }
                    d = r1 - r2;
                    if (d >= -threshold && d <= threshold) {
                        r += f * r2;
                        rf += f;
                    }
                    d = g1 - g2;
                    if (d >= -threshold && d <= threshold) {
                        g += f * g2;
                        gf += f;
                    }
                    d = b1 - b2;
                    if (d >= -threshold && d <= threshold) {
                        b += f * b2;
                        bf += f;
                    }
                }

                // the accumulation variables will never be 0,
                // because a Gaussian kernel center is always positive
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
