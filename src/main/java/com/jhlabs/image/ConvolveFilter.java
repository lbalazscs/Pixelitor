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
     * Treat pixels off the edge as zero.
     */
    public static final int ZERO_EDGES = 0;

    /**
     * Clamp pixels off the edge to the nearest edge.
     */
    public static final int CLAMP_EDGES = 1;

    /**
     * Wrap pixels off the edge to the opposite edge.
     */
    public static final int WRAP_EDGES = 2;

    /**
     * The convolution kernel.
     */
    protected Kernel kernel = null;

    /**
     * Whether to premultiply the alpha before convolving.
     */
    protected boolean premultiplyAlpha = true;

    /**
     * What to do at the image edges.
     */
    private int edgeAction = CLAMP_EDGES;

    /**
     * Construct a filter with a null kernel. This is only useful if you're going to change the kernel later on.
     */
    public ConvolveFilter(String filterName) {
        this(new float[9], filterName);
    }

    /**
     * Construct a filter with the given 3x3 kernel.
     *
     * @param matrix an array of 9 floats containing the kernel
     */
    public ConvolveFilter(float[] matrix, String filterName) {
        this(new Kernel(3, 3, matrix), filterName);
    }

    /**
     * Construct a filter with the given kernel.
     *
     * @param rows   the number of rows in the kernel
     * @param cols   the number of columns in the kernel
     * @param matrix an array of rows*cols floats containing the kernel
     */
    public ConvolveFilter(int rows, int cols, float[] matrix, String filterName) {
        this(new Kernel(cols, rows, matrix), filterName);
    }

    /**
     * Construct a filter with the given 3x3 kernel.
     *
     * @param kernel the convolution kernel
     */
    public ConvolveFilter(Kernel kernel, String filterName) {
        super(filterName);
        this.kernel = kernel;
    }

    /**
     * Set the convolution kernel.
     *
     * @param kernel the kernel
     */
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * Set the action to perfomr for pixels off the image edges.
     *
     * @param edgeAction the action
     */
    public void setEdgeAction(int edgeAction) {
        this.edgeAction = edgeAction;
    }

    /**
     * Set whether to premultiply the alpha channel.
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
        convolve(kernel, inPixels, outPixels, width, height, edgeAction);
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
     * @param edgeAction what to do at the edges
     */
    public void convolve(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height, int edgeAction) {
        if (kernel.getHeight() == 1) {
            convolveH(kernel, inPixels, outPixels, width, height, edgeAction);
        } else if (kernel.getWidth() == 1) {
            convolveV(kernel, inPixels, outPixels, width, height, edgeAction);
        } else {
            convolveHV(kernel, inPixels, outPixels, width, height, edgeAction);
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
     * @param edgeAction what to do at the edges
     */
    public void convolveHV(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height, int edgeAction) {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int rows = kernel.getHeight();
        int cols = kernel.getWidth();
        int rows2 = rows / 2;
        int cols2 = cols / 2;

        pt = createProgressTracker(height);

        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0;
                int origPacked = inPixels[offset + x];
                int origAlpha = (origPacked >> 24) & 0xff;

                for (int row = -rows2; row <= rows2; row++) {
                    int iy = y + row;
                    int ioffset;
                    if (0 <= iy && iy < height) {
                        ioffset = iy * width;
                    } else if (edgeAction == CLAMP_EDGES) {
                        ioffset = y * width;
                    } else if (edgeAction == WRAP_EDGES) {
                        ioffset = ((iy + height) % height) * width;
                    } else {
                        continue;
                    }
                    int moffset = cols * (row + rows2) + cols2;
                    for (int col = -cols2; col <= cols2; col++) {
                        float f = matrix[moffset + col];

                        if (f != 0) {
                            int ix = x + col;
                            if (!(0 <= ix && ix < width)) {
                                if (edgeAction == CLAMP_EDGES) {
                                    ix = x;
                                } else if (edgeAction == WRAP_EDGES) {
                                    ix = (x + width) % width;
                                } else {
                                    continue;
                                }
                            }
                            int rgb = inPixels[ioffset + ix];
                            r += f * ((rgb >> 16) & 0xff);
                            g += f * ((rgb >> 8) & 0xff);
                            b += f * (rgb & 0xff);
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
     * @param edgeAction what to do at the edges
     */
    public static void convolveH(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height, int edgeAction) {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int cols = kernel.getWidth();
        int cols2 = cols / 2;

        for (int y = 0; y < height; y++) {
            int ioffset = y * width;
            for (int x = 0; x < width; x++) {
                int origPacked = inPixels[ioffset + x];
                int origAlpha = (origPacked >> 24) & 0xff;
                float r = 0, g = 0, b = 0;
                int moffset = cols2;
                for (int col = -cols2; col <= cols2; col++) {
                    float f = matrix[moffset + col];

                    if (f != 0) {
                        int ix = x + col;
                        if (ix < 0) {
                            if (edgeAction == CLAMP_EDGES) {
                                ix = 0;
                            } else if (edgeAction == WRAP_EDGES) {
                                ix = (x + width) % width;
                            }
                        } else if (ix >= width) {
                            if (edgeAction == CLAMP_EDGES) {
                                ix = width - 1;
                            } else if (edgeAction == WRAP_EDGES) {
                                ix = (x + width) % width;
                            }
                        }
                        int rgb = inPixels[ioffset + ix];
                        r += f * ((rgb >> 16) & 0xff);
                        g += f * ((rgb >> 8) & 0xff);
                        b += f * (rgb & 0xff);
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
     * @param edgeAction what to do at the edges
     */
    public static void convolveV(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height, int edgeAction) {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int rows = kernel.getHeight();
        int rows2 = rows / 2;

        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0;
                int origPacked = inPixels[offset + x];
                int origAlpha = (origPacked >> 24) & 0xff;

                for (int row = -rows2; row <= rows2; row++) {
                    int iy = y + row;
                    int ioffset;
                    if (iy < 0) {
                        if (edgeAction == CLAMP_EDGES) {
                            ioffset = 0;
                        } else if (edgeAction == WRAP_EDGES) {
                            ioffset = ((y + height) % height) * width;
                        } else {
                            ioffset = iy * width;
                        }
                    } else if (iy >= height) {
                        if (edgeAction == CLAMP_EDGES) {
                            ioffset = (height - 1) * width;
                        } else if (edgeAction == WRAP_EDGES) {
                            ioffset = ((y + height) % height) * width;
                        } else {
                            ioffset = iy * width;
                        }
                    } else {
                        ioffset = iy * width;
                    }

                    float f = matrix[row + rows2];

                    if (f != 0) {
                        int rgb = inPixels[ioffset + x];
                        r += f * ((rgb >> 16) & 0xff);
                        g += f * ((rgb >> 8) & 0xff);
                        b += f * (rgb & 0xff);
                    }
                }
                int ir = PixelUtils.clamp((int) (r + 0.5));
                int ig = PixelUtils.clamp((int) (g + 0.5));
                int ib = PixelUtils.clamp((int) (b + 0.5));
                outPixels[index++] = (origAlpha << 24) | (ir << 16) | (ig << 8) | ib;
            }
        }
    }

    @Override
    public String toString() {
        return "Blur/Convolve...";
    }
}
