/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters;

import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Hue-Saturation (and Colorize) filter
 */
public class HueSat extends FilterWithParametrizedGUI {
    private static final int MIN_HUE = -180;
    private static final int MAX_HUE = 180;
    private static final int DEFAULT_HUE = 0;

    private static final int MIN_SAT = -100;
    private static final int MAX_SAT = 100;
    private static final int DEFAULT_SAT = 0;

    private static final int MIN_BRI = -100;
    private static final int MAX_BRI = 100;
    private static final int DEFAULT_BRI = 0;

    private final RangeParam hueParam = new RangeParam("Hue", MIN_HUE, MAX_HUE, DEFAULT_HUE);
    private final RangeParam saturationParam = new RangeParam("Saturation", MIN_SAT, MAX_SAT, DEFAULT_SAT);
    private final RangeParam brightnessParam = new RangeParam("Brightness", MIN_BRI, MAX_BRI, DEFAULT_BRI);

    public HueSat() {
        super("Hue/Saturation", true, false);
        setParamSet(new ParamSet(
                hueParam,
                saturationParam,
                brightnessParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int hueP = hueParam.getValue();
        int satP = saturationParam.getValue();
        int briP = brightnessParam.getValue();

        if ((hueP == 0) && (satP == 0) && (briP == 0)) {
            return src;
        }

        float satShift = satP / 100.0f;
        float briShift = briP / 100.0f;
        float hueShift = hueP / 360.0f;

        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        int length = srcData.length;
        if (length != destData.length) {
            throw new IllegalArgumentException("src and dest are not the same size");
        }

        float[] tmpHSBArray = {0.0f, 0.0f, 0.0f};

        for (int i = 0; i < length; i++) {
            int rgb = srcData[i];
            int a = rgb & 0xFF000000;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = (rgb) & 0xFF;

            tmpHSBArray = Color.RGBtoHSB(r, g, b, tmpHSBArray);

            float hue = tmpHSBArray[0] + hueShift;
            float sat = tmpHSBArray[1] + satShift;
            float bri = tmpHSBArray[2] + briShift;

            if (sat < 0.0f) {
                sat = 0.0f;
            }
            if (sat > 1.0f) {
                sat = 1.0f;
            }

            if (bri < 0.0f) {
                bri = 0.0f;
            }
            if (bri > 1.0f) {
                bri = 1.0f;
            }

            int newRGB = Color.HSBtoRGB(hue, sat, bri);  // alpha is 255 here
            newRGB &= 0x00FFFFFF;  // set alpha to 0
            destData[i] = a | newRGB; // add the real alpha
        }

        return dest;
    }
}