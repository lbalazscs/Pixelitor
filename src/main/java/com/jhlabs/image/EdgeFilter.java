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

/**
 * An edge-detection filter.
 */
public class EdgeFilter extends WholeImageFilter {
    private static final float SQRT_2 = (float) Math.sqrt(2);

    // Roberts cross vertical and horizontal edge detection matrices
    // https://en.wikipedia.org/wiki/Roberts_cross
    public static final float[] ROBERTS_V = {
        0, 0, -1,
        0, 1, 0,
        0, 0, 0,
    };
    public static final float[] ROBERTS_H = {
        -1, 0, 0,
        0, 1, 0,
        0, 0, 0,
    };

    // Prewitt vertical and horizontal edge detection matrices
    // https://en.wikipedia.org/wiki/Prewitt_operator
    public static final float[] PREWITT_V = {
        -1, 0, 1,
        -1, 0, 1,
        -1, 0, 1,
    };
    public static final float[] PREWITT_H = {
        -1, -1, -1,
        0, 0, 0,
        1, 1, 1,
    };

    // Sobel vertical and horizontal edge detection matrices
    // https://en.wikipedia.org/wiki/Sobel_operator
    public static final float[] SOBEL_V = {
        -1, 0, 1,
        -2, 0, 2,
        -1, 0, 1,
    };
    public static final float[] SOBEL_H = {
        -1, -2, -1,
        0, 0, 0,
        1, 2, 1,
    };

    // Frei-Chen vertical and horizontal edge detection matrices
    public static final float[] FREI_CHEN_V = {
        -1, 0, 1,
        -SQRT_2, 0, SQRT_2,
        -1, 0, 1,
    };
    public static final float[] FREI_CHEN_H = {
        -1, -SQRT_2, -1,
        0, 0, 0,
        1, SQRT_2, 1,
    };

    public static final int CHANNEL_RGB = 0;
    public static final int CHANNEL_LUMINANCE = 1;

    private int channel = CHANNEL_RGB;

    private float[] vEdgeMatrix = SOBEL_V;
    private float[] hEdgeMatrix = SOBEL_H;

    public EdgeFilter(String filterName) {
        super(filterName);
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public void setVEdgeMatrix(float[] vEdgeMatrix) {
        this.vEdgeMatrix = vEdgeMatrix;
    }

    public void setHEdgeMatrix(float[] hEdgeMatrix) {
        this.hEdgeMatrix = hEdgeMatrix;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        pt = createProgressTracker(height);

        int[] outPixels = switch (channel) {
            case CHANNEL_LUMINANCE -> findEdgesLuma(width, height, inPixels);
            case CHANNEL_RGB -> findEdgesRGB(width, height, inPixels);
            default -> throw new IllegalStateException("channel = " + channel);
        };

        finishProgressTracker();
        return outPixels;
    }

    private int[] findEdgesLuma(int width, int height, int[] inPixels) {
        int[] outPixels = new int[width * height];
        int index = 0;

        // pre-process to get luma values
        float[] luma = ImageMath.calcLuminance(inPixels);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float ih = 0, iv = 0;
                int matrixIndex = 0;

                // convolve the 3x3 neighborhood around the current pixel
                for (int row = -1; row <= 1; row++) {
                    int iy = Math.clamp(y + row, 0, height - 1);
                    int ioffset = iy * width;

                    for (int col = -1; col <= 1; col++) {
                        int ix = Math.clamp(x + col, 0, width - 1);

                        float h = hEdgeMatrix[matrixIndex];
                        float v = vEdgeMatrix[matrixIndex];
                        matrixIndex++;

                        float pixelLuma = luma[ioffset + ix];
                        ih += h * pixelLuma;
                        iv += v * pixelLuma;
                    }
                }

                int i = (int) (Math.sqrt(ih * ih + iv * iv) / 1.8);
                i = PixelUtils.clamp(i);

                int a = inPixels[index] & 0xFF_00_00_00;
                outPixels[index++] = a | (i << 16) | (i << 8) | i;
            }
            pt.unitDone();
        }
        return outPixels;
    }

    private int[] findEdgesRGB(int width, int height, int[] inPixels) {
        int[] outPixels = new int[width * height];
        int index = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rh = 0, gh = 0, bh = 0;
                int rv = 0, gv = 0, bv = 0;
                int a = inPixels[index] & 0xFF_00_00_00;
                int matrixIndex = 0;

                // convolve the 3x3 neighborhood around the current pixel
                for (int row = -1; row <= 1; row++) {
                    int iy = Math.clamp(y + row, 0, height - 1);
                    int ioffset = iy * width;

                    for (int col = -1; col <= 1; col++) {
                        int ix = Math.clamp(x + col, 0, width - 1);

                        float h = hEdgeMatrix[matrixIndex];
                        float v = vEdgeMatrix[matrixIndex];
                        matrixIndex++;

                        int rgb = inPixels[ioffset + ix];

                        int r = (rgb & 0xFF_00_00) >> 16;
                        int g = (rgb & 0x00_FF_00) >> 8;
                        int b = rgb & 0x00_00_FF;

                        rh += (int) (h * r);
                        gh += (int) (h * g);
                        bh += (int) (h * b);
                        rv += (int) (v * r);
                        gv += (int) (v * g);
                        bv += (int) (v * b);
                    }
                }

                int r = (int) (Math.sqrt(rh * rh + rv * rv) / 1.8);
                int g = (int) (Math.sqrt(gh * gh + gv * gv) / 1.8);
                int b = (int) (Math.sqrt(bh * bh + bv * bv) / 1.8);

                r = PixelUtils.clamp(r);
                g = PixelUtils.clamp(g);
                b = PixelUtils.clamp(b);

                outPixels[index++] = a | (r << 16) | (g << 8) | b;
            }
            pt.unitDone();
        }
        return outPixels;
    }
}
