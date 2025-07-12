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
        ProgressTracker pt = new StatusBarProgressTracker(NAME, height);
        float[] hsv = new float[3];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // find the mean brightness of the most homogeneous sub-region around the current pixel
                float bestMean = findBestRegionMean(srcPixels, width, height, x, y, radius);

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
     * Calculates the mean brightness of the sub-region with the lowest variance.
     */
    private static float findBestRegionMean(int[] pixels, int width, int height, int cx, int cy, int radius) {
        // defines the top-left corners of the four overlapping sub-regions
        int[][] regionOrigins = {
            {cx - radius, cy - radius}, {cx, cy - radius},
            {cx - radius, cy}, {cx, cy}
        };

        float minVariance = Float.MAX_VALUE;
        float bestMean = 0.0f;

        // analyze each of the four sub-regions
        for (int[] origin : regionOrigins) {
            float sum = 0.0f;
            float sumSq = 0.0f;
            int count = 0;

            // iterate over the pixels in the current sub-region
            for (int y = 0; y <= radius; y++) {
                for (int x = 0; x <= radius; x++) {
                    int px = origin[0] + x;
                    int py = origin[1] + y;

                    // ensure the pixel is within the image bounds
                    if (px >= 0 && px < width && py >= 0 && py < height) {
                        int index = py * width + px;
                        float brightness = rgbToBrightness(pixels[index]);
                        sum += brightness;
                        sumSq += brightness * brightness;
                        count++;
                    }
                }
            }

            if (count == 0) {
                continue; // skip empty regions at image edges
            }

            float mean = sum / count;
            // variance = E[X²] - (E[X])²
            float variance = (sumSq / count) - (mean * mean);

            if (variance < minVariance) {
                minVariance = variance;
                bestMean = mean;
            }
        }

        // if all regions were empty, fall back to the original pixel's brightness
        if (minVariance == Float.MAX_VALUE) {
            return rgbToBrightness(pixels[cy * width + cx]);
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