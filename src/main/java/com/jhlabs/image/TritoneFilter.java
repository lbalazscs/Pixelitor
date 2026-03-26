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
 * A filter which performs a tritone conversion on an image. Given three colors for shadows, midtones and highlights,
 * it converts the image to grayscale and then applies a color mapping based on the colors.
 */
public class TritoneFilter extends PointFilter {
    // lookup table for mapping grayscale values
    // to the corresponding tritone colors
    private final int[] lut;

    /**
     * Constructs a {@link TritoneFilter} with the given colors for the tritone mapping.
     *
     * @param filterName  the name of the filter
     * @param shadowColor the shadow color, applied to dark areas of the image
     * @param midColor    the mid color, applied to midtone areas of the image
     * @param highColor   the highlight color, applied to bright areas of the image
     */
    public TritoneFilter(String filterName, int shadowColor, int midColor, int highColor) {
        super(filterName);

        lut = new int[256];

        // fill the LUT for shadow to midtone transition
        for (int i = 0; i < 128; i++) {
            float t = i / 127.0f;
            lut[i] = ImageMath.mixColors(t, shadowColor, midColor);
        }

        // fill the LUT for midtone to highlight transition
        for (int i = 128; i < 256; i++) {
            float t = (i - 127) / 128.0f;
            lut[i] = ImageMath.mixColors(t, midColor, highColor);
        }
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        return lut[ImageMath.calcLuminanceInt(rgb)];
    }
}
