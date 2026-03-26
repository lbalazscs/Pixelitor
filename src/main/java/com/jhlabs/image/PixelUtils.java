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
 * Some more useful math functions for image processing.
 * These are becoming obsolete as we move to Java2D. Use MiscComposite instead.
 */
public class PixelUtils {
    private PixelUtils() {
    }

    /**
     * Clamp a value to the range 0..255
     */
    public static int clamp(int c) {
        return c < 0 ? 0 : c > 255 ? 255 : c;
    }

    /**
     * Clamp for shorts
     */
    public static short clamp(short c) {
        return c < 0 ? 0 : c > 255 ? 255 : c;
    }

    /**
     * Makes sure that the value is not higher than 255
     */
    public static int max255(int c) {
        return Math.min(c, 255);
    }

    public static int interpolate(int v1, int v2, float f) {
        return clamp((int) (v1 + f * (v2 - v1)));
    }

    public static int addPixels(int rgb1, int rgb2) {
        int a1 = rgb1 >>> 24;
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int a2 = rgb2 >>> 24;
        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        a1 += a2; // add the alpha channel
        if (a1 > 255) {
            a1 = 255;
        }

        r1 += r2;
        if (r1 > 255) {
            r1 = 255;
        }

        g1 += g2;
        if (g1 > 255) {
            g1 = 255;
        }

        b1 += b2;
        if (b1 > 255) {
            b1 = 255;
        }

        return (a1 << 24) | (r1 << 16) | (g1 << 8) | b1;
    }
}
