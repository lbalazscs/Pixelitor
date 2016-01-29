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

import java.awt.Rectangle;
import java.util.concurrent.Future;

/**
 * A filter which produces a "oil-painting" effect.
 *
 * Laszlo: the original algorithm from Jerry had three R,G,B histograms,
 * but this led to strange artifacts, so I changed it according to
 * http://supercomputingblog.com/graphics/oil-painting-algorithm/
 * to use only one intensity-histogram.
 */
public class OilFilter extends WholeImageFilter {
    private int rangeX = 3;
    private int rangeY = 3;
    private int levels = 256;

    public OilFilter(String filterName) {
        super(filterName);
    }

    public void setRangeX(int rangeX) {
        this.rangeX = rangeX;
    }

    public void setRangeY(int rangeY) {
        this.rangeY = rangeY;
    }

    /**
     * Set the number of levels for the effect.
     *
     * @param levels the number of levels
     * @see #getLevels
     */
    public void setLevels(int levels) {
        this.levels = levels;
    }

    /**
     * Get the number of levels for the effect.
     *
     * @return the number of levels
     * @see #setLevels
     */
    public int getLevels() {
        return levels;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels, Rectangle transformedSpace) {
        int[] outPixels = new int[width * height];

        pt = createProgressTracker(height);
        Future<?>[] futures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable lineTask = () -> calculateLine(width, height, inPixels, outPixels, finalY);
            futures[y] = ThreadPool.submit(lineTask);
        }

        ThreadPool.waitForFutures(futures, pt);
        finishProgressTracker();

        return outPixels;
    }

    private void calculateLine(int width, int height, int[] inPixels, int[] outPixels, int y) {
        int index = y * width;
        int[] rTotal = new int[levels];
        int[] gTotal = new int[levels];
        int[] bTotal = new int[levels];
        int[] histogram = new int[levels];
        for (int x = 0; x < width; x++) {
            // The idea is that for each pixel the most frequently occuring
            // intensity value in its neighborhood is found, and this will determine
            // new value of the pixel
            for (int i = 0; i < levels; i++) {
                histogram[i] = rTotal[i] = gTotal[i] = bTotal[i] = 0;
            }

            // For each pixel, all pixels within the brush size will have to be examined.
            for (int row = -rangeY; row <= rangeY; row++) {
                int iy = y + row;
                int ioffset;
                if (0 <= iy && iy < height) {
                    ioffset = iy * width;
                    for (int col = -rangeX; col <= rangeX; col++) {
                        int ix = x + col;
                        if (0 <= ix && ix < width) {
                            // examining each neighbor pixel which is within brush size
                            int rgb = inPixels[ioffset + ix];
                            int r = (rgb >> 16) & 0xff;
                            int g = (rgb >> 8) & 0xff;

                            int b = rgb & 0xff;
                            int intensity = (r + g + b) / 3;
                            // For each sub-pixel, calculate the intensity, and determine
                            // which intensity bin that intensity number falls into
                            int intensityI = intensity * levels / 256;
                            histogram[intensityI]++;

                            // Also maintain the total red, green, and blue values for each bin,
                            // later these may be used to determine the final value of the pixel.
                            rTotal[intensityI] += r;
                            gTotal[intensityI] += g;
                            bTotal[intensityI] += b;
                        }
                    }
                }
            }

            // Determine which intensity bin has the most number of pixels in it.
            int maxIndex = 0;
            int curMax = 0;
            for (int i = 0; i < levels; i++) {
                if (histogram[i] > curMax) {
                    curMax = histogram[i];
                    maxIndex = i;
                }
            }

            // The final color of the pixel is the average of the colors
            // in the bin with the highest number of pixels
            int r = rTotal[maxIndex] / curMax;
            int g = gTotal[maxIndex] / curMax;
            int b = bTotal[maxIndex] / curMax;

//                r = PixelUtils.clamp(r);
//                g = PixelUtils.clamp(g);
//                b = PixelUtils.clamp(b);

            outPixels[index] = (inPixels[index] & 0xff000000) | (r << 16) | (g << 8) | b;
            index++;
        }
    }

    public String toString() {
		return "Stylize/Oil...";
	}

}

