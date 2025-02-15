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

import java.awt.image.BufferedImage;

/**
 * A filter which performs a tritone conversion on an image. Given three colors for shadows, midtones and highlights,
 * it converts the image to grayscale and then applies a color mapping based on the colors.
 */
public class TritoneFilter extends PointFilter {
    private int shadowColor = 0xff000000;
    private int midColor = 0xff888888;
    private int highColor = 0xffffffff;

    // lookup table for mapping grayscale values
    // to the corresponding tritone colors
    private int[] lut;

    public TritoneFilter(String filterName) {
        super(filterName);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
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

        dst = super.filter(src, dst);
        lut = null;
        return dst;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        return lut[PixelUtils.brightness(rgb)];
    }

    /**
     * Set the shadow color.
     *
     * @param shadowColor the shadow color
     */
    public void setShadowColor(int shadowColor) {
        this.shadowColor = shadowColor;
    }

    /**
     * Set the mid color.
     *
     * @param midColor the mid color
     */
    public void setMidColor(int midColor) {
        this.midColor = midColor;
    }

    /**
     * Set the high color.
     *
     * @param highColor the high color
     */
    public void setHighColor(int highColor) {
        this.highColor = highColor;
    }

    @Override
    public String toString() {
        return "Colors/Tritone...";
    }
}

