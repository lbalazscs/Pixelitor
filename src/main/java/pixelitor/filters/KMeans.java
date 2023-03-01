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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.Dithering;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.KMeansClustering;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.Arrays;
import java.util.Random;

/**
 * Quantization filter based on k-means clustering.
 */
public class KMeans extends ParametrizedFilter {
    public static final String NAME = "K-Means Quantize";

    @Serial
    private static final long serialVersionUID = 1L;

    private final RangeParam k = new RangeParam(
        "Number of Colors", 2, 5, 10);
    private final RangeParam iterations = new RangeParam(
        "Iterations", 1, 5, 20);
    private final BooleanParam kpp = new BooleanParam(
        "PlusPlus", false);

    private final RangeParam diffusionStrengthParam = new RangeParam("Dithering Amount (%)", 0, 0, 100);
    private final IntChoiceParam ditheringMethodParam = Dithering.createDitheringChoices();

    public KMeans() {
        super(true);

        diffusionStrengthParam.setupEnableOtherIfNotZero(ditheringMethodParam);

        initParams(
            k,
            iterations,
            kpp,
            diffusionStrengthParam,
            ditheringMethodParam
        ).withReseedAction();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int[] srcPixels = ImageUtils.getPixels(src);
        int[] destPixels = ImageUtils.getPixels(dest);
        Random random = paramSet.getLastSeedRandom();
        int numIterations = iterations.getValue();

        boolean dither = diffusionStrengthParam.getValue() != 0;
        double diffusionStrength = diffusionStrengthParam.getPercentage();
        int ditheringMethod = ditheringMethodParam.getValue();

        var pt = new StatusBarProgressTracker(NAME, numIterations + 2);

        // if dithering, work on a copy of the source pixels to propagate errors
        int[] inputPixels = dither ? Arrays.copyOf(srcPixels, srcPixels.length) : srcPixels;

        // perform k-means clustering to find the color palette
        KMeansClustering kmeans = new KMeansClustering(k.getValue(), numIterations, random);
        int[] centroids = kmeans.cluster(srcPixels, kpp.isChecked(), pt);

        int width = src.getWidth();
        int length = destPixels.length;

        // map each pixel to the nearest color in the palette
        for (int i = 0; i < length; i++) {
            int inRGB = inputPixels[i];
            // preserve the original alpha channel
            int alpha = srcPixels[i] & 0xFF_00_00_00;

            // find the closest color in the palette
            int closestCentroidIndex = KMeansClustering.findClosestCentroidIndex(inRGB, centroids);
            int outRGB = centroids[closestCentroidIndex];

            if (dither) {
                // calculate the quantization error
                int inR = (inRGB >>> 16) & 0xFF;
                int inG = (inRGB >>> 8) & 0xFF;
                int inB = inRGB & 0xFF;

                int outR = (outRGB >>> 16) & 0xFF;
                int outG = (outRGB >>> 8) & 0xFF;
                int outB = outRGB & 0xFF;

                double errorR = (inR - outR) * diffusionStrength;
                double errorG = (inG - outG) * diffusionStrength;
                double errorB = (inB - outB) * diffusionStrength;

                // distribute the error to neighboring pixels
                Dithering.ditherRGB(ditheringMethod, inputPixels, i, width, length, errorR, errorG, errorB);
            }

            // combine the new RGB with the original alpha
            destPixels[i] = alpha | outRGB;
        }

        pt.unitDone();
        pt.finished();

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}