/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.PointFilter;
import net.jafama.FastMath;
import pixelitor.colors.ColorSpaces;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.filters.gui.TransparencyMode.OPAQUE_ONLY;
import static pixelitor.gui.GUIText.BRIGHTNESS;
import static pixelitor.gui.GUIText.COLOR;
import static pixelitor.utils.Texts.i18n;

/**
 * The "Colorize" filter.
 * <p>
 * By default, the luma of each pixel selects a color on a ramp from black
 * to the chosen color, which is the color of white. This is similar to
 * multiplying a grayscale version of the image with the chosen color.
 * <p>
 * If the "Preserve Lightness" checkbox is selected, the Oklab lightness
 * of each pixel is kept, and its hue and chroma are replaced with the hue
 * and chroma of the chosen color (the lightness of the chosen color is
 * ignored). If that chroma doesn't fit into the sRGB gamut at a given
 * lightness, it is reduced (keeping the hue), so the shadows fade to
 * black and the highlights fade to white.
 * <p>
 * In both cases the result depends only on a single lightness value per
 * pixel, so the target colors are precomputed into a lookup table (the ramp).
 */
public class Colorize extends ParametrizedFilter {
    public static final String NAME = i18n("colorize");

    @Serial
    private static final long serialVersionUID = 7036946857972080432L;

    // The resolution of the ramp when "Preserve Lightness" is selected.
    // An Oklab lightness step of 1/1023 is below one sRGB level everywhere.
    private static final int OKLCH_RAMP_SIZE = 1024;

    // Tolerance when checking if a color is within the sRGB gamut.
    // Tiny excursions are harmless, because the conversion clamps anyway.
    private static final double GAMUT_EPSILON = 1.0e-4;

    private final ColorParam colorParam = new ColorParam(
        COLOR, new Color(255, 207, 119), OPAQUE_ONLY);
    private final BooleanParam preserveParam = new BooleanParam("Preserve Lightness", false);
    private final RangeParam brightnessParam = new RangeParam(
        BRIGHTNESS, -100, 0, 100);
    private final RangeParam amountParam = new RangeParam(
        "Amount (%)", 0, 100, 100);

