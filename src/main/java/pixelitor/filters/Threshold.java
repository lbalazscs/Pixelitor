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

import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.levels.Channel;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

import static com.jhlabs.image.ImageMath.clamp;
import static pixelitor.utils.Texts.i18n;

/**
 * The Threshold filter
 */
public class Threshold extends ParametrizedFilter {
    private static final String THRESHOLD = i18n("threshold");
    public static final String NAME = THRESHOLD;

    private final EnumParam<Channel> channelParam = Channel.asParam();
    private final RangeParam thresholdParam = new RangeParam(THRESHOLD, 0, 128, 255);
    private final RangeParam dithering = new RangeParam("Dithering Amount (%)", 0, 0, 100);

    public Threshold() {
        super(true);

        thresholdParam.setPresetKey("Threshold");

        setParams(channelParam, thresholdParam, dithering);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        boolean dither = dithering.getValue() != 0;
        double ditherStrength = dithering.getPercentage();
        BufferedImage input;
        if (dither) {
            input = ImageUtils.copyImage(src);
        } else {
            input = src;
        }

        double threshold = thresholdParam.getValueAsDouble();
        Channel channel = channelParam.getSelected();
        int[] inputData = ImageUtils.getPixelsAsArray(input);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        int width = src.getWidth();
        int length = inputData.length;
        for (int i = 0; i < length; i++) {
            int rgb = inputData[i];

            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;
            int out;

            double value = channel.getValue(r, g, b);
            if (value > threshold) {
                out = 255;
            } else {
                out = 0;
            }

            if (dither) {
                double error = (value - out) * ditherStrength;
                // Floyd–Steinberg dithering
                if (i + 1 < length) {
                    addError(inputData, i + 1, (int) (error * 7.0 / 16));
                }
                int bellowIndex = i + width;
                if (bellowIndex + 1 < length) {
                    addError(inputData, bellowIndex - 1, (int) (error * 3.0 / 16));
                    addError(inputData, bellowIndex, (int) (error * 5.0 / 16));
                    addError(inputData, bellowIndex + 1, (int) (error / 16));
                }
            }

            destData[i] = 0xFF << 24 | out << 16 | out << 8 | out;
        }

        return dest;
    }

    private static void addError(int[] pixels, int index, int value) {
        int rgb = pixels[index];
        int r = clamp(((rgb >>> 16) & 0xFF) + value, 0, 255);
        int g = clamp(((rgb >>> 8) & 0xFF) + value, 0, 255);
        int b = clamp((rgb & 0xFF) + value, 0, 255);
        pixels[index] = r << 16 | g << 8 | b;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
