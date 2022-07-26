/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.Colors;
import pixelitor.filters.util.FilterAction;
import pixelitor.filters.util.Filters;
import pixelitor.gui.GUIText;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static pixelitor.utils.Texts.i18n;

/**
 * Extracts a channel from the image
 */
public class ExtractChannelFilter extends Filter {
    public static final String DESATURATE_NAME = i18n("desaturate");
    public static final String SATURATION_NAME = i18n("saturation");
    private final RGBPixelOp rgbOp;

    public ExtractChannelFilter(RGBPixelOp rgbOp) {
        this.rgbOp = rgbOp;
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return Filters.runRGBPixelOp(rgbOp, src, dest);
    }

    @Override
    public boolean supportsGray() {
        return false;
    }

    @Override
    public boolean canBeSmart() {
        return false;
    }

    // static factory methods from here

    public static FilterAction getValueChannelFA() {
        RGBPixelOp rgbOp = (a, r, g, b) -> {
            // value = max(R, G, B)
            int maxRGB = Math.max(r, g);
            if (b > maxRGB) {
                maxRGB = b;
            }

            int value = maxRGB;

            r = value;
            g = value;
            b = value;

            return a << 24 | r << 16 | g << 8 | b;
        };
        String name = "Value = max(R,G,B)";
        return rgbOp.toFilterAction(name);
    }

    public static FilterAction getDesaturateChannelFA() {
        RGBPixelOp rgbOp = (a, r, g, b) -> {
            // achieves desaturation by setting the brightness to [max(R, G, B) + min (R, G, B)] / 2
            int maxRGB = Math.max(r, g);
            if (b > maxRGB) {
                maxRGB = b;
            }
            int minRGB = Math.min(r, g);
            if (b < minRGB) {
                minRGB = b;
            }

            int brightness = (maxRGB + minRGB) / 2;

            r = brightness;
            g = brightness;
            b = brightness;

            return a << 24 | r << 16 | g << 8 | b;
        };
        return rgbOp.toFilterAction(DESATURATE_NAME);
    }

    public static FilterAction getSaturationChannelFA() {
        RGBPixelOp rgbOp = (a, r, g, b) -> {
            int rgbMax = Math.max(r, g);
            if (b > rgbMax) {
                rgbMax = b;
            }
            int rgbMin = Math.min(r, g);
            if (b < rgbMin) {
                rgbMin = b;
            }

            int saturation = 0;
            if (rgbMax != 0) {
                saturation = (int) ((rgbMax - rgbMin) / (float) rgbMax * 255);
            }

            r = saturation;
            g = saturation;
            b = saturation;

            return a << 24 | r << 16 | g << 8 | b;
        };
        return rgbOp.toFilterAction(SATURATION_NAME);
    }

    public static FilterAction getHueChannelFA() {
        var rgbOp = new RGBPixelOp() {
            private float[] tmpHSBArray = {0.0f, 0.0f, 0.0f};

            @Override
            public int changeRGB(int a, int r, int g, int b) {
                tmpHSBArray = Color.RGBtoHSB(r, g, b, tmpHSBArray);

                // Color.RGBtoHSB return all values in the 0..1 interval
                int hue = (int) (tmpHSBArray[0] * 255);

                r = hue;
                g = hue;
                b = hue;

                return a << 24 | r << 16 | g << 8 | b;
            }
        };
        return rgbOp.toFilterAction(GUIText.HUE);
    }

    public static FilterAction getHueInColorsChannelFA() {
        var rgbOp = new RGBPixelOp() {
            private static final float DEFAULT_SATURATION = 0.9f;
            private static final float DEFAULT_BRIGHTNESS = 0.75f;

            private float[] tmpHSBArray = {0.0f, 0.0f, 0.0f};

            @Override
            public int changeRGB(int a, int r, int g, int b) {
                if (a == 0) {
                    return 0; // for premultiplied images
                }
                tmpHSBArray = Color.RGBtoHSB(r, g, b, tmpHSBArray);
                int newRGB = Color.HSBtoRGB(tmpHSBArray[0],
                    DEFAULT_SATURATION, DEFAULT_BRIGHTNESS);
                // Color.HSBtoRGB always creates an alpha of 255 => restore the original value
                return Colors.setAlpha(newRGB, a);
            }
        };
        return rgbOp.toFilterAction("Hue (with colors)");
    }
}
