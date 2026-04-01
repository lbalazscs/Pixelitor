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
 * A filter which applies a convolution kernel to an image.
 *
 * @author Jerry Huxtable
 */
public class ConvolveFilter extends AbstractBufferedImageOp {
    /**
     * The convolution kernel.
     */
    protected Kernel kernel = null;

    /**
     * Whether to premultiply the alpha before convolving.
     */
    protected boolean premultiplyAlpha = true;

    /**
     * Constructs a ConvolveFilter with an empty 3x3 kernel (all zeros).
     */
    public ConvolveFilter(String filterName) {
        this(filterName, new float[9]);
    }

    /**
     * Constructs a filter with the given 3x3 kernel.
     *
     * @param matrix an array of 9 floats containing the kernel
     */
    public ConvolveFilter(String filterName, float[] matrix) {
        this(new Kernel(3, 3, matrix), filterName);
    }

    /**
     * Constructs a filter with the given kernel.
     *
     * @param rows   the number of rows in the kernel
     * @param cols   the number of columns in the kernel
     * @param matrix an array of rows*cols floats containing the kernel
     */
    public ConvolveFilter(int rows, int cols, float[] matrix, String filterName) {
        this(new Kernel(cols, rows, matrix), filterName);
    }

    /**
     * Constructs a filter with the given 3x3 kernel.
     *
     * @param kernel the convolution kernel
     */
    public ConvolveFilter(Kernel kernel, String filterName) {
        super(filterName);
        this.kernel = kernel;
    }

    /**
     * Sets the convolution kernel.
     *
     * @param kernel the kernel
     */
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * Sets whether to premultiply the alpha channel.
     *
     * @param premultiplyAlpha true to premultiply the alpha
     */
    public void setPremultiplyAlpha(boolean premultiplyAlpha) {
        this.premultiplyAlpha = premultiplyAlpha;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] inPixels = new int[width * height];
        int[] outPixels = new int[width * height];
        getRGB(src, 0, 0, width, height, inPixels);

        if (premultiplyAlpha) {
            ImageMath.premultiply(inPixels, 0, inPixels.length);
        }
        convolve(kernel, inPixels, outPixels, width, height);
        if (premultiplyAlpha) {
            ImageMath.unpremultiply(outPixels, 0, outPixels.length);
        }

        setRGB(dst, 0, 0, width, height, outPixels);
        return dst;
    }

    /**
     * Convolve a block of pixels.
     *
     * @param kernel     the kernel
     * @param inPixels   the input pixels
     * @param outPixels  the output pixels
     * @param width      the width
     * @param height     the height
     */
    private void convolve(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height) {
        if (kernel.getHeight() == 1) {
            convolveH(kernel, inPixels, outPixels, width, height);
        } else if (kernel.getWidth() == 1) {
            convolveV(kernel, inPixels, outPixels, width, height);
        } else {
            convolveHV(kernel, inPixels, outPixels, width, height);
        }
    }

    /**
     * Convolve with a 2D kernel.
     *
     * @param kernel     the kernel
     * @param inPixels   the input pixels
     * @param outPixels  the output pixels
     * @param width      the width
     * @param height     the height
     */
    private void convolveHV(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height) {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int rows = kernel.getHeight();
        int cols = kernel.getWidth();
        int rowRadius = rows / 2;
        int colRadius = cols / 2;

        pt = createProgressTracker(height);

        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0;
                int origPacked = inPixels[offset + x];
                int origAlpha = origPacked >>> 24;

                for (int row = -rowRadius; row <= rowRadius; row++) {
                    int iy = y + row;
                    int ioffset;
                    if (0 <= iy && iy < height) {
                        ioffset = iy * width;
                    } else {
                        ioffset = y * width;
                    }
                    int moffset = cols * (row + rowRadius) + colRadius;
                    for (int col = -colRadius; col <= colRadius; col++) {
                        float weight = matrix[moffset + col];

                        if (weight != 0) {
                            int ix = x + col;
                            if (!(0 <= ix && ix < width)) {
                                ix = x;
                            }
                            int rgb = inPixels[ioffset + ix];
                            r += weight * ((rgb >> 16) & 0xFF);
                            g += weight * ((rgb >> 8) & 0xFF);
                            b += weight * (rgb & 0xFF);
                        }
                    }
                }
                int ir = PixelUtils.clamp((int) (r + 0.5));
                int ig = PixelUtils.clamp((int) (g + 0.5));
                int ib = PixelUtils.clamp((int) (b + 0.5));
                outPixels[index++] = (origAlpha << 24) | (ir << 16) | (ig << 8) | ib;
            }
            pt.unitDone();
        }
        finishProgressTracker();
    }

    /**
     * Convolve with a kernel consisting of one row.
     *
     * @param kernel     the kernel
     * @param inPixels   the input pixels
     * @param outPixels  the output pixels
     * @param width      the width
     * @param height     the height
     */
    private static void convolveH(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height) {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int cols = kernel.getWidth();
        int colRadius = cols / 2;

        for (int y = 0; y < height; y++) {
            int ioffset = y * width;
            for (int x = 0; x < width; x++) {
                int origPacked = inPixels[ioffset + x];
                int origAlpha = origPacked >>> 24;
                float r = 0, g = 0, b = 0;
                int moffset = colRadius;
                for (int col = -colRadius; col <= colRadius; col++) {
                    float weight = matrix[moffset + col];

                    if (weight != 0) {
                        int ix = x + col;
                        if (ix < 0) {
                            ix = 0;
                        } else if (ix >= width) {
                            ix = width - 1;
                        }
                        int rgb = inPixels[ioffset + ix];
                        r += weight * ((rgb >> 16) & 0xFF);
                        g += weight * ((rgb >> 8) & 0xFF);
                        b += weight * (rgb & 0xFF);
                    }
                }
                int ir = PixelUtils.clamp((int) (r + 0.5));
                int ig = PixelUtils.clamp((int) (g + 0.5));
                int ib = PixelUtils.clamp((int) (b + 0.5));
                outPixels[index++] = (origAlpha << 24) | (ir << 16) | (ig << 8) | ib;
            }
        }
    }

    /**
     * Convolve with a kernel consisting of one column.
     *
     * @param kernel     the kernel
     * @param inPixels   the input pixels
     * @param outPixels  the output pixels
     * @param width      the width
     * @param height     the height
     */
    private static void convolveV(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height) {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int rows = kernel.getHeight();
        int rowRadius = rows / 2;

        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0;
                int origPacked = inPixels[offset + x];
                int origAlpha = origPacked >>> 24;

                for (int row = -rowRadius; row <= rowRadius; row++) {
                    int iy = y + row;
                    int ioffset;
                    if (iy < 0) {
                        ioffset = 0;
                    } else if (iy >= height) {
                        ioffset = (height - 1) * width;
                    } else {
                        ioffset = iy * width;
                    }

                    float weight = matrix[row + rowRadius];

                    if (weight != 0) {
                        int rgb = inPixels[ioffset + x];
                        r += weight * ((rgb >> 16) & 0xFF);
                        g += weight * ((rgb >> 8) & 0xFF);
                        b += weight * (rgb & 0xFF);
                    }
                }
                int ir = PixelUtils.clamp((int) (r + 0.5));
                int ig = PixelUtils.clamp((int) (g + 0.5));
                int ib = PixelUtils.clamp((int) (b + 0.5));
                outPixels[index++] = (origAlpha << 24) | (ir << 16) | (ig << 8) | ib;
            }
        }
    }
}
