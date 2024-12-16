/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.Dithering;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.utils.Texts.i18n;

/**
 * Color Threshold filter
 */
public class ColorThreshold extends ParametrizedFilter {
    public static final String NAME = "Color Threshold";

    @Serial
    private static final long serialVersionUID = -4064195363482725916L;

    private final RangeParam redThreshold = new RangeParam(i18n("red"), 0, 128, 256);
    private final RangeParam greenThreshold = new RangeParam(i18n("green"), 0, 128, 256);
    private final RangeParam blueThreshold = new RangeParam(i18n("blue"), 0, 128, 256);

    private final RangeParam diffusionStrengthParam = new RangeParam("Dithering Amount (%)", 0, 0, 100);
    private final IntChoiceParam ditheringMethodParam = Dithering.createDitheringChoices();

    public ColorThreshold() {
        super(true);

        var threshold = new GroupedRangeParam(i18n("threshold"),
            new RangeParam[]{
                redThreshold,
                greenThreshold,
                blueThreshold
            }, false);

        threshold.setPresetKey("Threshold");
        diffusionStrengthParam.setupEnableOtherIfNotZero(ditheringMethodParam);

        setParams(
            threshold,
            diffusionStrengthParam,
            ditheringMethodParam
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int redTh = redThreshold.getValue();
        int greenTh = greenThreshold.getValue();
        int blueTh = blueThreshold.getValue();

        boolean dither = diffusionStrengthParam.getValue() != 0;
        double diffusionStrength = diffusionStrengthParam.getPercentage();
        int ditheringMethod = ditheringMethodParam.getValue();

        BufferedImage input = dither ? ImageUtils.copyImage(src) : src;

        int[] inputData = ImageUtils.getPixelArray(input);
        int[] destData = ImageUtils.getPixelArray(dest);
        int[] srcData = ImageUtils.getPixelArray(src);

        int width = src.getWidth();
        int length = inputData.length;

        for (int i = 0; i < length; i++) {
            int inPixel = inputData[i];
            int a = srcData[i] & 0xFF_00_00_00;
            int r = (inPixel >>> 16) & 0xFF;
            int g = (inPixel >>> 8) & 0xFF;
            int b = inPixel & 0xFF;

            int outR = r >= redTh ? 0xFF : 0;
            int outG = g >= greenTh ? 0xFF : 0;
            int outB = b >= blueTh ? 0xFF : 0;

            if (dither) {
                double errorR = (r - outR) * diffusionStrength;
                double errorG = (g - outG) * diffusionStrength;
                double errorB = (b - outB) * diffusionStrength;

                Dithering.ditherRGB(ditheringMethod, inputData, i, width, length, errorR, errorG, errorB);
            }

            destData[i] = a | outR << 16 | outG << 8 | outB;
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}