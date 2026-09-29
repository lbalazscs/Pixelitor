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

public class PointillizeFilter extends CellularFilter {
    private float dotRadius = 0.4f;
    private boolean fadeEdges = false;
    private int backgroundColor = 0xFF_00_00_00;
    private float fuzziness = 0.1f;

    public PointillizeFilter(String filterName) {
        super(filterName);
    }

    public void setDotRadius(float dotRadius) {
        this.dotRadius = dotRadius;
    }

    public void setFadeEdges(boolean fadeEdges) {
        this.fadeEdges = fadeEdges;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setFuzziness(float fuzziness) {
        this.fuzziness = fuzziness;
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
