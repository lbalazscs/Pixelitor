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
 * A filter that renders an image as colored dots
 * sampled at the feature points of a Voronoi grid.
 */
public class PointillizeFilter extends CellularFilter {
    private final float dotRadius;
    private final boolean fadeEdges;
    private final int backgroundColor;
    private final float fuzziness;

    /**
     * Constructs a new {@code PointillizeFilter}.
     *
     * @param filterName      the name of the filter
     * @param scale           the grid size (texture scale)
     * @param stretch         the texture stretch factor
     * @param angle           the texture angle in radians
     * @param gridType        the grid type defining feature point placement
     * @param randomness      the randomness factor for grid point placement
     * @param dotRadius       the relative radius of each dot
     * @param fuzziness       the edge transition fuzziness of each dot
     * @param backgroundColor the ARGB background color between dots
     * @param fadeEdges       whether to blend neighboring colors instead of drawing dots on a solid background
     */
    public PointillizeFilter(String filterName,
                             float scale,
                             float stretch,
                             float angle,
                             GridType gridType,
                             float randomness,
                             float dotRadius,
                             float fuzziness,
                             int backgroundColor,
                             boolean fadeEdges) {
        super(filterName, scale, stretch, angle, gridType, randomness, null, 1.0f, 0.0f, 0.0f);

        this.dotRadius = dotRadius;
        this.fuzziness = fuzziness;
        this.fadeEdges = fadeEdges;
        this.backgroundColor = backgroundColor;
    }

    @Override
    public int genPixel(int x, int y, int[] inPixels, int width, int height, int needed) {
        Point[] results = findNearestPoints(x, y, needed);

        if (fadeEdges) {
            return blendNeighbors(results, inPixels, width, height);
        }

        float f1 = results[0].distance;
        if (f1 >= dotRadius + fuzziness) {
            return backgroundColor; // dotBlend would be 0
        }

        // sample source pixel using the inverse transform
        int color = getSourcePixel(results[0], inPixels, width, height);
        if (f1 <= dotRadius) {
            return color; // dotBlend would be 1
        }

        float dotBlend = 1 - ImageMath.smoothStep(dotRadius, dotRadius + fuzziness, f1);
        return ImageMath.mixColors(dotBlend, backgroundColor, color);
    }

    private int blendNeighbors(Point[] results, int[] inPixels, int width, int height) {
        float f1 = results[0].distance;
        float f2 = results[1].distance;
        int color = getSourcePixel(results[0], inPixels, width, height);
        int secondColor = getSourcePixel(results[1], inPixels, width, height);
        return ImageMath.mixColors(0.5f * f1 / f2, color, secondColor);
    }

    @Override
    protected int requiredPoints() {
        return fadeEdges ? 2 : 1;
    }
}
