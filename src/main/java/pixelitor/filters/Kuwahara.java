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

import pixelitor.filters.gui.Help;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;

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

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Kuwahara_filter");

        initParams(
            radiusParam
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        // ensure the destination image is a distinct copy of the source
        dest = ImageUtils.copyImage(src);

        int radius = radiusParam.getValue();
        int width = src.getWidth();
        int height = src.getHeight();

        int[] srcPixels = ImageUtils.getPixels(src);
        int[] destPixels = ImageUtils.getPixels(dest);

        apply(srcPixels, destPixels, width, height, radius);

        return dest;
    }

    /**
     * Applies the Kuwahara filter, reading from the source pixels and writing to the destination pixels.
     */
    private static void apply(int[] srcPixels, int[] destPixels, int width, int height, int radius) {
        ProgressTracker pt = new StatusBarProgressTracker(NAME, height + 2);

        // pre-calculate brightness for all pixels
        float[][] brightnesses = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                brightnesses[y][x] = rgbToBrightness(srcPixels[y * width + x]);
            }
        }
        pt.unitDone();

        // pre-compute integral images for sum and sum-of-squares
        double[][] integralSum = new double[height + 1][width + 1];
        double[][] integralSumSq = new double[height + 1][width + 1];
        computeIntegralImages(brightnesses, width, height, integralSum, integralSumSq);
        pt.unitDone();

        float[] hsv = new float[3];

        // main filter loop
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // find the mean brightness of the most homogeneous sub-region around the current pixel
                float bestMean = findBestRegionMean(integralSum, integralSumSq, width, height, x, y, radius);

                // preserve the original hue and saturation, but use the new brightness
                int index = y * width + x;
                rgbToHsv(srcPixels[index], hsv);
                hsv[2] = bestMean;
                destPixels[index] = hsvToRgb(hsv);
            }
            pt.unitDone();
        }
        pt.finished();
    }

    /**
     * Computes integral images for both the sum and the sum of squares of brightness values.
     */
    private static void computeIntegralImages(float[][] brightnesses, int width, int height, double[][] integralSum, double[][] integralSumSq) {
        for (int y = 0; y < height; y++) {
            double rowSum = 0.0;
            double rowSumSq = 0.0;
            for (int x = 0; x < width; x++) {
                float b = brightnesses[y][x];
                rowSum += b;
                rowSumSq += b * b;
                // uses padded (height+1, width+1) arrays to simplify boundary checks later
                integralSum[y + 1][x + 1] = rowSum + integralSum[y][x + 1];
                integralSumSq[y + 1][x + 1] = rowSumSq + integralSumSq[y][x + 1];
            }
        }
    }

    /**
     * Calculates the sum of values within a given rectangular region in O(1) time
     * using a pre-computed integral image.
     *
     * @param integralImage The padded (height+1, width+1) integral image.
     * @param x1            The starting x-coordinate of the region (inclusive).
     * @param y1            The starting y-coordinate of the region (inclusive).
     * @param x2            The ending x-coordinate of the region (inclusive).
     * @param y2            The ending y-coordinate of the region (inclusive).
     * @return The sum of values in the specified rectangle.
     */
    private static double getRegionSum(double[][] integralImage, int x1, int y1, int x2, int y2) {
        // I[y+1][x+1] corresponds to the sum of the rectangle from origin to original pixel (x,y)
        return integralImage[y2 + 1][x2 + 1] - integralImage[y1][x2 + 1] - integralImage[y2 + 1][x1] + integralImage[y1][x1];
    }

    /**
     * Calculates the mean brightness of the sub-region with the lowest variance using integral images.
     */
    private static float findBestRegionMean(double[][] integralSum, double[][] integralSumSq, int width, int height, int cx, int cy, int radius) {
        // the top-left corners of the four overlapping sub-regions
        int[][] regionOrigins = {
            {cx - radius, cy - radius}, {cx, cy - radius},
            {cx - radius, cy}, {cx, cy}
        };

        float minVariance = Float.MAX_VALUE;
        float bestMean = 0.0f;

        // analyze each of the four sub-regions
        for (int[] origin : regionOrigins) {
            // define the sub-region boundaries, clamped to the image dimensions
            int x1 = Math.max(0, origin[0]);
            int y1 = Math.max(0, origin[1]);
            int x2 = Math.min(width - 1, origin[0] + radius);
            int y2 = Math.min(height - 1, origin[1] + radius);

            // skip if the region is entirely outside the image
            if (x1 > x2 || y1 > y2) {
                continue;
            }

            int count = (x2 - x1 + 1) * (y2 - y1 + 1);
            if (count == 0) {
                continue;
            }

            // calculate sum and sum of squares in O(1) using the integral images
            double sum = getRegionSum(integralSum, x1, y1, x2, y2);
            double sumSq = getRegionSum(integralSumSq, x1, y1, x2, y2);

            float mean = (float) (sum / count);
            // variance = E[X²] - (E[X])²
            float variance = (float) (sumSq / count) - (mean * mean);

            if (variance < minVariance) {
                minVariance = variance;
                bestMean = mean;
            }
        }

        return bestMean;
    }

    private static void rgbToHsv(int rgb, float[] hsv) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        Color.RGBtoHSB(red, green, blue, hsv);
    }

    private static float rgbToBrightness(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        // brightness is the 'value' component of the HSV/HSB color model
        return Math.max(r, Math.max(g, b)) / 255.0f;
    }

    private static int hsvToRgb(float[] hsv) {
        return Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]);
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}