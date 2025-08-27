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

package pixelitor.filters;

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.PointFilter;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.util.ColorSpace;
import pixelitor.gui.GUIText;
import pixelitor.utils.ColorSpaces;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.gui.GUIText.BRIGHTNESS;
import static pixelitor.gui.GUIText.HUE;
import static pixelitor.gui.GUIText.SATURATION;

/**
 * The Hue-Saturation filter.
 */
public class HueSat extends ParametrizedFilter {
    public static final String NAME = HUE + "/" + SATURATION;

    @Serial
    private static final long serialVersionUID = -5215830710090103691L;

    private static final int MIN_HUE = -180;
    private static final int MAX_HUE = 180;
    private static final int DEFAULT_HUE = 0;

    private static final int MIN_SAT = -100;
    private static final int MAX_SAT = 100;
    private static final int DEFAULT_SAT = 0;

    private static final int MIN_BRI = -100;
    private static final int MAX_BRI = 100;
    private static final int DEFAULT_BRI = 0;

    private static final int COLOR_SPACE_HSV = 0;
    private static final int COLOR_SPACE_OKLCH = 1;

    private final IntChoiceParam colorSpace = new IntChoiceParam(GUIText.COLOR_SPACE, ColorSpace.PRESET_KEY, new Item[]{
        new Item("HSV (Faster)", COLOR_SPACE_HSV),
        new Item("Oklch (Better)", COLOR_SPACE_OKLCH),
    });
    private final RangeParam hue = new RangeParam(HUE, MIN_HUE, DEFAULT_HUE, MAX_HUE);
    private final RangeParam saturation = new RangeParam(SATURATION, MIN_SAT, DEFAULT_SAT, MAX_SAT);
    private final RangeParam brightness = new RangeParam(BRIGHTNESS, MIN_BRI, DEFAULT_BRI, MAX_BRI);

    public HueSat() {
        super(true);

        hue.setPresetKey("Hue");
        saturation.setPresetKey("Saturation");
        brightness.setPresetKey("Brightness");

        initParams(
            colorSpace,
            hue,
            saturation,
            brightness
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (hue.isZero() && saturation.isZero() && brightness.isZero()) {
            return src;
        }

        if (colorSpace.valueIs(COLOR_SPACE_OKLCH)) {
            float hueShift = hue.getValueAsFloat();
            // satFactor is a multiplier, e.g., 1.5 for a 50% increase
            float satFactor = 1.0f + (float) saturation.getPercentage();
            float briShift = (float) brightness.getPercentage();

            dest = new OklchImpl(hueShift, satFactor, briShift).filter(src, dest);
            return dest;
        }

        // HSV color space
        float satShift = (float) saturation.getPercentage();
        float briShift = (float) brightness.getPercentage();
        float hueRot = hue.getValueAsFloat() / 360.0f;

        dest = new HsvImpl(hueRot, satShift, briShift).filter(src, dest);

        return dest;
    }

    /**
     * An implementation of the filter that works in the Oklch color space.
     */
    private static class OklchImpl extends PointFilter {
        private final float hueShift;
        private final float satFactor;
        private final float briShift;

        protected OklchImpl(float hueShift, float satFactor, float briShift) {
            super(NAME);
            this.hueShift = hueShift;
            this.satFactor = satFactor;
            this.briShift = briShift;
        }

        @Override
        public int processPixel(int x, int y, int rgb) {
            int a = rgb & 0xFF_00_00_00;

            // for the multithreaded performance it's better to
            // create this array here instead of reusing it as a class field
            float[] oklch = ColorSpaces.srgbToOklch(rgb);

            // L is in [0, 1], C is >= 0, h is in [0, 360)
            float l = oklch[0];
            float c = oklch[1];
            float h = oklch[2];

            // apply adjustments
            h += hueShift;
            // normalize hue to be in the range [0, 360)
            if (h < 0.0f) {
                h += 360.0f;
            }
            if (h >= 360.0f) {
                h -= 360.0f;
            }

            c *= satFactor;
            // chroma can't be negative
            c = Math.max(0.0f, c);

            l += briShift;
            // clamp lightness to [0, 1]
            l = ImageMath.clamp01(l);

            oklch[0] = l;
            oklch[1] = c;
            oklch[2] = h;

            int newRGB = ColorSpaces.oklchToSrgb(oklch);
            return a | (newRGB & 0x00_FF_FF_FF);
        }
    }

    private static class HsvImpl extends PointFilter {
        private final float hueRot;
        private final float satShift;
        private final float briShift;

        protected HsvImpl(float hueRot, float satShift, float briShift) {
            super(NAME);
            this.hueRot = hueRot;
            this.satShift = satShift;
            this.briShift = briShift;
        }

        @Override
        public int processPixel(int x, int y, int rgb) {
            int a = rgb & 0xFF_00_00_00;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

            // for the multithreaded performance it's better to
            // create this array here instead of reusing it as a class field
            float[] tmpHSBArray = {0.0f, 0.0f, 0.0f};

            tmpHSBArray = Color.RGBtoHSB(r, g, b, tmpHSBArray);

            float newHue = tmpHSBArray[0] + hueRot;
            float newSat = tmpHSBArray[1] + satShift;
            float newBri = tmpHSBArray[2] + briShift;

            newSat = ImageMath.clamp01(newSat);
            newBri = ImageMath.clamp01(newBri);

            if (newHue < 0 && newHue > -0.00000003) {
                // workaround for a bug in Color.HSBtoRGB, see issue #87
                newHue = 0;
            }

            int newRGB = Color.HSBtoRGB(newHue, newSat, newBri);  // alpha is 255 here
            newRGB &= 0x00_FF_FF_FF;  // set alpha to 0
            return a | newRGB; // add the real alpha
        }
    }
}
