/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.progress.StatusBarProgressTracker;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.random.RandomGenerator;

import static pixelitor.gui.GUIText.OPACITY;
import static pixelitor.utils.ImageUtils.isGrayscale;

/**
 * The "Add Noise" filter.
 */
public class AddNoise extends ParametrizedFilter {
    public static final String NAME = "Add Noise";

    @Serial
    private static final long serialVersionUID = 3647024991920186929L;

    private static final int METHOD_FASTER = 1;
    private static final int METHOD_COVERAGE_ANIM = 2;
    private static final int WORK_UNIT = 100_000;

    private final RangeParam opacityParam = new RangeParam(OPACITY, 0, 100, 100);
    private final RangeParam coverageParam = new RangeParam("Coverage (%)", 0, 50, 100);
    private final RangeParam saturationParam = new RangeParam("Saturation (%)", 0, 100, 100);
    private final IntChoiceParam methodParam = new IntChoiceParam("Method", new Item[]{
        new Item("Faster", METHOD_FASTER),
        new Item("Smooth Coverage Animation", METHOD_COVERAGE_ANIM),
    });

    public AddNoise() {
        super(true);

        opacityParam.setPresetKey("Opacity (%)");

        initParams(
            coverageParam,
            saturationParam,
            opacityParam,
            methodParam
        ).withReseedAction();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        RandomGenerator rand = paramSet.getSRandomWithLastSeed();

        return isGrayscale(src)
            ? addNoiseToGray(src, dest, rand)
            : addNoiseToRGB(src, dest, rand);
    }

    private BufferedImage addNoiseToRGB(BufferedImage src, BufferedImage dest, RandomGenerator rand) {
        int[] srcPixels = ImageUtils.getPixels(src);
        int[] destPixels = ImageUtils.getPixels(dest);
        int numPixels = destPixels.length;

        boolean coverageAnim = methodParam.hasValue(METHOD_COVERAGE_ANIM);
        boolean fullSaturation = saturationParam.getValue() == 100;
        boolean fullOpacity = opacityParam.getValue() == 100;

        float opacity = (float) opacityParam.getPercentage();
        float saturation = (float) saturationParam.getPercentage();
        double coverage = coverageParam.getPercentage();

        int counter = 0;
        int numWorkUnits = numPixels / WORK_UNIT;
        var pt = StatusBarProgressTracker.create(NAME, numWorkUnits);

        float[] tmpHSV = new float[3];

        for (int i = 0; i < numPixels; i++) {
            // count at the beginning of the loop because of early returns
            counter++;
            if (counter == WORK_UNIT) {
                counter = 0;
                pt.unitDone();
            }

            int randomARGB = 0;
            if (coverageAnim) {
                // generate random numbers for the discarded pixels as well
                randomARGB = rand.nextInt();
            }

            int srcARGB = srcPixels[i];
            if (rand.nextDouble() > coverage) {
                destPixels[i] = srcARGB;
                continue;
            }

            int sourceAlpha = 0xFF_00_00_00 & srcARGB;
            if (sourceAlpha == 0) {
                destPixels[i] = srcARGB;
                continue;
            }

            if (!coverageAnim) {
                // if coverage animation isn't a requirement, then it is faster
                // to generate the random values only here, for the covered pixels
                randomARGB = rand.nextInt();
            }

            if (!fullSaturation) {
                // desaturate the random pixel
                int r = (randomARGB >>> 16) & 0xFF;
                int g = (randomARGB >>> 8) & 0xFF;
                int b = randomARGB & 0xFF;

                Color.RGBtoHSB(r, g, b, tmpHSV);
                float newSaturation = ImageMath.lerp(saturation, 0.0f, tmpHSV[1]);
                randomARGB = Color.HSBtoRGB(tmpHSV[0], newSaturation, tmpHSV[2]);
            }

            // mask out the generated alpha bits before applying the source alpha
            randomARGB = (randomARGB & 0x00_FF_FF_FF) | sourceAlpha;

            if (fullOpacity) {
                destPixels[i] = randomARGB;
            } else {
                destPixels[i] = ImageMath.mixColors(opacity, srcARGB, randomARGB);
            }
        }
        pt.finished();

        return dest;
    }

    private BufferedImage addNoiseToGray(BufferedImage src, BufferedImage dest,
                                         RandomGenerator rand) {
        byte[] srcPixels = ImageUtils.getGrayPixels(src);
        byte[] destPixels = ImageUtils.getGrayPixels(dest);

        // fill the dest with random values (as a consequence, even
        // the fast method will have smooth coverage animation)
        rand.nextBytes(destPixels);

        boolean fullOpacity = opacityParam.getValue() == 100;
        boolean fullCoverage = coverageParam.getValue() == 100;
        double coverage = coverageParam.getPercentage();

        if (fullOpacity && fullCoverage) {
            return dest;
        }

        int numPixels = destPixels.length;
        int counter = 0;
        int numWorkUnits = numPixels / WORK_UNIT;
        var pt = StatusBarProgressTracker.create(NAME, numWorkUnits);

        double destWeight = opacityParam.getPercentage();
        for (int i = 0; i < numPixels; i++) {
            // count at the beginning of the loop because of early returns
            counter++;
            if (counter == WORK_UNIT) {
                counter = 0;
                pt.unitDone();
            }

            byte srcPixel = srcPixels[i];
            if (rand.nextDouble() > coverage) {
                destPixels[i] = srcPixel;
                continue;
            }

            int d = Byte.toUnsignedInt(destPixels[i]);
            int s = Byte.toUnsignedInt(srcPixel);
            destPixels[i] = (byte) ImageMath.lerp(destWeight, s, d);
        }
        pt.finished();

        return dest;
    }
}
