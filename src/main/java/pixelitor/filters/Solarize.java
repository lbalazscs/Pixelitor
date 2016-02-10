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

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Solarize
 */
public class Solarize extends FilterWithParametrizedGUI {
    public static final String NAME = "Solarize";

    private static final int TYPE_CLASSIC = 1; // pixels above the threshold level are inverted + contrast is maximized
    private static final int TYPE_INVERTED = 2; // upside down: corresponds to a V-shaped curves adjustment

    private final RangeParam redThreshold = new RangeParam("Red Threshold", 0, 128, 255);
    private final RangeParam greenThreshold = new RangeParam("Green Threshold", 0, 128, 255);
    private final RangeParam blueThreshold = new RangeParam("Blue Threshold", 0, 128, 255);

    private final IntChoiceParam type = new IntChoiceParam("Type", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Classic", TYPE_CLASSIC),
            new IntChoiceParam.Value("Upside Down Curve", TYPE_INVERTED)
    }, IGNORE_RANDOMIZE);

    public Solarize() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                type,
                redThreshold,
                greenThreshold,
                blueThreshold
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        float redThr = redThreshold.getValueAsFloat();
        float greenThr = greenThreshold.getValueAsFloat();
        float blueThr = blueThreshold.getValueAsFloat();

        float m1Red = 255.0f / redThr;
        float m2Red = 255.0f / (255.0f - redThr);

        float m1Green = 255.0f / greenThr;
        float m2Green = 255.0f / (255.0f - greenThr);

        float m1Blue = 255.0f / blueThr;
        float m2Blue = 255.0f / (255.0f - blueThr);

        int[] redLookup = new int[256];
        int[] greenLookup = new int[256];
        int[] blueLookup = new int[256];

        int solarizeType = type.getValue();
        if (solarizeType == TYPE_CLASSIC) {
            for (int i = 0; i < 256; i++) {
                if (i > redThr) {
                    redLookup[i] = 255 - (int) (m2Red * (i - redThr));
                } else {
                    redLookup[i] = 255 - (int) (m1Red * (redThr - i));
                }

                if (i > greenThr) {
                    greenLookup[i] = 255 - (int) (m2Green * (i - greenThr));
                } else {
                    greenLookup[i] = 255 - (int) (m1Green * (greenThr - i));
                }

                if (i > blueThr) {
                    blueLookup[i] = 255 - (int) (m2Blue * (i - blueThr));
                } else {
                    blueLookup[i] = 255 - (int) (m1Blue * (blueThr - i));
                }
            }
        } else if (solarizeType == TYPE_INVERTED) {
            for (int i = 0; i < 256; i++) {
                if (i > redThr) {
                    redLookup[i] = (int) (m2Red * (i - redThr));
                } else {
                    redLookup[i] = (int) (m1Red * (redThr - i));
                }

                if (i > greenThr) {
                    greenLookup[i] = (int) (m2Green * (i - greenThr));
                } else {
                    greenLookup[i] = (int) (m1Green * (greenThr - i));
                }

                if (i > blueThr) {
                    blueLookup[i] = (int) (m2Blue * (i - blueThr));
                } else {
                    blueLookup[i] = (int) (m1Blue * (blueThr - i));
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

    @Override
    public boolean supportsGray() {
        return false;
    }
}