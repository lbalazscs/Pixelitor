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

import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.Arrays;

/**
 * Kuwahara filter.
 */
public class Kuwahara extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Kuwahara";

    private final RangeParam radiusParam = new RangeParam(
        "Radius", 1, 1, 10);

    public Kuwahara() {
        super(true);

        helpURL = "https://en.wikipedia.org/wiki/Kuwahara_filter";

        setParams(
            radiusParam
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.copyImage(src);

        int radius = radiusParam.getValue();

        int[] destPixels = ImageUtils.getPixels(dest);
        filterKuwahara(destPixels, dest.getWidth(), dest.getHeight(), radius);
        return dest;
    }

    private static void filterKuwahara(int[] array, int imgWidth, int imgHeight, int radius) {
        ProgressTracker pt = new StatusBarProgressTracker(NAME, imgHeight);

        int[] result = new int[array.length]; // stores the filtered image
        float[] hsv = new float[3];

        for (int y = 0; y < imgHeight; y++) {
            for (int x = 0; x < imgWidth; x++) {
                int index = y * imgWidth + x; // the index of the current pixel
                rgbToHsv(array[index], hsv);

                // get the brightness values in the four
                // sub-regions around the current pixel
                float[][] subRegions = getSubRegions(array, imgWidth, imgHeight, x, y, radius);

                // the central pixel will take the mean value of the sub-region
                // that is most homogenous (has the smallest variance)
                float minVariance = Float.MAX_VALUE;
                float minVarianceMean = 0;
                for (float[] subRegion : subRegions) {
                    if (subRegion.length == 0) {
                        continue; // skip empty subregions
                    }
                    float mean = calcMean(subRegion);
                    float variance = calcVariance(subRegion, mean);

                    if (variance < minVariance) {
                        minVariance = variance;
                        minVarianceMean = mean;
                    }
                }

                hsv[2] = minVarianceMean;
                result[index] = hsvToRgb(hsv);
            }
            pt.unitDone();
        }

        // copy the filtered image back to the original array
        System.arraycopy(result, 0, array, 0, array.length);
        pt.finished();
    }

    private static float[][] getSubRegions(int[] array, int imgWidth, int imgHeight, int x, int y, int radius) {
        float[][] regions = new float[4][];

        int offsetArea = (radius + 1) * (radius + 1);

        // starting coordinates for each sub-region
        int[][] startCoords = {
            {x - radius, y - radius}, {x, y - radius},
            {x - radius, y}, {x, y}
        };

        // iterate through each sub-region
        for (int regionIndex = 0; regionIndex < 4; regionIndex++) {
            float[] regionValues = new float[offsetArea];
            int validCount = 0;

            for (int i = 0; i <= radius; i++) {
                for (int j = 0; j <= radius; j++) {
                    // the coordinates of the current pixel in the sub-region
                    int currentX = startCoords[regionIndex][0] + i;
                    int currentY = startCoords[regionIndex][1] + j;

                    if (currentX >= 0 && currentX < imgWidth && currentY >= 0 && currentY < imgHeight) {
                        int index = currentY * imgWidth + currentX;
                        regionValues[validCount++] = rgbToBightness(array[index]);
                    }
                }
            }

            // trim to actual size
            regions[regionIndex] = Arrays.copyOf(regionValues, validCount);
        }
        return regions;
    }

    private static float calcMean(float[] values) {
        float sum = 0;
        for (float value : values) {
            sum += value;
        }
        return values.length > 0 ? sum / values.length : 0;
    }

    private static float calcVariance(float[] values, float mean) {
        float sumSquaredDifferences = 0;
        for (float value : values) {
            float diff = value - mean;
            sumSquaredDifferences += diff * diff;
        }
        return values.length > 0 ? sumSquaredDifferences / values.length : 0;
    }

    private static void rgbToHsv(int rgb, float[] hsv) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        Color.RGBtoHSB(red, green, blue, hsv);
    }

    private static float rgbToBightness(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        int max = Math.max(r, g);
        if (b > max) {
            max = b;
        }

        return ((float) max) / 255.0f;
    }

    private static int hsvToRgb(float[] hsv) {
        return Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]);
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}