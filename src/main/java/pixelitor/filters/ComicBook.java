/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.BoxBlurFilter;
import org.jdesktop.swingx.graphics.ColorUtilities;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ComicBook extends ParametrizedFilter {
    public static final String NAME = "Comic Book";

    private final RangeParam stepsParam = new RangeParam("Color Steps", 1, 4, 20);
    private final RangeParam detailParam = new RangeParam("Detail", 1, 90, 100);
    private final RangeParam edgesParam = new RangeParam("Edge Amount", 0, 10, 100);

    // Essentially threshold is a value in the range [0-256]. For any value of
    // bias below the threshold, the corresponding color will be set to Black.
    // However, only values around [100-130] seems visually logical.
    // Therefore, this parameter focuses on picking fine values within that range.
    private final RangeParam thresholdParam = new RangeParam("Threshold", 0, 85, 100);

    public ComicBook() {
        super(true);

        setParams(stepsParam, detailParam, edgesParam, thresholdParam);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (src.getWidth() == 1 || src.getHeight() == 1) {
            // avoids ArrayIndexOutOfBoundsException in BoxBlurFilter
            return src;
        }

        float blurRadius = 30.0f - detailParam.getValue() / 100.0f * 30.0f;
        BufferedImage blurredImg = blur(src, blurRadius);

        BufferedImage edgesImg = edges(blurredImg, edgesParam.getValue());
        BufferedImage grayImg = gray(edgesImg);
        BufferedImage stairedImg = stairs(blurredImg, stepsParam.getValue());

        int threshold = (int) (100 + 30 * thresholdParam.getPercentage());
        BufferedImage finalImg = threshold(grayImg, stairedImg, threshold);

        return finalImg;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }

    public static BufferedImage blur(BufferedImage src, float radius) {
        BoxBlurFilter blur = new BoxBlurFilter(radius, radius, 1, NAME);
        blur.setPremultiplyAlpha(false);
        return blur.filter(src, null);
    }

    public static BufferedImage edges(BufferedImage src, float radius) {
        BufferedImage blurredI = blur(src, radius);
        return ImageUtils.getHighPassFilteredImage(src, blurredI);
    }

    public static BufferedImage gray(BufferedImage src) {
        BufferedImage out = ImageUtils.createImageWithSameCM(src);

        int[] src_pixels = ImageUtils.getPixelsAsArray(src);
        int[] out_pixels = ImageUtils.getPixelsAsArray(out);

        for (int i = 0; i < src_pixels.length; i++) {
            int rgb = src_pixels[i];
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;
            r = (r + g + b) / 3;
            rgb = packSingleValue(r);
            out_pixels[i] = rgb;
        }

        return out;
    }

    public static BufferedImage stairs(BufferedImage src, int stair_steps) {
        Stairs stairs = new Stairs(stair_steps);
        BufferedImage out = ImageUtils.copyImage(src);
        int[] pixels = ImageUtils.getPixelsAsArray(out);
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

            float[] hsl = ColorUtilities.RGBtoHSL(r, g, b);
            hsl[2] = stairs.get(hsl[2]);

            int[] rgbs = ColorUtilities.HSLtoRGB(hsl[0], hsl[1], hsl[2], null);
            r = rgbs[0];
            g = rgbs[1];
            b = rgbs[2];

            rgb = (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
            pixels[i] = rgb;
        }
        return out;
    }

    public static BufferedImage threshold(BufferedImage bias_src, BufferedImage src, int threshold) {
        BufferedImage out = ImageUtils.copyImage(src);
        int[] bias_pix = ImageUtils.getPixelsAsArray(bias_src);
        int[] out_pix = ImageUtils.getPixelsAsArray(out);

        int blackRGB = Color.BLACK.getRGB();
        for (int i = 0; i < bias_pix.length; i++) {
            int bias = bias_pix[i] & 0xFF; // Just the b is enough because here we know that the image is gray scaled.
            if (bias < threshold) {
                out_pix[i] = blackRGB;
            }
        }

        return out;
    }

    public static int packSingleValue(int c) {
        return (0xFF << 24) | ((c & 0xFF) << 16) | ((c & 0xFF) << 8) | (c & 0xFF);
    }

    public static class Stairs {
        private final float step_size;
        private final float[] points;

        public Stairs(int stairs) {
            this.step_size = 1.0f / stairs;
            float start = (step_size / 2);
            points = new float[stairs];

            int stepIndex = 0;
            for (; stepIndex < stairs; stepIndex++) {
                points[stepIndex] = start + stepIndex * step_size;
            }
        }

        public float get(float v) {
            for (int i = 0; i < points.length; i++) {
                if (v < points[i]) {
                    return i * step_size;
                }
            }
            return 1;
        }
    }
}
