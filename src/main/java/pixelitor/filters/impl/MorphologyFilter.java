/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.Morphology;

import java.awt.Rectangle;

/**
 * The implementation of the {@link Morphology} filter.
 */
public class MorphologyFilter extends WholeImageFilter {
    private int iterations = 1;

    public static final int OP_ERODE = 1;  // reduces bright areas
    public static final int OP_DILATE = 2; // expands bright areas
    private int op;

    public static final int KERNEL_DIAMOND = 3; // includes cross-shaped neighboring pixels
    public static final int KERNEL_SQUARE = 4; // includes all 8 surrounding pixels
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
        int numPixels = inPixels.length;
        short[] inA = new short[numPixels];
        short[] inR = new short[numPixels];
        short[] inG = new short[numPixels];
        short[] inB = new short[numPixels];
        int[] outPixels = new int[width * height];

        pt = createProgressTracker(iterations);
        for (int it = 0; it < iterations; it++) {
            if (it > 0) {
                // use output pixels from previous iteration as input for this iteration
                System.arraycopy(outPixels, 0, inPixels, 0, numPixels);
            }

            // extract the color channels
            for (int i = 0; i < numPixels; i++) {
                int rgb = inPixels[i];
                inA[i] = (short) ((rgb >> 24) & 0xFF);
                inR[i] = (short) ((rgb >> 16) & 0xFF);
                inG[i] = (short) ((rgb >> 8) & 0xFF);
                inB[i] = (short) (rgb & 0xFF);
            }

            int index = 0; // the index of the processed pixel
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // the final alpha, red, green, blue values
                    short a = 0xFF;
                    short r = 0xFF;
                    short g = 0xFF;
                    short b = 0xFF;

                    if (op == OP_DILATE) {
                        // initialize them with 0
                        r = 0;
                        g = 0;
                        b = 0;
                    }

                    // examine neighboring pixels
                    for (int dy = -1; dy <= 1; dy++) {
                        int ny = y + dy;
                        if (0 <= ny && ny < height) {
                            int xOffset = ny * width;
                            for (int dx = -1; dx <= 1; dx++) {
                                if (kernel == KERNEL_DIAMOND) {
                                    // ignore the corner neighbours
                                    if (dx == dy && dx != 0) {
                                        continue;
                                    }
                                    if (dx == -dy && dx != 0) {
                                        continue;
                                    }
                                }

                                int nx = x + dx;
                                if (0 <= nx && nx < width) {
                                    int neighborIndex = xOffset + nx;

                                    if (op == OP_ERODE) {
                                        a = min(a, inA[neighborIndex]);
                                        r = min(r, inR[neighborIndex]);
                                        g = min(g, inG[neighborIndex]);
                                        b = min(b, inB[neighborIndex]);
                                    } else {
                                        a = max(a, inA[neighborIndex]);
                                        r = max(r, inR[neighborIndex]);
                                        g = max(g, inG[neighborIndex]);
                                        b = max(b, inB[neighborIndex]);
                                    }
                                }
                            }
                        }
                    }
                    outPixels[index++] = a << 24 | r << 16 | g << 8 | b;
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
}

