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

import com.jhlabs.image.ImageMath;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Add Noise filter
 */
public class AddNoise extends FilterWithParametrizedGUI {
    private final RangeParam opacity = new RangeParam("Opacity (%)", 0, 100, 100);
    private final RangeParam coverage = new RangeParam("Coverage (%)", 0, 100, 50);
    private final RangeParam saturation = new RangeParam("Saturation (%)", 0, 100, 100);

    private float[] tmpHSV = new float[3];

    public AddNoise() {
        super("Add Noise", true, false);
        setParamSet(new ParamSet(
                coverage,
                saturation,
                opacity
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        float opacityValueAsPercentage = opacity.getValueAsPercentage();
        int opacityValue = opacity.getValue();

        float saturationValueAsPercentage = saturation.getValueAsPercentage();
        int saturationValue = saturation.getValue();

        float coverageValue = coverage.getValueAsPercentage();

        Random random = new Random();
        for (int i = 0; i < destData.length; i++) {
            int srcRGB = srcData[i];

            float rn = random.nextFloat();
            if (rn > coverageValue) {
                destData[i] = srcRGB;
                continue;
            }

            int sourceAlpha = 0xFF000000 & srcRGB;
            if (sourceAlpha == 0) { // for premultiplied
                destData[i] = 0;
                continue;
            }

            int randomInt = random.nextInt();


            if(saturationValue == 100) {
                // make the alpha channel the same as for the source
                randomInt |= sourceAlpha;
                if (opacityValue == 100) {
                    destData[i] = randomInt;
                } else {
                    destData[i] = ImageMath.mixColors(opacityValueAsPercentage, srcRGB, randomInt);
                }
            } else {
                int r = (randomInt >>> 16) & 0xFF;
                int g = (randomInt >>> 8) & 0xFF;
                int b = (randomInt) & 0xFF;

                Color.RGBtoHSB(r, g, b, tmpHSV);
                float newSaturation = ImageMath.lerp(saturationValueAsPercentage, 0.0f, tmpHSV[1]);
                randomInt = Color.HSBtoRGB(tmpHSV[0], newSaturation, tmpHSV[2]);

                // make the alpha channel the same as for the source
                randomInt |= sourceAlpha;

                if (opacityValue == 100) {
                    destData[i] = randomInt;
                } else {
                    destData[i] = ImageMath.mixColors(opacityValueAsPercentage, srcRGB, randomInt);
                }
            }
        }

        return dest;
    }
}
