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

import pixelitor.ThreadPool;
import pixelitor.utils.ProgressTracker;

import java.awt.image.BufferedImage;
import java.awt.image.Kernel;
import java.util.concurrent.Future;

/**
 * A filter which applies Gaussian blur to an image.
 * <p>
 * While this is a subclass of ConvolveFilter, it overrides the default
 * 2D convolution with an optimized, separable 1D convolution approach.
 * It uses multithreading to apply a horizontal blur followed by a vertical blur.
 *
 * @author Jerry Huxtable
 */
public class GaussianFilter extends ConvolveFilter {
    /**
     * The blur radius.
     */
    protected float radius;

    /**
     * Constructs a Gaussian filter.
     */
    public GaussianFilter(String filterName) {
        this(filterName, 2);
    }

    /**
     * Constructs a Gaussian filter.
     *
     * @param radius blur radius in pixels
     */
    public GaussianFilter(String filterName, float radius) {
        super(filterName);
        setRadius(radius);
    }

    /**
     * Sets the radius of the kernel, and hence the amount of blur. The bigger the radius, the longer this filter will take.
     *
     * @param radius the radius of the blur in pixels.
     */
    public void setRadius(float radius) {
        this.radius = radius;
        kernel = makeKernel(radius);
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

        getRGB(src, 0, 0, width, height, inPixels);

        if (radius > 0) {
            int[] outPixels = new int[width * height];
            convolveAndTranspose(kernel, inPixels, outPixels, width, height, premultiplyAlpha, false, pt);
            convolveAndTranspose(kernel, outPixels, inPixels, height, width, false, premultiplyAlpha, pt);
        }

        setRGB(dst, 0, 0, width, height, inPixels);

        finishProgressTracker();

        return dst;
    }

    /**
     * Blur and transpose a block of ARGB pixels.
     *
     * @param kernel     the blur kernel
     * @param inPixels   the input pixels
     * @param outPixels  the output pixels
     * @param width      the width of the pixel array
     * @param height     the height of the pixel array
     * @param alpha      whether to blur the alpha channel
     * @param edgeAction what to do at the edges
     */
    public static void convolveAndTranspose(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height,
                                            boolean premultiply, boolean unpremultiply,
                                            ProgressTracker pt) {
        float[] matrix = kernel.getKernelData(null);
        int cols = kernel.getWidth();
        int cols2 = cols / 2;

        Future<?>[] rowFutures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable rowTask = () -> convolveAndTransposeRow(inPixels, outPixels, width, height, premultiply, unpremultiply, matrix, cols2, finalY);
            rowFutures[y] = ThreadPool.submit(rowTask);
        }

        ThreadPool.waitFor(rowFutures, pt);
    }

    private static void convolveAndTransposeRow(int[] inPixels, int[] outPixels, int width, int height, boolean premultiply, boolean unpremultiply, float[] matrix, int cols2, int y) {
        int index = y;
        int ioffset = y * width;
        for (int x = 0; x < width; x++) {
            float r = 0, g = 0, b = 0, a = 0;
            int moffset = cols2;
            for (int col = -cols2; col <= cols2; col++) {
                float f = matrix[moffset + col];

                if (f != 0) {
                    int ix = x + col;
                    if (ix < 0) {
                        ix = 0;
                    } else if (ix >= width) {
                        ix = width - 1;
                    }
                    int rgb = inPixels[ioffset + ix];
                    int pa = rgb >>> 24;
                    int pr = (rgb >> 16) & 0xFF;
                    int pg = (rgb >> 8) & 0xFF;
                    int pb = rgb & 0xFF;
                    if (premultiply) {
                        float a255 = pa * (1.0f / 255.0f);
                        pr = (int) (pr * a255);
                        pg = (int) (pg * a255);
                        pb = (int) (pb * a255);
                    }
                    a += f * pa;
                    r += f * pr;
                    g += f * pg;
                    b += f * pb;
                }
            }
            if (unpremultiply && a != 0 && a != 255) {
                float f = 255.0f / a;
                r *= f;
                g *= f;
                b *= f;
            }

            int ir = PixelUtils.clamp((int) (r + 0.5));
            int ig = PixelUtils.clamp((int) (g + 0.5));
            int ib = PixelUtils.clamp((int) (b + 0.5));
            int ia = PixelUtils.clamp((int) (a + 0.5));
            outPixels[index] = (ia << 24) | (ir << 16) | (ig << 8) | ib;

            index += height;
        }
    }

    /**
     * Make a Gaussian blur kernel.
     *
     * @param radius the blur radius
     * @return the kernel
     */
    public static Kernel makeKernel(float radius) {
        int r = (int) radius;
        int rows = r * 2 + 1;
        float[] matrix = new float[rows];

        // if radius is < 1.0, return a 1x1 identity kernel.
        if (r == 0) {
            matrix[0] = 1.0f;
            return new Kernel(rows, 1, matrix);
        }

        float sigma = radius / 3.0f;
        float sigma2Sq = 2.0f * sigma * sigma;
        float total = 0.0f;

        // center element
        matrix[r] = 1.0f;  // exp(0) = 1
        total += matrix[r];

        // calculate one side and mirror it
        for (int row = 1; row <= r; row++) {
            float distSq = row * row;

            // no constant multiplier, because it will be normalized anyway
            float value = (float) Math.exp(-distSq / sigma2Sq);

            matrix[r - row] = value; // left side
            matrix[r + row] = value; // right side
            total += 2.0f * value;   // add both sides to the total
        }

        // normalize the kernel so the sum of all elements equals 1.0
        for (int i = 0; i < rows; i++) {
            matrix[i] /= total;
        }

        return new Kernel(rows, 1, matrix);
    }
}
