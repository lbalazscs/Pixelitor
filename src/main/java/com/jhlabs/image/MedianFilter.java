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
 * A filter which performs a 3x3 median operation. Useful for removing dust and noise.
 */
public class MedianFilter extends WholeImageFilter {
    private static final int KERNEL_SIZE = 9;

    public MedianFilter(String filterName) {
        super(filterName);
    }

    // finds the index of the median color
    private static int rgbMedian(int[] r, int[] g, int[] b) {
        // sorting channels independently would mix up colors =>
        // the median is defined as the pixel with the minimum
        // total L1 color distance to all other pixels

        // reusing this array saves no time
        int[] sums = new int[KERNEL_SIZE];

        // compute unique pairs only
        for (int i = 0; i < KERNEL_SIZE - 1; i++) {
            for (int j = i + 1; j < KERNEL_SIZE; j++) {
                int dist = Math.abs(r[i] - r[j])
                    + Math.abs(g[i] - g[j])
                    + Math.abs(b[i] - b[j]);
                sums[i] += dist;
                sums[j] += dist;
            }
        }

        int min = Integer.MAX_VALUE;
        int medianIndex = 0;
        for (int i = 0; i < sums.length; i++) {
            if (sums[i] < min) {
                min = sums[i];
                medianIndex = i;
            }
        }
        return medianIndex;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        int[] outPixels = new int[width * height];

        pt = createProgressTracker(height);
        Future<?>[] rowFutures = new Future[height];

        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable rowTask = () -> processRow(width, height, inPixels, finalY, outPixels);
            rowFutures[y] = ThreadPool.submit(rowTask);
        }

        ThreadPool.waitFor(rowFutures, pt);
        finishProgressTracker();
        return outPixels;
    }

    private static void processRow(int width, int height, int[] inPixels, int finalY, int[] outPixels) {
        // local instances for each thread
        int[] argb = new int[KERNEL_SIZE];
        int[] r = new int[KERNEL_SIZE];
        int[] g = new int[KERNEL_SIZE];
        int[] b = new int[KERNEL_SIZE];

        // precalculate valid adjacent row offsets for the entire row width
        int minDy = Math.max(-1, -finalY);
        int maxDy = Math.min(1, height - 1 - finalY);
        int[] rowOffsets = new int[maxDy - minDy + 1];
        for (int dy = minDy; dy <= maxDy; dy++) {
            rowOffsets[dy - minDy] = (finalY + dy) * width;
        }

        for (int x = 0; x < width; x++) {
            int k = 0;

            for (int ioffset : rowOffsets) {
                for (int dx = -1; dx <= 1; dx++) {
                    int ix = x + dx;
                    if (ix < 0 || ix >= width) {
                        continue;
                    }
                    // collect the neighbor's pixel components
                    int rgb = inPixels[ioffset + ix];
                    argb[k] = rgb;
                    r[k] = (rgb >> 16) & 0xFF;
                    g[k] = (rgb >> 8) & 0xFF;
                    b[k] = rgb & 0xFF;
                    k++;
                }
            }
            // pixels outside the image boundary were skipped, and the
            // remaining slots are padded with black (0xFF_00_00_00)
            while (k < KERNEL_SIZE) {
                argb[k] = 0xFF_00_00_00;
                r[k] = g[k] = b[k] = 0;
                k++;
            }
            outPixels[finalY * width + x] = argb[rgbMedian(r, g, b)];
        }
    }
}
