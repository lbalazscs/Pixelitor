/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.PixelUtils;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

import static pixelitor.gui.GUIText.BRIGHTNESS;
import static pixelitor.utils.Texts.i18n;

/**
 * The Brightness/Contrast filter
 */
public class BrightnessContrast extends ParametrizedFilter {
    public static final String CONTRAST = i18n("contrast");
    public static final String NAME = BRIGHTNESS + "/" + CONTRAST;

    private final RangeParam brightnessParam = new RangeParam(BRIGHTNESS, -100, 0, 100);
    private final RangeParam contrastParam = new RangeParam(CONTRAST, -100, 0, 100);

    public BrightnessContrast() {
        super(ShowOriginal.YES);

        setParams(
            brightnessParam,
            contrastParam
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (brightnessParam.isZero() && contrastParam.isZero()) {
            return src;
        }

        // prepare brightness
        double brightnessValue = brightnessParam.getValue() / 10.0;
        double pow = -brightnessValue + 1;
        if(brightnessValue > 0) {
            pow = 1.0 / (brightnessValue + 1);
        }
        double normalize = Math.pow(255, pow - 1);

        // prepare contrast
        double contrastValue = contrastParam.getValue() * 2.55;
        double contrastFactor = (259.0 * (contrastValue + 255)) / (255.0 * (259 - contrastValue));

        // create the lookup table
        int[] lookup = new int[256];
        for (int i = 0; i < lookup.length; i++) {
            double lookupValue = i; // by default do nothing

            // modify for brightness
            lookupValue = (float) Math.pow(lookupValue, pow) / normalize;

            // modify for contrast
            lookupValue = contrastFactor * (lookupValue - 128) + 128;

            lookup[i] = PixelUtils.clamp((int) lookupValue);
        }

        // transform the image
        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);
        for (int i = 0; i < destData.length; i++) {
            int rgb = srcData[i];

//            int a = (rgb >>> 24) & 0xFF;
            int a = rgb & 0xFF000000;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

            if (a == 0) {
                destData[i] = 0; // for premultiplied images
            } else {
                r = lookup[r];
                g = lookup[g];
                b = lookup[b];

                destData[i] = a | r << 16 | g << 8 | b;
            }
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}