    public Colorize() {
        super(true);

        brightnessParam.setPresetKey("Brightness");
        colorParam.setPresetKey("Color");

        initParams(
            colorParam,
            preserveParam,
            brightnessParam,
            amountParam
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        float briShift = (float) brightnessParam.getPercentage();
        float opacity = (float) amountParam.getPercentage();

        Color color = colorParam.getColor();
        PointFilter filter = preserveParam.isChecked()
            ? new PreserveLightnessFilter(color, briShift, opacity)
            : new MultiplyFilter(color, briShift, opacity);

        return filter.filter(src, dest);
    }

    /**
     * The common part of both modes: a pixel is mapped to an index
     * in a precomputed color ramp, and the result is blended with the source.
     * The ramp is read-only after construction, so processPixel is thread-safe.
     */
    private abstract static class ColorizeFilter extends PointFilter {
        private final int[] ramp; // packed RGB values
        private final float opacity;
        private final float transparency;

        protected ColorizeFilter(int[] ramp, float opacity) {
            super(NAME);
            this.ramp = ramp;
            this.opacity = opacity;
            this.transparency = 1 - opacity;
        }

        /**
         * Returns the index in the ramp for the given source pixel.
         */
        protected abstract int rampIndex(int srcRGB);

        @Override
        public int processPixel(int x, int y, int srcRGB) {
            int rampRGB = ramp[rampIndex(srcRGB)];
            int destRed = (rampRGB >>> 16) & 0xFF;
            int destGreen = (rampRGB >>> 8) & 0xFF;
            int destBlue = rampRGB & 0xFF;

            if (opacity < 1.0f) {
                int srcR = (srcRGB >>> 16) & 0xFF;
                int srcG = (srcRGB >>> 8) & 0xFF;
                int srcB = srcRGB & 0xFF;

                destRed = (int) (destRed * opacity + srcR * transparency);
                destGreen = (int) (destGreen * opacity + srcG * transparency);
                destBlue = (int) (destBlue * opacity + srcB * transparency);
            }

            int a = srcRGB & 0xFF_00_00_00;
            return a | destRed << 16 | destGreen << 8 | destBlue;
        }
    }

    /**
     * The original algorithm. The final R, G, B values depend on the target
     * color's R, G, B values and the luma of the source pixels (in gamma-encoded
     * sRGB), which is used as the index of the ramp.
     */
    private static class MultiplyFilter extends ColorizeFilter {
        private final float briShift;

        MultiplyFilter(Color color, float briShift, float opacity) {
            super(createMultiplyRamp(color), opacity);
            this.briShift = briShift;
        }

        private static int[] createMultiplyRamp(Color color) {
            int red = color.getRed();
            int green = color.getGreen();
            int blue = color.getBlue();

            int[] ramp = new int[256];
            for (int i = 0; i < 256; i++) {
                ramp[i] = ((i * red) / 255) << 16
                    | ((i * green) / 255) << 8
                    | ((i * blue) / 255);
            }
            return ramp;
        }

        @Override
        protected int rampIndex(int srcRGB) {
            float lum = ImageMath.calcLuminance(srcRGB);
            if (briShift > 0) {
                lum = lum * (1.0f - briShift);
                lum += 255 - (1.0f - briShift) * 255.0f;
            } else if (briShift < 0) {
                lum = lum * (briShift + 1.0f);
            }
            return (int) lum;
        }
    }

    /**
     * Keeps the Oklab lightness of the source pixels, and gives them the
     * hue and chroma of the target color.
     */
    private static class PreserveLightnessFilter extends ColorizeFilter {
        private final float briShift;

        PreserveLightnessFilter(Color color, float briShift, float opacity) {
            super(createOklchRamp(color), opacity);
            this.briShift = briShift;
        }

        /**
         * Creates a ramp indexed by Oklab lightness, where each entry has
         * the target color's hue and (if possible) chroma.
         */
        private static int[] createOklchRamp(Color color) {
            float[] oklch = ColorSpaces.srgbToOklch(color.getRGB());
            float chroma = oklch[1];
            float hue = oklch[2];

            int[] ramp = new int[OKLCH_RAMP_SIZE];
            for (int i = 0; i < OKLCH_RAMP_SIZE; i++) {
                float lightness = i / (float) (OKLCH_RAMP_SIZE - 1);
                ramp[i] = oklchToSrgbReducingChroma(lightness, chroma, hue);
            }
            return ramp;
        }

        @Override
        protected int rampIndex(int srcRGB) {
            float l = srgbToOklabL(srcRGB);

            // the same kind of brightness shift as in the default mode,
            // but applied to the Oklab lightness (in the range 0..1)
            if (briShift > 0) {
                l += (1.0f - l) * briShift; // towards white
            } else if (briShift < 0) {
                l *= (1.0f + briShift);     // towards black
            }

            return (int) (ImageMath.clamp01(l) * (OKLCH_RAMP_SIZE - 1) + 0.5f);
        }
    }

    /**
     * Returns the Oklab lightness (0..1) of a packed sRGB value,
     * without allocating anything. Could be moved to ColorSpaces.
     */
    private static float srgbToOklabL(int srgb) {
        double r = ColorSpaces.SRGB_TO_LINEAR_LUT[(srgb >> 16) & 0xFF];
        double g = ColorSpaces.SRGB_TO_LINEAR_LUT[(srgb >> 8) & 0xFF];
        double b = ColorSpaces.SRGB_TO_LINEAR_LUT[srgb & 0xFF];

        double l = FastMath.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
        double m = FastMath.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
        double s = FastMath.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);

        return (float) (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s);
    }

    /**
     * Converts an Oklch color to packed sRGB. If the color is outside the
     * sRGB gamut, the chroma is reduced (with bisection) until it fits,
     * while the lightness and the hue are kept.
     * Only used for building the ramp, so it doesn't have to be fast.
     * Could be moved to ColorSpaces.
     */
    private static int oklchToSrgbReducingChroma(float lightness, float chroma, float hueDeg) {
        if (lightness <= 0.0f) {
            return 0xFF_00_00_00; // black
        }
        if (lightness >= 1.0f) {
            return 0xFF_FF_FF_FF; // white
        }

        double hRad = Math.toRadians(hueDeg);
        double cosH = Math.cos(hRad);
        double sinH = Math.sin(hRad);

        double c = chroma;
        if (!isInSrgbGamut(lightness, c * cosH, c * sinH)) {
            // the chroma at lo is always in gamut (zero chroma is gray),
            // the chroma at hi is always out of gamut
            double lo = 0.0;
            double hi = c;
            for (int i = 0; i < 24; i++) {
                double mid = (lo + hi) / 2.0;
                if (isInSrgbGamut(lightness, mid * cosH, mid * sinH)) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            c = lo;
        }

        return ColorSpaces.oklabToSrgb(lightness, (float) (c * cosH), (float) (c * sinH));
    }

    private static boolean isInSrgbGamut(float l, double a, double b) {
        double[] rgb = ColorSpaces.oklabToLinearRgb(new float[]{l, (float) a, (float) b});
        for (double v : rgb) {
            if (v < -GAMUT_EPSILON || v > 1.0 + GAMUT_EPSILON) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
