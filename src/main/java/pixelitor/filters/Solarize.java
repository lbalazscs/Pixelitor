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

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Solarize
 */
public class Solarize extends ParametrizedFilter {
    public static final String NAME = "Solarize";

    @Serial
    private static final long serialVersionUID = -5483360370529563790L;

    private static final int TYPE_CLASSIC = 1; // pixels above the threshold level are inverted + contrast is maximized
    private static final int TYPE_INVERTED = 2; // upside down: corresponds to a V-shaped curves adjustment

    private final RangeParam redThreshold = new RangeParam("Red Threshold", 0, 128, 255);
    private final RangeParam greenThreshold = new RangeParam("Green Threshold", 0, 128, 255);
    private final RangeParam blueThreshold = new RangeParam("Blue Threshold", 0, 128, 255);

    private final IntChoiceParam type = new IntChoiceParam(GUIText.TYPE, new Item[]{
        new Item("Classic", TYPE_CLASSIC),
        new Item("Inverted", TYPE_INVERTED)
    });

    public Solarize() {
        super(true);

        type.setPresetKey("Type");

        initParams(
            type,
            redThreshold,
            greenThreshold,
            blueThreshold
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int[] srcPixels = ImageUtils.getPixels(src);
        int[] destPixels = ImageUtils.getPixels(dest);

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

        for (int i = 0, destPixelsLength = destPixels.length; i < destPixelsLength; i++) {
            int rgb = srcPixels[i];

            int a = (rgb >>> 24) & 0xFF;
            if (a == 0) {
                destPixels[i] = 0;
            } else {
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = rgb & 0xFF;

                int newR = redLookup[r];
                int newG = greenLookup[g];
                int newB = blueLookup[b];

                destPixels[i] = a << 24 | newR << 16 | newG << 8 | newB;
            }
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}