/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import net.jafama.FastMath;

/**
 * Utility methods related to color spaces.
 */
public class ColorSpaces {
    /**
     * A lookup table for transforming sRGB (ints in the 0..255 range)
     * to linear RGB values (doubles in the 0..1 range)
     */
    public static final double[] SRGB_TO_LINEAR_LUT = new double[256];

    static {
        for (int i = 0; i < 256; i++) {
            SRGB_TO_LINEAR_LUT[i] = srgbToLinear(i / 255.0);
        }
    }

    private static final int DOUBLE_RESOLUTION = 1000;

    /**
     * A lookup table for linear RGB -> sRGB transformation
     */
    private static final int[] TO_sRGB_LUT = new int[DOUBLE_RESOLUTION + 1];

    static {
        for (int i = 0; i <= DOUBLE_RESOLUTION; i++) {
            TO_sRGB_LUT[i] = (int) (255.0 * linearToSRGB(i / (double) DOUBLE_RESOLUTION));
        }
    }

    private ColorSpaces() {
        // prevent initialization
    }

    private static double srgbToLinear(double sRGB) {
        double lin;
        if (sRGB < 0.04045) {
            lin = sRGB / 12.92;
        } else {
            lin = Math.pow(((sRGB + 0.055) / 1.055), 2.4);
        }
        return lin;
    }

    private static double linearToSRGB(double lin) {
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
        int index = (int) (lin * DOUBLE_RESOLUTION + 0.5);
        return TO_sRGB_LUT[index];
    }

    /**
     * Converts a color from the sRGB color space to the Oklab color space.
     */
    public static float[] srgbToOklab(int srgb) {
        int r = (srgb >> 16) & 0xFF;
        int g = (srgb >> 8) & 0xFF;
        int b = srgb & 0xFF;

        double lr = SRGB_TO_LINEAR_LUT[r];
        double lg = SRGB_TO_LINEAR_LUT[g];
        double lb = SRGB_TO_LINEAR_LUT[b];

        // L (lightness) is in [0, 1], a (green-red) and b (blue-yellow) are roughly in [-0.4, 0.4].
        return linearSrgbToOklab(lr, lg, lb);
    }

    /**
     * Converts a color from the Oklab color space to the sRGB color space.
     */
    public static int oklabToSrgb(float[] oklab) {
        double[] linearSrgb = oklabToLinearSrgb(oklab);

        int r = linearToSRGBInt(linearSrgb[0]);
        int g = linearToSRGBInt(linearSrgb[1]);
        int b = linearToSRGBInt(linearSrgb[2]);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int oklabToSrgbPrecise(float[] oklab) {
        double[] linearSrgb = oklabToLinearSrgb(oklab);

        // Oklab can have a larger gamut than sRGB
        double r_lin = ImageMath.clamp01(linearSrgb[0]);
        double g_lin = ImageMath.clamp01(linearSrgb[1]);
        double b_lin = ImageMath.clamp01(linearSrgb[2]);

        int r = (int) (255.0 * linearToSRGB(r_lin) + 0.5);
        int g = (int) (255.0 * linearToSRGB(g_lin) + 0.5);
        int b = (int) (255.0 * linearToSRGB(b_lin) + 0.5);

        assert r >= 0 && g >= 0 && b >= 0;
        assert r <= 0xFF && g <= 0xFF && b <= 0xFF;

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Converts a color from the sRGB color space to the OkLCH color space.
     */
    public static float[] srgbToOklch(int srgb) {
        float[] oklab = srgbToOklab(srgb);

        //  L (lightness, [0, 1]), C (chroma, [0, ~0.4]), h (hue, [0, 360) degrees)
        return oklabToOklch(oklab);
    }

    /**
     * Converts a color from the OkLCH color space to the sRGB color space.
     */
    public static int oklchToSrgb(float[] oklch) {
        float[] oklab = oklchToOklab(oklch);
        return oklabToSrgb(oklab);
    }

    public static int oklchToSrgbPrecise(float[] oklch) {
        float[] oklab = oklchToOklab(oklch);
        return oklabToSrgbPrecise(oklab);
    }

    private static float[] linearSrgbToOklab(double r, double g, double b) {
        double l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b;
        double m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b;
        double s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b;

        double l_ = Math.cbrt(l);
        double m_ = Math.cbrt(m);
        double s_ = Math.cbrt(s);

        return new float[]{
            (float) (0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_),
            (float) (1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_),
            (float) (0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_),
        };
    }

    private static double[] oklabToLinearSrgb(float[] c) {
        double l_ = (double) c[0] + 0.3963377774 * c[1] + 0.2158037573 * c[2];
        double m_ = (double) c[0] - 0.1055613458 * c[1] - 0.0638541728 * c[2];
        double s_ = (double) c[0] - 0.0894841775 * c[1] - 1.2914855480 * c[2];

        double l = l_ * l_ * l_;
        double m = m_ * m_ * m_;
        double s = s_ * s_ * s_;

        return new double[]{
            +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        };
    }

    private static float[] oklabToOklch(float[] oklab) {
        float L = oklab[0];
        float a = oklab[1];
        float b = oklab[2];

        float C = (float) Math.sqrt(a * a + b * b);
        float h_rad = (float) Math.atan2(b, a);
        float h_deg = (float) Math.toDegrees(h_rad);

        // normalize hue to be in the range [0, 360)
        if (h_deg < 0) {
            h_deg += 360;
        }

        return new float[]{L, C, h_deg};
    }

    private static float[] oklchToOklab(float[] oklch) {
        float L = oklch[0];
        float C = oklch[1];
        float h_deg = oklch[2];

        float h_rad = (float) Math.toRadians(h_deg);

        float a = C * (float) FastMath.cos(h_rad);
        float b = C * (float) FastMath.sin(h_rad);

        return new float[]{L, a, b};
    }
}