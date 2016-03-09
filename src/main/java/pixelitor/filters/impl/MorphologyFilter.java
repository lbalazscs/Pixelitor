/*
 * Copyright 2016 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters.impl;

import com.jhlabs.image.WholeImageFilter;

import java.awt.Rectangle;

/**
 * A morphology filter
 */
public class MorphologyFilter extends WholeImageFilter {
    private int iterations = 1;

    public static final int OP_ERODE = 1;
    public static final int OP_DILATE = 2;
    private int op;

    public static final int KERNEL_DIAMOND = 3;
    public static final int KERNEL_SQUARE = 4;
    private int kernel;

    public MorphologyFilter(String filterName) {
        super(filterName);
    }

    public void setKernel(int kernel) {
        this.kernel = kernel;
    }

    public void setOp(int op) {
        this.op = op;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels, Rectangle transformedSpace) {
        int[] outPixels = new int[width * height];
        int numPixels = inPixels.length;
        short[] inA = new short[numPixels];
        short[] inR = new short[numPixels];
        short[] inG = new short[numPixels];
        short[] inB = new short[numPixels];

        pt = createProgressTracker(iterations);
        for (int it = 0; it < iterations; it++) {
            if (it > 0) {
                System.arraycopy(outPixels, 0, inPixels, 0, numPixels);
            }
            for (int i = 0; i < numPixels; i++) {
                int rgb = inPixels[i];
                inA[i] = (short) ((rgb >> 24) & 0xff);
                inR[i] = (short) ((rgb >> 16) & 0xff);
                inG[i] = (short) ((rgb >> 8) & 0xff);
                inB[i] = (short) (rgb & 0xff);
            }

            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    short a = 0xff;
                    short r = 0xff;
                    short g = 0xff;
                    short b = 0xff;

                    if (op == OP_DILATE) {
                        r = 0;
                        g = 0;
                        b = 0;
                    }

                    for (int dy = -1; dy <= 1; dy++) {
                        int iy = y + dy;
                        if (0 <= iy && iy < height) {
                            int xOffset = iy * width;
                            for (int dx = -1; dx <= 1; dx++) {
                                if (kernel == KERNEL_DIAMOND) {
                                    if (dx == dy && dx != 0) {
                                        continue;
                                    }
                                    if (dx == -dy && dx != 0) {
                                        continue;
                                    }
                                }

                                int ix = x + dx;
                                if (0 <= ix && ix < width) {
                                    int comparedIndex = xOffset + ix;

                                    if (op == OP_ERODE) {
                                        a = min(a, inA[comparedIndex]);
                                        r = min(r, inR[comparedIndex]);
                                        g = min(g, inG[comparedIndex]);
                                        b = min(b, inB[comparedIndex]);
                                    } else {
                                        a = max(a, inA[comparedIndex]);
                                        r = max(r, inR[comparedIndex]);
                                        g = max(g, inG[comparedIndex]);
                                        b = max(b, inB[comparedIndex]);
                                    }
                                }
                            }
                        }
                    }
                    outPixels[index++] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
            pt.unitDone();
        }
        finishProgressTracker();
        return outPixels;
    }

    private static short min(short a, short b) {
        return (a <= b) ? a : b;
    }

    private static short max(short a, short b) {
        return (a >= b) ? a : b;
    }

    public String toString() {
        return "Blur/Minimum";
    }
}

