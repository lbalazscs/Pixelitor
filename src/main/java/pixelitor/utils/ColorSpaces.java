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
 * Utility methods for color space conversions.
 */
public class ColorSpaces {
    /**
     * Lookup table for transforming sRGB (0..255) to linear RGB (0..1).
     */
    public static final double[] SRGB_TO_LINEAR_LUT = new double[256];

    static {
        for (int i = 0; i < 256; i++) {
            SRGB_TO_LINEAR_LUT[i] = srgbToLinear(i / 255.0);
        }
    }

    private static final int DOUBLE_RESOLUTION = 1000;

    /**
     * Lookup table for transforming linear RGB (0..1) to sRGB (0..255).
     */
    private static final int[] TO_SRGB_LUT = new int[DOUBLE_RESOLUTION + 1];

    static {
        double step = 1.0 / DOUBLE_RESOLUTION;
        for (int i = 0; i <= DOUBLE_RESOLUTION; i++) {
            TO_SRGB_LUT[i] = (int) (255.0 * linearToSrgbExact(i * step));
        }
    }

    private ColorSpaces() {
        // prevent instantiation
    }

    /**
     * Converts sRGB (0..1) to linear RGB (0..1).
     */
    private static double srgbToLinear(double srgb) {
        return (srgb < 0.04045)
            ? srgb / 12.92
            : FastMath.pow((srgb + 0.055) / 1.055, 2.4);
    }

    /**
     * Converts linear RGB (0..1) to sRGB (0..1) without clamping.
     */
    private static double linearToSrgbExact(double lin) {
        return (lin <= 0.0031308)
            ? lin * 12.92
            : 1.055 * FastMath.pow(lin, 1.0 / 2.4) - 0.055;
    }

    /**
     * Converts linear RGB (0..1) to sRGB int (0..255).
     */
    public static int linearToSRGBInt(double lin) {
        lin = ImageMath.clamp01(lin);
        int index = (int) (lin * DOUBLE_RESOLUTION + 0.5);
        return TO_SRGB_LUT[index];
    }

    /**
     * Converts a packed sRGB int to Oklab.
     */
    public static float[] srgbToOklab(int srgb) {
        int r = (srgb >> 16) & 0xFF;
        int g = (srgb >> 8) & 0xFF;
        int b = srgb & 0xFF;

        double rLin = SRGB_TO_LINEAR_LUT[r];
        double gLin = SRGB_TO_LINEAR_LUT[g];
        double bLin = SRGB_TO_LINEAR_LUT[b];

        // L in [0,1], a in ~[-0.4,0.4], b in ~[-0.4,0.4]
        return linearRGBToOklab(rLin, gLin, bLin);
    }

    /**
     * Converts an array of packed sRGB ints to Oklab in packed float array form.
     */
    public static void srgbToOklabBulk(int[] src, float[] dst) {
        for (int i = 0, di = 0; i < src.length; i++, di += 3) {
            int rgb = src[i];
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            double rLin = SRGB_TO_LINEAR_LUT[r];
            double gLin = SRGB_TO_LINEAR_LUT[g];
            double bLin = SRGB_TO_LINEAR_LUT[b];

            float[] lab = linearRGBToOklab(rLin, gLin, bLin);
            dst[di] = lab[0];
            dst[di + 1] = lab[1];
            dst[di + 2] = lab[2];
        }
    }

