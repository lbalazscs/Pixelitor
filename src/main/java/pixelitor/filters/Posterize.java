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

import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.lookup.RGBLookup;
import pixelitor.filters.util.ColorSpace;
import pixelitor.utils.ColorSpaces;
import pixelitor.utils.Dithering;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.Serial;

import static pixelitor.utils.Texts.i18n;

/**
 * The "Posterize" filter.
 */
public class Posterize extends ParametrizedFilter {
    public static final String NAME = i18n("posterize");

    @Serial
    private static final long serialVersionUID = 4448706459360371642L;

    private final EnumParam<ColorSpace> colorSpace = ColorSpace.asParam();

    private final RangeParam levels1 = new RangeParam(i18n("red"), 1, 2, 8);
    private final RangeParam levels2 = new RangeParam(i18n("green"), 1, 2, 8);
    private final RangeParam levels3 = new RangeParam(i18n("blue"), 1, 2, 8);

    private final GroupedRangeParam levelsParam = new GroupedRangeParam("Levels",
        new RangeParam[]{
            levels1,
            levels2,
            levels3
        }, true);

    private final RangeParam ditheringAmountParam = new RangeParam("Dithering Amount (%)", 0, 0, 100);
    private final IntChoiceParam ditheringMethodParam = Dithering.createDitheringChoices();

    public Posterize() {
        super(true);

        ditheringAmountParam.setupEnableOtherIfNotZero(ditheringMethodParam);

        initParams(
            colorSpace,
            levelsParam,
            ditheringAmountParam,
            ditheringMethodParam
        );

        colorSpace.addOnChangeTask(this::updateSliders);
    }

    private void updateSliders() {
        switch (colorSpace.getSelected()) {
            case SRGB -> {
                levels1.setName(i18n("red"));
                levels2.setName(i18n("green"));
                levels3.setName(i18n("blue"));
            }
            case OKLAB -> {
                levels1.setName("Green-Red (a)");
                levels2.setName("Blue-Yellow (b)");
                levels3.setName("Lightness");
            }
        }
        levelsParam.updateGUIAppearance();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return switch (colorSpace.getSelected()) {
            case SRGB -> posterizeSrgb(src, dest);
            case OKLAB -> posterizeOklab(src, dest);
        };
    }

    private BufferedImage posterizeSrgb(BufferedImage src, BufferedImage dest) {
        RGBLookup srgbLookup = RGBLookup.createForPosterize(
            levels1.getValue(), levels2.getValue(), levels3.getValue());

        boolean ditheringEnabled = ditheringAmountParam.isNotZero();
        if (ditheringEnabled) {
            return posterizeSrgbWithDithering(src, dest, srgbLookup);
        }

        // simple case for sRGB without dithering
        BufferedImageOp filterOp = srgbLookup.asFastLookupOp();
        filterOp.filter(src, dest);
        return dest;
    }

