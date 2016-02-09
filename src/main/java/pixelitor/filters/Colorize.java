/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.lookup.LuminanceLookup;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static pixelitor.filters.gui.ColorParam.OpacitySetting.NO_OPACITY;

/**
 * Colorize
 */
public class Colorize extends FilterWithParametrizedGUI {
    public static final String NAME = "Colorize";

    private final RangeParam adjustBrightness = new RangeParam("Adjust Brightness", -100, 0, 100);
    private final ColorParam colorParam = new ColorParam("Color:", new Color(255, 207, 119), NO_OPACITY);
    private final RangeParam opacityParam = new RangeParam("Amount (%)", 0, 100, 100);

    public Colorize() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                colorParam,
                adjustBrightness,
                opacityParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float briShift = adjustBrightness.getValueAsPercentage();
        float opacity = opacityParam.getValueAsPercentage();

        Color colorizeColor = colorParam.getColor();

        return colorize(src, dest, colorizeColor, briShift, opacity);
    }

    public static BufferedImage colorize(BufferedImage src, BufferedImage dest, Color colorizeColor, float briShift, float opacity) {
        float translucence = 1 - opacity;

        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        int colorizeR = colorizeColor.getRed();
        int colorizeG = colorizeColor.getGreen();
        int colorizeB = colorizeColor.getBlue();

        // The final R,G,B values depend on the colorize R,G,B values and on the luminosity of the source pixels.
        // For performance reasons the luminosity will be the index in these lookup tables
        int[] redLookup = new int[256];
        int[] greenLookup = new int[256];
        int[] blueLookup = new int[256];
        for (int i = 0; i < 256; i++) {
            redLookup[i] = (i * colorizeR) / 255;
            greenLookup[i] = (i * colorizeG) / 255;
            blueLookup[i] = (i * colorizeB) / 255;
        }

        int length = srcData.length;

        for (int i = 0; i < length; i++) {
            int rgb = srcData[i];
            int a = rgb & 0xFF000000;
            int lum = LuminanceLookup.getLuminosity(rgb);
            if (briShift > 0) {
                lum = (int) ((float) lum * (1.0f - briShift));
                lum += 255 - (1.0f - briShift) * 255.0f;
            } else if (briShift < 0) {
                lum = (int) ((float) lum * (briShift + 1.0f));
            }

            int destRed = redLookup[lum];
            int destGreen = greenLookup[lum];
            int destBlue = blueLookup[lum];

            if (opacity < 1.0f) {
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = rgb & 0xFF;

                destRed = (int) (destRed * opacity + r * translucence);
                destGreen = (int) (destGreen * opacity + g * translucence);
                destBlue = (int) (destBlue * opacity + b * translucence);
            }

            destData[i] = a | (destRed << 16) | (destGreen << 8) | destBlue;
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}