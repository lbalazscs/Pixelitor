/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Solarize
 */
public class Solarize extends FilterWithParametrizedGUI {
    private static final int TYPE_CLASSIC = 1; // pixels above the threshold level are inverted + contrast is maximized
    private static final int TYPE_INVERTED = 2; // upside down: corresponds to a V-shaped curves adjustment

    private final RangeParam redThresholdParam = new RangeParam("Red Threshold", 0, 255, 128);
    private final RangeParam greenThresholdParam = new RangeParam("Green Threshold", 0, 255, 128);
    private final RangeParam blueThresholdParam = new RangeParam("Blue Threshold", 0, 255, 128);

    private final IntChoiceParam type = new IntChoiceParam("Type", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Classic", TYPE_CLASSIC),
            new IntChoiceParam.Value("Upside Down Curve", TYPE_INVERTED)
    }, true);

    public Solarize() {
        super("Solarize", true, false);
        setParamSet(new ParamSet(
                type,
                redThresholdParam,
                greenThresholdParam,
                blueThresholdParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        int redThreshold = redThresholdParam.getValue();
        int greenThreshold = greenThresholdParam.getValue();
        int blueThreshold = blueThresholdParam.getValue();

        float m1Red = 255.0f / redThreshold;
        float m2Red = 255.0f / (255.0f - redThreshold);

        float m1Green = 255.0f / greenThreshold;
        float m2Green = 255.0f / (255.0f - greenThreshold);

        float m1Blue = 255.0f / blueThreshold;
        float m2Blue = 255.0f / (255.0f - blueThreshold);

        int[] redLookup = new int[256];
        int[] greenLookup = new int[256];
        int[] blueLookup = new int[256];

        int solarizeType = type.getValue();
        if (solarizeType == TYPE_CLASSIC) {
            for (int i = 0; i < 256; i++) {
                if (i > redThreshold) {
                    redLookup[i] = 255 - (int) (m2Red * (i - redThreshold));
                } else {
                    redLookup[i] = 255 - (int) (m1Red * (redThreshold - i));
                }

                if (i > greenThreshold) {
                    greenLookup[i] = 255 - (int) (m2Green * (i - greenThreshold));
                } else {
                    greenLookup[i] = 255 - (int) (m1Green * (greenThreshold - i));
                }

                if (i > blueThreshold) {
                    blueLookup[i] = 255 - (int) (m2Blue * (i - blueThreshold));
                } else {
                    blueLookup[i] = 255 - (int) (m1Blue * (blueThreshold - i));
                }
            }
        } else if (solarizeType == TYPE_INVERTED) {
            for (int i = 0; i < 256; i++) {
                if (i > redThreshold) {
                    redLookup[i] = (int) (m2Red * (i - redThreshold));
                } else {
                    redLookup[i] = (int) (m1Red * (redThreshold - i));
                }

                if (i > greenThreshold) {
                    greenLookup[i] = (int) (m2Green * (i - greenThreshold));
                } else {
                    greenLookup[i] = (int) (m1Green * (greenThreshold - i));
                }

                if (i > blueThreshold) {
                    blueLookup[i] = (int) (m2Blue * (i - blueThreshold));
                } else {
                    blueLookup[i] = (int) (m1Blue * (blueThreshold - i));
                }
            }
        }

        for (int i = 0, destDataLength = destData.length; i < destDataLength; i++) {
            int rgb = srcData[i];

            int a = (rgb >>> 24) & 0xFF;
            if (a == 0) {
                destData[i] = 0;
            } else {
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = (rgb) & 0xFF;

                int newR = redLookup[r];
                int newG = greenLookup[g];
                int newB = blueLookup[b];

                destData[i] = (a << 24) | (newR << 16) | (newG << 8) | newB;
            }
        }

        return dest;
    }
}