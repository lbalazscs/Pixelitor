/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.utils;

import com.jhlabs.image.ImageMath;

public class ColorSpaces {
    /**
     * A lookup table for transforming sRGB (ints in the 0..255 range)
     * to linear RGB values (doubles in the 0..1 range)
     */
    public static final double[] SRGB_TO_LINEAR_LUT = new double[256];

    static {
        for (int i = 0; i < 256; i++) {
            SRGB_TO_LINEAR_LUT[i] = toLinear(i / 255.0);
        }
    }

    private static final int DOUBLE_RESOLUTION = 1000;

    /**
     * A lookup table for linear RGB -> sRGB transformation
     */
    private static final int[] TO_sRGB_LUT = new int[DOUBLE_RESOLUTION + 1];

    static {
        for (int i = 0; i <= DOUBLE_RESOLUTION; i++) {
            TO_sRGB_LUT[i] = (int) (255.0 * toSRGB(i / (double) DOUBLE_RESOLUTION));
        }
    }

    private ColorSpaces() {
        // private constructor to prevent initialization
    }

    private static double toLinear(double sRGB) {
        double lin;
        if (sRGB < 0.04045) {
            lin = sRGB / 12.92;
        } else {
            lin = Math.pow(((sRGB + 0.055) / 1.055), 2.4);
        }
        return lin;
    }

    private static double toSRGB(double lin) {
        double sRGB;
        if (lin <= 0.0031308) {
            sRGB = lin * 12.92;
        } else {
            sRGB = 1.055 * Math.pow(lin, 1.0 / 2.4) - 0.055;
        }
        return sRGB;
    }

    /**
     * Converts a linear-space double in the range 0..1
     * to an sRGB int in the range 0..255
     */
    public static int linearToSRGBInt(double lin) {
        lin = ImageMath.clamp01(lin);
        int index = (int) (lin * DOUBLE_RESOLUTION);
        return TO_sRGB_LUT[index];
    }
}
