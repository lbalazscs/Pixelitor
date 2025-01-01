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
    public static final float R2 = (float) Math.sqrt(2);

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
            -R2, 0, R2,
            -1, 0, 1,
    };
    public static float[] FREI_CHEN_H = {
            -1, -R2, -1,
            0, 0, 0,
            1, R2, 1,
    };

    private float[] vEdgeMatrix = SOBEL_V;
    private float[] hEdgeMatrix = SOBEL_H;

    public EdgeFilter(String filterName) {
        super(filterName);
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

        int index = 0;
        int[] outPixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = 0, g = 0, b = 0;
                int rh = 0, gh = 0, bh = 0;
                int rv = 0, gv = 0, bv = 0;
                int a = inPixels[y * width + x] & 0xff000000;

                // convolve the 3x3 neighborhood around the current pixel
                for (int row = -1; row <= 1; row++) {
                    int iy = y + row;
                    int ioffset;
                    if (0 <= iy && iy < height) {
                        ioffset = iy * width;
                    } else {
                        ioffset = y * width;
                    }
                    int moffset = 3 * (row + 1) + 1;
                    for (int col = -1; col <= 1; col++) {
                        int ix = x + col;
                        if (!(0 <= ix && ix < width)) {
                            ix = x;
                        }
                        int rgb = inPixels[ioffset + ix];
                        float h = hEdgeMatrix[moffset + col];
                        float v = vEdgeMatrix[moffset + col];

                        r = (rgb & 0xff0000) >> 16;
                        g = (rgb & 0x00ff00) >> 8;
                        b = rgb & 0x0000ff;
                        rh += (int) (h * r);
                        gh += (int) (h * g);
                        bh += (int) (h * b);
                        rv += (int) (v * r);
                        gv += (int) (v * g);
                        bv += (int) (v * b);
                    }
                }
                r = (int) (Math.sqrt(rh * rh + rv * rv) / 1.8);
                g = (int) (Math.sqrt(gh * gh + gv * gv) / 1.8);
                b = (int) (Math.sqrt(bh * bh + bv * bv) / 1.8);
                r = PixelUtils.clamp(r);
                g = PixelUtils.clamp(g);
                b = PixelUtils.clamp(b);
                outPixels[index++] = a | (r << 16) | (g << 8) | b;
            }
            pt.unitDone();
        }
        finishProgressTracker();
        return outPixels;
    }

    @Override
    public String toString() {
        return "Edges/Detect Edges";
    }
}
