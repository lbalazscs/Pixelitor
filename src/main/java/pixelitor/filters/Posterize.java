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

import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.levels.RGBLookup;
import pixelitor.filters.lookup.FastLookupOp;
import pixelitor.utils.Dithering;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ShortLookupTable;
import java.io.Serial;

import static pixelitor.utils.Texts.i18n;

/**
 * Posterize filter
 */
public class Posterize extends ParametrizedFilter {
    public static final String NAME = i18n("posterize");

    @Serial
    private static final long serialVersionUID = 4448706459360371642L;

    private final RangeParam redLevels = new RangeParam(i18n("red"), 1, 2, 8);
    private final RangeParam greenLevels = new RangeParam(i18n("green"), 1, 2, 8);
    private final RangeParam blueLevels = new RangeParam(i18n("blue"), 1, 2, 8);

    private final RangeParam diffusionStrengthParam = new RangeParam("Dithering Amount (%)", 0, 0, 100);
    private final IntChoiceParam ditheringMethodParam = Dithering.createDitheringChoices();

    public Posterize() {
        super(true);

        diffusionStrengthParam.setupEnableOtherIfNotZero(ditheringMethodParam);

        setParams(
            new GroupedRangeParam("Levels",
                new RangeParam[]{
                    redLevels,
                    greenLevels,
                    blueLevels
                }, true),
            diffusionStrengthParam,
            ditheringMethodParam
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int numRedLevels = redLevels.getValue();
        int numGreenLevels = greenLevels.getValue();
        int numBlueLevels = blueLevels.getValue();
        RGBLookup rgbLookup = new RGBLookup();
        rgbLookup.initFromPosterize(numRedLevels, numGreenLevels, numBlueLevels);

        boolean dither = diffusionStrengthParam.getValue() != 0;
        if (!dither) {
            // simple case
            BufferedImageOp filterOp = new FastLookupOp((ShortLookupTable) rgbLookup.getLookupOp());
            filterOp.filter(src, dest);
            return dest;
        }

        double diffusionStrength = diffusionStrengthParam.getPercentage();
        int ditheringMethod = ditheringMethodParam.getValue();


        BufferedImage input = ImageUtils.copyImage(src);
        int[] inputPixels = ImageUtils.getPixels(input);
        int[] srcPixels = ImageUtils.getPixels(src);
        int[] destPixels = ImageUtils.getPixels(dest);

        int width = src.getWidth();
        int length = inputPixels.length;

        short[] redLUT = rgbLookup.getRedLUT();
        short[] greenLUT = rgbLookup.getGreenLUT();
        short[] blueLUT = rgbLookup.getBlueLUT();

        for (int i = 0; i < length; i++) {
            int inRGB = inputPixels[i];
            int a = srcPixels[i] & 0xFF_00_00_00;
            int r = (inRGB >>> 16) & 0xFF;
            int g = (inRGB >>> 8) & 0xFF;
            int b = inRGB & 0xFF;

            int outR = redLUT[r];
            int outG = greenLUT[g];
            int outB = blueLUT[b];

            double errorR = (r - outR) * diffusionStrength;
            double errorG = (g - outG) * diffusionStrength;
            double errorB = (b - outB) * diffusionStrength;
            Dithering.ditherRGB(ditheringMethod, inputPixels, i, width, length, errorR, errorG, errorB);

            destPixels[i] = a | outR << 16 | outG << 8 | outB;
        }

        return dest;
    }

    @Override
    public boolean supportsTweenAnimation() {
        return false;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}