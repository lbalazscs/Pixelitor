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
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;
import pixelitor.utils.Dithering;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.utils.Dithering.DITHER_BURKES;
import static pixelitor.utils.Dithering.DITHER_FLOYD_STEINBERG;
import static pixelitor.utils.Dithering.DITHER_SIERRA;
import static pixelitor.utils.Dithering.DITHER_STUCKI;
import static pixelitor.utils.Dithering.ditherBurkes;
import static pixelitor.utils.Dithering.ditherFloydSteinberg;
import static pixelitor.utils.Dithering.ditherSierra;
import static pixelitor.utils.Dithering.ditherStucki;
import static pixelitor.utils.Texts.i18n;

/**
 * The Threshold filter
 */
public class Threshold extends ParametrizedFilter {
    private static final String THRESHOLD = i18n("threshold");
    public static final String NAME = THRESHOLD;

    @Serial
    private static final long serialVersionUID = 3739055511694844941L;

    private final EnumParam<Channel> channelParam = Channel.asParam(ColorSpace.SRGB);
    private final RangeParam thresholdParam = new RangeParam(THRESHOLD, 0, 128, 255);
    private final RangeParam diffusionStrengthParam = new RangeParam("Dithering Amount (%)", 0, 0, 100);
    private final IntChoiceParam ditheringMethodParam = Dithering.createDitheringChoices();

    public Threshold() {
        super(true);

        thresholdParam.setPresetKey("Threshold");

        diffusionStrengthParam.setupEnableOtherIfNotZero(ditheringMethodParam);

        initParams(
            thresholdParam,
            channelParam,
            diffusionStrengthParam,
            ditheringMethodParam);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        boolean dither = diffusionStrengthParam.isNotZero();
        double diffusionStrength = diffusionStrengthParam.getPercentage();

        // a copy of the input is needed because error diffusion modifies the pixel data
        BufferedImage input = dither ? ImageUtils.copyImage(src) : src;

        double threshold = thresholdParam.getValueAsDouble();
        int ditheringMethod = ditheringMethodParam.getValue();

        Channel channel = channelParam.getSelected();
        int[] inputPixels = ImageUtils.getPixels(input);
        int[] destPixels = ImageUtils.getPixels(dest);
        int[] srcPixels = ImageUtils.getPixels(src);

        int width = src.getWidth();
        int length = inputPixels.length;
        for (int i = 0; i < length; i++) {
            int rgb = inputPixels[i];

            int a = srcPixels[i] & 0xFF_00_00_00; // the original, and not shifted
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;
            int out;

            double intensity = channel.getIntensity(r, g, b);
            if (intensity > threshold) {
                out = 0xFF;
            } else {
                out = 0;
            }

            if (dither) {
                // the diffusion strength controls how much of the
                // quantization error is diffused to neighboring pixels
                double error = (intensity - out) * diffusionStrength;

                // Distribute the error to neighboring pixels.
                // These methods only modify the inputPixels array.
                // This will have an effect in the next iteration of the for loop.
                switch (ditheringMethod) {
                    case DITHER_FLOYD_STEINBERG:
                        ditherFloydSteinberg(inputPixels, i, width, length, error);
                        break;
                    case DITHER_STUCKI:
                        ditherStucki(inputPixels, i, width, length, error);
                        break;
                    case DITHER_BURKES:
                        ditherBurkes(inputPixels, i, width, length, error);
                        break;
                    case DITHER_SIERRA:
                        ditherSierra(inputPixels, i, width, length, error);
                        break;
                }
            }

            destPixels[i] = a | out << 16 | out << 8 | out;
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
