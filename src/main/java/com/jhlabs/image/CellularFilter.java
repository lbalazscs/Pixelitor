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
 * A variant of Worley noise that calculates distances to the three
 * nearest points and uses a weighted combination of these distances.
 */
public class CellularFilter extends CellSearchFilter {
    private final Colormap colormap;
    private final float[] coefficients; // weights of F1, F2, F3

    /**
     * Constructs a new {@code CellularFilter}.
     *
     * @param filterName the name of the filter
     * @param scale      the texture scale
     * @param stretch    the texture stretch factor
     * @param angle      the texture angle in radians
     * @param gridType   the grid type defining feature point placement
     * @param randomness the randomness factor for grid point placement
     * @param colormap   the colormap to be used for the filter, or {@code null} for grayscale
     * @param f1         the weight coefficient for the distance to the nearest point (F1)
     * @param f2         the weight coefficient for the distance to the second-nearest point (F2)
     * @param f3         the weight coefficient for the distance to the third-nearest point (F3)
     */
    public CellularFilter(String filterName,
                          float scale,
                          float stretch,
                          float angle,
                          GridType gridType,
                          float randomness,
                          Colormap colormap,
                          float f1,
                          float f2,
                          float f3) {
        super(filterName, scale, stretch, angle, gridType, randomness);

        this.colormap = colormap;
        this.coefficients = new float[]{f1, f2, f3};
    }

    @Override
    public int genPixel(int x, int y, int[] inPixels, int width, int height, int needed) {
        float nx = toNoiseX(x, y);
        float ny = toNoiseY(x, y);
        float f = 2 * evaluate(nx, ny, needed);

        if (colormap != null) {
            return colormap.getColor(f);
        }

        int v = PixelUtils.clamp((int) (f * 255));
        return 0xFF_00_00_00 | (v << 16) | (v << 8) | v;
    }

    // nx, ny are noise-space coordinates
    private float evaluate(float nx, float ny, int needed) {
        Point[] results = resultsTL.get();
        searchNeighborhood(nx, ny, results, needed);

        // at this point results is guaranteed to hold the needed smallest distances encountered,
        // so we can now calculate the weighted combination of these distances
        float f = coefficients[0] * results[0].distance;
        if (needed > 1) {
            f += coefficients[1] * results[1].distance;
        }
        if (needed > 2) {
            f += coefficients[2] * results[2].distance;
        }
        return f;
    }

    @Override
    protected int requiredPoints() {
        if (coefficients[2] != 0) {
            return 3;
        }
        if (coefficients[1] != 0) {
            return 2;
        }
        return 1;
    }
}
