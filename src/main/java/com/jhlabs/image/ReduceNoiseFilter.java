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
 * A filter which reduces noise by looking at each pixel's 8 neighbors,
 * and if it's a minimum or maximum, replacing it by the next
 * minimum or maximum of the neighbors.
 */
public class ReduceNoiseFilter extends WholeImageFilter {
    public ReduceNoiseFilter(String filterName) {
        super(filterName);
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        int index = 0;
        int[] r = new int[9];
        int[] g = new int[9];
        int[] b = new int[9];
        int[] outPixels = new int[width * height];

        pt = createProgressTracker(height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int k = 0;
                int irgb = inPixels[index];
                int ir = (irgb >> 16) & 0xFF;
                int ig = (irgb >> 8) & 0xFF;
                int ib = irgb & 0xFF;
                for (int dy = -1; dy <= 1; dy++) {
                    int iy = y + dy;
                    if (0 <= iy && iy < height) {
                        int ioffset = iy * width;
                        for (int dx = -1; dx <= 1; dx++) {
                            int ix = x + dx;
                            if (0 <= ix && ix < width) {
                                int rgb = inPixels[ioffset + ix];
                                r[k] = (rgb >> 16) & 0xFF;
                                g[k] = (rgb >> 8) & 0xFF;
                                b[k] = rgb & 0xFF;
                            } else {
                                r[k] = ir;
                                g[k] = ig;
                                b[k] = ib;
                            }
                            k++;
                        }
                    } else {
                        for (int dx = -1; dx <= 1; dx++) {
                            r[k] = ir;
                            g[k] = ig;
                            b[k] = ib;
                            k++;
                        }
                    }
                }
                outPixels[index] = (irgb & 0xFF_00_00_00) | (smooth(r) << 16) | (smooth(g) << 8) | smooth(b);
                index++;
            }
            pt.unitDone();
        }

        finishProgressTracker();

        return outPixels;
    }

    /**
     * Smooths a center pixel value by clamping it to the range of its 8 neighbors.
     */
    private static int smooth(int[] v) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                min = Math.min(min, v[i]);
                max = Math.max(max, v[i]);
            }
        }

        return Math.clamp(v[4], min, max);
    }
}