    private BufferedImage posterizeOklab(BufferedImage src, BufferedImage dest) {
        int aLevels = levels1.getValue();
        int bLevels = levels2.getValue();
        int lLevels = levels3.getValue();

        boolean ditheringEnabled = ditheringAmountParam.isNotZero();
        double diffusionStrength = ditheringAmountParam.getPercentage();
        int ditheringMethod = ditheringMethodParam.getValue();

        int width = src.getWidth();

        int[] srcPixels = ImageUtils.getPixels(src);
        int[] destPixels = ImageUtils.getPixels(dest);
        int numPixels = srcPixels.length;
        int[] inputPixels;

        // Pre-pass to find the actual min/max range of a and b channels for this image.
        // This adapts the quantization to the image's content, avoiding the perceptual
        // artifacts caused by a fixed, symmetric range.
        float minA = Float.MAX_VALUE, maxA = Float.MIN_VALUE;
        float minB = Float.MAX_VALUE, maxB = Float.MIN_VALUE;

        for (int srcPixel : srcPixels) {
            float[] oklab = ColorSpaces.srgbToOklab(srcPixel);
            float a = oklab[1];
            float b = oklab[2];

            if (a < minA) {
                minA = a;
            }
            if (a > maxA) {
                maxA = a;
            }
            if (b < minB) {
                minB = b;
            }
            if (b > maxB) {
                maxB = b;
            }
        }

        if (ditheringEnabled) {
            // use a copy of the image for dithering to diffuse errors
            inputPixels = ImageUtils.getPixels(ImageUtils.copyImage(src));
        } else {
            // no dithering, process source pixels directly
            inputPixels = srcPixels;
        }

        for (int i = 0; i < numPixels; i++) {
            int inRGB = inputPixels[i];
            // always get the alpha from the original, unmodified source image
            int alpha = srcPixels[i] & 0xFF000000;

            float[] oklab = ColorSpaces.srgbToOklab(inRGB);

            // quantize L, a, b channels
            float quantizedL = quantizeFloat(oklab[0], lLevels, 0.0f, 1.0f);
            // use the calculated dynamic range
            float quantizedA = quantizeFloat(oklab[1], aLevels, minA, maxA);
            float quantizedB = quantizeFloat(oklab[2], bLevels, minB, maxB);

            float[] quantizedOklab = {quantizedL, quantizedA, quantizedB};

            int outRGB = ColorSpaces.oklabToSrgb(quantizedOklab);

            if (ditheringEnabled) {
                int r = (inRGB >>> 16) & 0xFF;
                int g = (inRGB >>> 8) & 0xFF;
                int b = inRGB & 0xFF;

                int outR = (outRGB >>> 16) & 0xFF;
                int outG = (outRGB >>> 8) & 0xFF;
                int outB = outRGB & 0xFF;

                double errorR = (r - outR) * diffusionStrength;
                double errorG = (g - outG) * diffusionStrength;
                double errorB = (b - outB) * diffusionStrength;
                Dithering.ditherRGB(ditheringMethod, inputPixels, i, width, numPixels, errorR, errorG, errorB);
            }

            destPixels[i] = alpha | (outRGB & 0x00FFFFFF);
        }

        return dest;
    }

    /**
     * Quantizes a float value to a specified number of levels within a given range.
     */
    private static float quantizeFloat(float value, int numLevels, float min, float max) {
        if (numLevels <= 1) {
            return min + (max - min) / 2.0f;
        }
        float range = max - min;

        // prevent division by zero.
        if (range == 0.0f) {
            return min;
        }

        // normalize value to 0-1 range
        float normalizedValue = (value - min) / range;
        // clamp to handle out-of-gamut values and ensure it's < 1.0 for level calculation
        normalizedValue = Math.max(0.0f, Math.min(normalizedValue, 0.999999f));

        // find the discrete level, an integer from 0 to numLevels-1
        int level = (int) (normalizedValue * numLevels);

        // map the discrete level back to a value in the 0-1 range
        float quantizedNormalized = (float) level / (numLevels - 1);

        // scale back to the original min-max range
        return min + quantizedNormalized * range;
    }

    private BufferedImage posterizeSrgbWithDithering(BufferedImage src, BufferedImage dest, RGBLookup rgbLookup) {
        double diffusionStrength = ditheringAmountParam.getPercentage();
        int ditheringMethod = ditheringMethodParam.getValue();

        BufferedImage input = ImageUtils.copyImage(src);
        int[] inputPixels = ImageUtils.getPixels(input);
        int[] srcPixels = ImageUtils.getPixels(src);
        int[] destPixels = ImageUtils.getPixels(dest);

        int width = src.getWidth();
        int numPixels = inputPixels.length;

        short[] redLUT = rgbLookup.getRedLUT();
        short[] greenLUT = rgbLookup.getGreenLUT();
        short[] blueLUT = rgbLookup.getBlueLUT();

        for (int i = 0; i < numPixels; i++) {
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
            Dithering.ditherRGB(ditheringMethod, inputPixels, i, width, numPixels, errorR, errorG, errorB);

            destPixels[i] = a | outR << 16 | outG << 8 | outB;
        }

        return dest;
    }

    @Override
    public boolean isAnimatable() {
        return false;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
