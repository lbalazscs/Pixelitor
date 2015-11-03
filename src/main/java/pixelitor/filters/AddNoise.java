/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import com.jhlabs.image.ImageMath;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ReseedSupport;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Add Noise filter
 */
public class AddNoise extends FilterWithParametrizedGUI {
    private static final int METHOD_FASTER = 1;
    private static final int METHOD_COVERAGE_ANIM = 2;

    private final RangeParam opacity = new RangeParam("Opacity (%)", 0, 100, 100);
    private final RangeParam coverage = new RangeParam("Coverage (%)", 0, 50, 100);
    private final RangeParam saturation = new RangeParam("Saturation (%)", 0, 100, 100);
    private final IntChoiceParam method = new IntChoiceParam("Method", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Faster", METHOD_FASTER),
            new IntChoiceParam.Value("Smooth Coverage Animation", METHOD_COVERAGE_ANIM),
    });

    private final float[] tmpHSV = new float[3];

    public AddNoise() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                coverage,
                saturation,
                opacity,
                method
        ).withAction(ReseedSupport.createAction()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        ReseedSupport.reInitialize();
        Random rand = ReseedSupport.getRand();

        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        float opacityValueAsPercentage = opacity.getValueAsPercentage();
        int opacityValue = opacity.getValue();

        float saturationValueAsPercentage = saturation.getValueAsPercentage();
        int saturationValue = saturation.getValue();

        float coverageValue = coverage.getValueAsPercentage();

        boolean coverageAnim = method.getValue() == METHOD_COVERAGE_ANIM;

        for (int i = 0; i < destData.length; i++) {
            int srcRGB = srcData[i];

            int randomInt = 0;
            if (coverageAnim) {
                // generate random numbers for the discarded pixels as well
                randomInt = rand.nextInt();
            }

            float rn = rand.nextFloat();
            if (rn > coverageValue) {
                destData[i] = srcRGB;
                continue;
            }

            int sourceAlpha = 0xFF000000 & srcRGB;
            if (sourceAlpha == 0) { // for premultiplied
                destData[i] = 0;
                continue;
            }

            if (!coverageAnim) {
                // if coverage animation is not a requirement, then it is faster
                // to generate the random values only here, for the covered pixels
                randomInt = rand.nextInt();
            }

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
