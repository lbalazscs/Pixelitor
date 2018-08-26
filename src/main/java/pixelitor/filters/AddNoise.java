/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.IntChoiceParam.Value;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.ReseedSupport;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

/**
 * Add Noise filter
 */
public class AddNoise extends ParametrizedFilter {
    public static final String NAME = "Add Noise";

    private static final int METHOD_FASTER = 1;
    private static final int METHOD_COVERAGE_ANIM = 2;

    private final RangeParam opacityParam = new RangeParam("Opacity (%)", 0, 100, 100);
    private final RangeParam coverageParam = new RangeParam("Coverage (%)", 0, 50, 100);
    private final RangeParam saturationParam = new RangeParam("Saturation (%)", 0, 100, 100);
    private final IntChoiceParam method = new IntChoiceParam("Method", new Value[]{
            new Value("Faster", METHOD_FASTER),
            new Value("Smooth Coverage Animation", METHOD_COVERAGE_ANIM),
    });

    private final float[] tmpHSV = new float[3];

    public AddNoise() {
        super(ShowOriginal.YES);

        setParams(
                coverageParam,
                saturationParam,
                opacityParam,
                method
        ).withAction(ReseedSupport.createAction());
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        Random rand = ReseedSupport.reInitialize();

        if (src.getType() == TYPE_BYTE_GRAY) {
            return addNoiseToGray(src, dest, rand);
        }

        boolean coverageAnim = method.getValue() == METHOD_COVERAGE_ANIM;
        return addNoiseToRGB(src, dest, coverageAnim, rand);
    }

    private BufferedImage addNoiseToRGB(BufferedImage src, BufferedImage dest,
                                        boolean coverageAnim, Random rand) {
        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);
        int length = destData.length;

        boolean fullSaturation = saturationParam.getValue() == 100;
        boolean fullOpacity = opacityParam.getValue() == 100;

        float opacityPercentage = opacityParam.getValueAsPercentage();
        float saturationPercentage = saturationParam.getValueAsPercentage();
        float coveragePercentage = coverageParam.getValueAsPercentage();

        int workUnit = 100_000;
        int counter = 0;
        int numWorkUnits = length / workUnit;
        ProgressTracker pt = new StatusBarProgressTracker(NAME, numWorkUnits);

        for (int i = 0; i < length; i++) {
            // count at the beginning of the loop because of early returns
            counter++;
            if (counter == workUnit) {
                counter = 0;
                pt.unitDone();
            }

            int srcRGB = srcData[i];

            int randomInt = 0;
            if (coverageAnim) {
                // generate random numbers for the discarded pixels as well
                randomInt = rand.nextInt();
            }

            float rn = rand.nextFloat();
            if (rn > coveragePercentage) {
                destData[i] = srcRGB;
                continue;
            }

            int sourceAlpha = 0xFF000000 & srcRGB;
            if (sourceAlpha == 0) {
                destData[i] = 0;
                continue;
            }

            if (!coverageAnim) {
                // if coverage animation is not a requirement, then it is faster
                // to generate the random values only here, for the covered pixels
                randomInt = rand.nextInt();
            }

            if (fullSaturation) {
                // make the alpha channel the same as for the source
                randomInt |= sourceAlpha;

                if (fullOpacity) {
                    // if we have full saturation (the default), then we can
                    // just use the random pixel as it is if the opacity is also 100...
                    destData[i] = randomInt;
                } else {
                    // ...or mix the random pixel with the source according to
                    // the opacity
                    destData[i] = ImageMath.mixColors(opacityPercentage, srcRGB, randomInt);
                }
            } else { // desaturate the random pixel
                int r = (randomInt >>> 16) & 0xFF;
                int g = (randomInt >>> 8) & 0xFF;
                int b = (randomInt) & 0xFF;

                Color.RGBtoHSB(r, g, b, tmpHSV);
                float newSaturation = ImageMath.lerp(saturationPercentage, 0.0f, tmpHSV[1]);
                randomInt = Color.HSBtoRGB(tmpHSV[0], newSaturation, tmpHSV[2]);

                // make the alpha channel the same as for the source
                randomInt |= sourceAlpha;

                if (fullOpacity) {
                    destData[i] = randomInt;
                } else {
                    destData[i] = ImageMath.mixColors(opacityPercentage, srcRGB, randomInt);
                }
            }
        }
        pt.finish();

        return dest;
    }

    private BufferedImage addNoiseToGray(BufferedImage src, BufferedImage dest,
                                         Random rand) {
        byte[] srcPixels = ImageUtils.getGrayPixelsAsByteArray(src);
        byte[] destPixels = ImageUtils.getGrayPixelsAsByteArray(dest);

        // fill the dest with random values
        rand.nextBytes(destPixels);

        boolean fullOpacity = opacityParam.getValue() == 100;
        boolean fullCoverage = coverageParam.getValue() == 100;
        float coveragePercentage = coverageParam.getValueAsPercentage();

        if (fullOpacity && fullCoverage) {
            return dest;
        }

        int length = destPixels.length;
        int workUnit = 100_000;
        int counter = 0;
        int numWorkUnits = length / workUnit;
        ProgressTracker pt = new StatusBarProgressTracker(NAME, numWorkUnits);

        double destWeight = opacityParam.getValueAsPercentage();
        double srcWeight = 1.0 - destWeight;
        for (int i = 0; i < length; i++) {
            // count at the beginning of the loop because of early returns
            counter++;
            if (counter == workUnit) {
                counter = 0;
                pt.unitDone();
            }

            float rn = rand.nextFloat();
            byte srcPixel = srcPixels[i];
            if (rn > coveragePercentage) {
                destPixels[i] = srcPixel;
                continue;
            }

            int d = Byte.toUnsignedInt(destPixels[i]);
            int s = Byte.toUnsignedInt(srcPixel);
            int v = (int) (d * destWeight + s * srcWeight);

            destPixels[i] = (byte) v;
        }
        pt.finish();

        return dest;
    }
}
