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
    private final float fuzziness;
    private final int backgroundColor;
    private final boolean fadeEdges;

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
        this.backgroundColor = backgroundColor;
        this.fadeEdges = fadeEdges;
    }

    @Override
    public int genPixel(int x, int y, int[] inPixels, int width, int height) {
        Point[] results = findNearestPoints(x, y);

        float f1 = results[0].distance;

        // sample source pixel using the inverse transform
        int color = getSourcePixel(results[0], inPixels, width, height);

        if (fadeEdges) {
            float f2 = results[1].distance;
            // sample second nearest source pixel
            int secondColor = getSourcePixel(results[1], inPixels, width, height);
            color = ImageMath.mixColors(0.5f * f1 / f2, color, secondColor);
        } else {
            float dotBlend = 1 - ImageMath.smoothStep(dotRadius, dotRadius + fuzziness, f1);
            color = ImageMath.mixColors(dotBlend, backgroundColor, color);
        }
        return color;
    }
}
