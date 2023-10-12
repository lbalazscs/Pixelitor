/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.lookup.LuminanceLookup;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.NO_TRANSPARENCY;
import static pixelitor.gui.GUIText.BRIGHTNESS;
import static pixelitor.gui.GUIText.COLOR;
import static pixelitor.utils.Texts.i18n;

/**
 * Colorize
 */
public class Colorize extends ParametrizedFilter {
    public static final String NAME = i18n("colorize");

    @Serial
    private static final long serialVersionUID = 7036946857972080432L;

    private final RangeParam brightnessParam = new RangeParam(
        BRIGHTNESS, -100, 0, 100);
    private final ColorParam colorParam = new ColorParam(
        COLOR, new Color(255, 207, 119), NO_TRANSPARENCY);
    private final RangeParam opacityParam = new RangeParam(
        "Amount (%)", 0, 100, 100);

    public Colorize() {
        super(true);

        brightnessParam.setPresetKey("Brightness");
        colorParam.setPresetKey("Color");

        setParams(
            colorParam,
            brightnessParam,
            opacityParam
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float briShift = (float) brightnessParam.getPercentage();
        float opacity = (float) opacityParam.getPercentage();

        Color color = colorParam.getColor();

        return colorize(src, dest, color, briShift, opacity);
    }

    private static BufferedImage colorize(BufferedImage src, BufferedImage dest,
                                          Color color, float briShift, float opacity) {
        int[] srcData = ImageUtils.getPixelArray(src);
        int[] destData = ImageUtils.getPixelArray(dest);

        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();

        // The final R,G,B values depend on the colorize R,G,B values and on the luminosity of the source pixels.
        // For performance reasons the luminosity will be the index in these lookup tables
        int[] redLookup = new int[256];
        int[] greenLookup = new int[256];
        int[] blueLookup = new int[256];
        for (int i = 0; i < 256; i++) {
            redLookup[i] = (i * red) / 255;
            greenLookup[i] = (i * green) / 255;
            blueLookup[i] = (i * blue) / 255;
        }

        int length = srcData.length;

        float translucence = 1 - opacity;
        for (int i = 0; i < length; i++) {
            int srcRGB = srcData[i];
            float lum = LuminanceLookup.from(srcRGB);
            if (briShift > 0) {
                lum = lum * (1.0f - briShift);
                lum += 255 - (1.0f - briShift) * 255.0f;
            } else if (briShift < 0) {
                lum = lum * (briShift + 1.0f);
            }

            int lumIndex = (int) lum;

            int destRed = redLookup[lumIndex];
            int destGreen = greenLookup[lumIndex];
            int destBlue = blueLookup[lumIndex];

            if (opacity < 1.0f) {
                int srcR = (srcRGB >>> 16) & 0xFF;
                int srcG = (srcRGB >>> 8) & 0xFF;
                int srcB = srcRGB & 0xFF;

                destRed = (int) (destRed * opacity + srcR * translucence);
                destGreen = (int) (destGreen * opacity + srcG * translucence);
                destBlue = (int) (destBlue * opacity + srcB * translucence);
            }

            int a = srcRGB & 0xFF_00_00_00;
            destData[i] = a | destRed << 16 | destGreen << 8 | destBlue;
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}