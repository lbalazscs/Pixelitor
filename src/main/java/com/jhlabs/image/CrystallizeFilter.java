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
 * A filter which applies a crystallizing effect to an image,
 * by producing Voronoi cells filled with colors from the image.
 */
public class CrystallizeFilter extends CellularFilter {
    private final float edgeThickness;
    private final boolean fadeEdges;
    private final int edgeColor;

    /**
     * Constructs a new {@code CrystallizeFilter}.
     *
     * @param filterName    the name of the filter
     * @param scale         the cell size (texture scale)
     * @param stretch       the texture stretch factor
     * @param angle         the texture angle in radians
     * @param gridType      the grid type defining cell shapes
     * @param randomness    the randomness factor for cell shape placement
     * @param edgeThickness the thickness factor of the cell edges
     * @param edgeColor     the ARGB color of the cell edges
     * @param fadeEdges     whether to fade edges by blending neighbor cell colors instead of using a solid color
     */
    public CrystallizeFilter(String filterName,
                             float scale,
                             float stretch,
                             float angle,
                             GridType gridType,
                             float randomness,
                             float edgeThickness,
                             int edgeColor,
                             boolean fadeEdges) {
        super(filterName, scale, stretch, angle, gridType, randomness, null, 1.0f, 0.0f, 0.0f);

        this.edgeThickness = edgeThickness;
        this.edgeColor = edgeColor;
        this.fadeEdges = fadeEdges;
    }

    @Override
    public int genPixel(int x, int y, int[] inPixels, int width, int height, int needed) {
        Point[] results = findNearestPoints(x, y, needed);

        float f1 = results[0].distance;
        float f2 = results[1].distance;

        // sample source pixel using the inverse transform
        int color = getSourcePixel(results[0], inPixels, width, height);

        float edgeBlend = (f2 - f1) / edgeThickness;
        edgeBlend = ImageMath.smoothStep(0, edgeThickness, edgeBlend);
        // TODO instead of the 2 lines above, we should have
        // float edgeBlend = ImageMath.smoothStep(0, edgeThickness, f2 - f1);
        // (leaving the old way for now for compatibility)

        if (edgeBlend >= 1.0f) {
            return color; // interior: the blend would return this color anyway
        }

        if (fadeEdges) {
            // sample second nearest source pixel
            int secondColor = getSourcePixel(results[1], inPixels, width, height);
            secondColor = ImageMath.mixColors(0.5f, secondColor, color);
            color = ImageMath.mixColors(edgeBlend, secondColor, color);
        } else {
            color = ImageMath.mixColors(edgeBlend, edgeColor, color);
        }
        return color;
    }

    @Override
    protected int requiredPoints() {
        return 2;
    }
}