    /**
     * Converts Oklab to a packed sRGB int.
     */
    public static int oklabToSrgb(float[] oklab) {
        double[] linear = oklabToLinearRGB(oklab);

        int r = linearToSRGBInt(linear[0]);
        int g = linearToSRGBInt(linear[1]);
        int b = linearToSRGBInt(linear[2]);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Converts an array of Oklab (packed floats) to packed sRGB ints.
     */
    public static void oklabToSrgbBulk(float[] src, int[] dst) {
        for (int si = 0, i = 0; i < dst.length; i++, si += 3) {
            double[] linear = oklabToLinearRGB(src, si);
            int r = linearToSRGBInt(linear[0]);
            int g = linearToSRGBInt(linear[1]);
            int b = linearToSRGBInt(linear[2]);
            dst[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    /**
     * Converts Oklab to a packed sRGB int using precise gamma mapping.
     */
    public static int oklabToSrgbPrecise(float[] oklab) {
        double[] linear = oklabToLinearRGB(oklab);

        // oklab can have a larger gamut than sRGB
        double rLin = ImageMath.clamp01(linear[0]);
        double gLin = ImageMath.clamp01(linear[1]);
        double bLin = ImageMath.clamp01(linear[2]);

        int r = (int) (255.0 * linearToSrgbExact(rLin) + 0.5);
        int g = (int) (255.0 * linearToSrgbExact(gLin) + 0.5);
        int b = (int) (255.0 * linearToSrgbExact(bLin) + 0.5);

        assert r >= 0 && g >= 0 && b >= 0;
        assert r <= 0xFF && g <= 0xFF && b <= 0xFF;

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Converts a packed sRGB int to Oklch.
     */
    public static float[] srgbToOklch(int srgb) {
        return oklabToOklch(srgbToOklab(srgb));
    }

    /**
     * Converts Oklch to a packed sRGB int.
     */
    public static int oklchToSrgb(float[] oklch) {
        return oklabToSrgb(oklchToOklab(oklch));
    }

    /**
     * Converts Oklch to a packed sRGB int using precise gamma mapping.
     */
    public static int oklchToSrgbPrecise(float[] oklch) {
        return oklabToSrgbPrecise(oklchToOklab(oklch));
    }

    /**
     * Converts linear sRGB to Oklab.
     */
    public static float[] linearRGBToOklab(double r, double g, double b) {
        double l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b;
        double m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b;
        double s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b;

        double lCbrt = FastMath.cbrt(l);
        double mCbrt = FastMath.cbrt(m);
        double sCbrt = FastMath.cbrt(s);

        return new float[]{
            (float) (0.2104542553 * lCbrt + 0.7936177850 * mCbrt - 0.0040720468 * sCbrt),
            (float) (1.9779984951 * lCbrt - 2.4285922050 * mCbrt + 0.4505937099 * sCbrt),
            (float) (0.0259040371 * lCbrt + 0.7827717662 * mCbrt - 0.8086757660 * sCbrt),
        };
    }

    /**
     * SIMD-friendly version: converts linear sRGB (double array offset) to Oklab without allocations.
     */
    private static float[] linearRGBToOklab(double r, double g, double b, float[] out, int off) {
        double l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b;
        double m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b;
        double s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b;

        double lCbrt = FastMath.cbrt(l);
        double mCbrt = FastMath.cbrt(m);
        double sCbrt = FastMath.cbrt(s);

        out[off] = (float) (0.2104542553 * lCbrt + 0.7936177850 * mCbrt - 0.0040720468 * sCbrt);
        out[off + 1] = (float) (1.9779984951 * lCbrt - 2.4285922050 * mCbrt + 0.4505937099 * sCbrt);
        out[off + 2] = (float) (0.0259040371 * lCbrt + 0.7827717662 * mCbrt - 0.8086757660 * sCbrt);
        return out;
    }

    /**
     * Converts Oklab to linear RGB.
     */
    public static double[] oklabToLinearRGB(float[] c) {
        double l_ = c[0] + 0.3963377774 * c[1] + 0.2158037573 * c[2];
        double m_ = c[0] - 0.1055613458 * c[1] - 0.0638541728 * c[2];
        double s_ = c[0] - 0.0894841775 * c[1] - 1.2914855480 * c[2];

        double l = l_ * l_ * l_;
        double m = m_ * m_ * m_;
        double s = s_ * s_ * s_;

        return new double[]{
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        };
    }

    /**
     * SIMD-friendly version: converts Oklab from packed float array to linear RGB without allocations.
     */
    private static double[] oklabToLinearRGB(float[] c, int off) {
        double l_ = c[off] + 0.3963377774 * c[off + 1] + 0.2158037573 * c[off + 2];
        double m_ = c[off] - 0.1055613458 * c[off + 1] - 0.0638541728 * c[off + 2];
        double s_ = c[off] - 0.0894841775 * c[off + 1] - 1.2914855480 * c[off + 2];

        double l = l_ * l_ * l_;
        double m = m_ * m_ * m_;
        double s = s_ * s_ * s_;

        return new double[]{
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        };
    }

    /**
     * Converts Oklab to Oklch.
     */
    private static float[] oklabToOklch(float[] oklab) {
        float L = oklab[0];
        float a = oklab[1];
        float b = oklab[2];

        float C = (float) FastMath.sqrt(a * a + b * b);
        float hDeg = (float) FastMath.toDegrees(FastMath.atan2(b, a));

        // normalize hue to [0, 360)
        if (hDeg < 0) {
            hDeg += 360;
        }

        return new float[]{L, C, hDeg};
    }

    /**
     * Converts Oklch to Oklab.
     */
    private static float[] oklchToOklab(float[] oklch) {
        float L = oklch[0];
        float C = oklch[1];
        float hRad = (float) FastMath.toRadians(oklch[2]);

        return new float[]{
            L,
            C * (float) FastMath.cos(hRad),
            C * (float) FastMath.sin(hRad)
        };
    }
}