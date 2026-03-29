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

import java.util.concurrent.Future;

/**
 * A filter which produces a "oil-painting" effect.
 *
 * The original JH Labs implementation had three R,G,B histograms,
 * but this led to strange artifacts. The current implementation follows
 * http://supercomputingblog.com/graphics/oil-painting-algorithm/
 * to use only one intensity-histogram.
 */
public class OilFilter extends WholeImageFilter {
    private final int rangeX;
    private final int rangeY;
    private final int levels;

    /**
     * Constructs an OilFilter with the specified parameters.
     *
     * @param filterName the name of the filter
     * @param rangeX     the horizontal radius of the brush in pixels
     * @param rangeY     the vertical radius of the brush in pixels
     * @param levels     the number of intensity levels used when building
     *                   the brightness histogram; higher values preserve
     *                   more colour detail, lower values increase the
     *                   painterly effect
     */
    public OilFilter(String filterName, int rangeX, int rangeY, int levels) {
        super(filterName);

        this.rangeX = rangeX;
        this.rangeY = rangeY;
        this.levels = levels;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        int[] outPixels = new int[width * height];
        short[] bins = new short[width * height];

        // precompute bins to avoid recalculating in the tight inner loops
        for (int i = 0; i < inPixels.length; i++) {
            int rgb = inPixels[i];
            bins[i] = (short) (ImageMath.calcLuminanceInt(rgb) * levels / 256);
        }

        pt = createProgressTracker(height);
        Future<?>[] rowFutures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable rowTask = () -> processRow(width, height, inPixels, outPixels, finalY, bins);
            rowFutures[y] = ThreadPool.submit(rowTask);
        }

        ThreadPool.waitFor(rowFutures, pt);
        finishProgressTracker();

        return outPixels;
    }

    // Looks at all the pixels in the neighborhood and finds the
    // most common brightness value. Then sets the current pixel
    // to the average color of pixels that have that brightness.
    private void processRow(int width, int height, int[] inPixels, int[] outPixels, int y, short[] bins) {
        // how many pixels of each intensity level are in the window
        int[] histogram = new int[levels];

        // sum of R, G, B values for each intensity bin
        int[] rTotal = new int[levels];
        int[] gTotal = new int[levels];
        int[] bTotal = new int[levels];

        // pre-calculate valid row bounds for this y
        int rowStart = Math.max(-rangeY, -y);
        int rowEnd = Math.min(rangeY, height - 1 - y);

        // initialize the histogram for the first pixel (x = 0)
        for (int row = rowStart; row <= rowEnd; row++) {
            int iy = y + row;
            for (int col = -rangeX; col <= rangeX; col++) {
                int ix = col;
                if (ix >= 0 && ix < width) {
                    int index = iy * width + ix;
                    int rgb = inPixels[index];
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    int bin = bins[index];
                    histogram[bin] += 1;
                    rTotal[bin] += r;
                    gTotal[bin] += g;
                    bTotal[bin] += b;
                }
            }
        }

        int rowOffset = y * width;
        for (int x = 0; x < width; x++) {
            // find the most frequent intensity and set the output pixel
            int maxIndex = 0;
            int curMax = 0;
            for (int i = 0; i < levels; i++) {
                if (histogram[i] > curMax) {
                    curMax = histogram[i];
                    maxIndex = i;
                }
            }

            int r = rTotal[maxIndex] / curMax;
            int g = gTotal[maxIndex] / curMax;
            int b = bTotal[maxIndex] / curMax;
            outPixels[rowOffset + x] = (inPixels[rowOffset + x] & 0xFF_00_00_00) | (r << 16) | (g << 8) | b;

            // slide the window for the next pixel (x + 1)
            if (x + 1 < width) {
                // subtract the column that is leaving the window
                int leavingColX = x - rangeX;
                if (leavingColX >= 0) {
                    for (int row = rowStart; row <= rowEnd; row++) {
                        int index = (y + row) * width + leavingColX;
                        int rgb = inPixels[index];
                        int r1 = (rgb >> 16) & 0xFF;
                        int g1 = (rgb >> 8) & 0xFF;
                        int b1 = rgb & 0xFF;

                        int bin = bins[index];
                        histogram[bin] += -1;
                        rTotal[bin] -= r1;
                        gTotal[bin] -= g1;
                        bTotal[bin] -= b1;
                    }
                }

                // add the column that is entering the window
                int enteringColX = x + 1 + rangeX;
                if (enteringColX < width) {
                    for (int row = rowStart; row <= rowEnd; row++) {
                        int index = (y + row) * width + enteringColX;
                        int rgb = inPixels[index];
                        int r1 = (rgb >> 16) & 0xFF;
                        int g1 = (rgb >> 8) & 0xFF;
                        int b1 = rgb & 0xFF;

                        int bin = bins[index];
                        histogram[bin] += 1;
                        rTotal[bin] += r1;
                        gTotal[bin] += g1;
                        bTotal[bin] += b1;
                    }
                }
            }
        }
    }
}
