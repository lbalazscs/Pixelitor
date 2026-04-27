/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.image;

import pixelitor.ThreadPool;

import java.awt.image.BufferedImage;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * A filter that finds the brightest spots (highlights) in an image
 * and draw 8-pointed starbursts ("glints") on top of them.
 */
public class GlintFilter extends AbstractBufferedImageOp {
    private final int threshold;
    private final int length;
    private final float blur;
    private final float amount;
    private final boolean glintOnly;
    private final Colormap colormap;
    private final float coverage;

    private final int transparentEdgeColor;

    /**
     * Creates a new {@code GlintFilter}.
     *
     * @param filterName the name of the filter (used for progress tracking/logging)
     * @param threshold  the threshold value in the range [0, 1]; pixels brighter than this
     *                   are considered highlights
     * @param coverage   the probability in the range [0, 1] that a highlight pixel
     *                   will generate a glint
     * @param amount     the intensity of the glint in the range [0, 1]
     * @param length     the length of the starburst rays
     * @param blur       the blur radius applied before thresholding
     * @param colormap   the colormap used to color the glints
     * @param glintOnly  if true, only the glints are rendered
     */
    public GlintFilter(String filterName,
                       float threshold,
                       float coverage,
                       float amount,
                       int length,
                       float blur,
                       Colormap colormap,
                       boolean glintOnly) {
        super(filterName);

        this.threshold = (int) (255 * threshold);
        this.coverage = coverage;
        this.amount = amount;
        this.length = length;
        this.blur = blur;
        this.colormap = colormap;
        this.glintOnly = glintOnly;

        // use the outer edge of the colormap (1.0f) for the transparent background
        this.transparentEdgeColor = colormap.getColor(1.0f) & 0x00_FF_FF_FF;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        if (blur != 0) {
            // width+height for the blur, then height again for further processing
            pt = createProgressTracker(width + 2 * height);
        } else {
            pt = createProgressTracker(height);
        }

        // prevent division by 0
        int diagonalLength = Math.max(1, (int) (length / 1.414f));

        // the pre-calculated colors of the starburst arms
        int[] orthogonalColors = calcArmColors(length);
        int[] diagonalColors = calcArmColors(diagonalLength);

        // by extracting the highlights to a dedicated mask,
        // the highlights can be blurred before drawing the glints
        BufferedImage mask = new BufferedImage(width, height, TYPE_INT_ARGB);

        int rgbThresholdSum = threshold * 3;
        int[] pixels = new int[width];
        for (int y = 0; y < height; y++) {
            getRGB(src, 0, y, width, 1, pixels);
            for (int x = 0; x < width; x++) {
                int rgb = pixels[x];
                int a = rgb & 0xFF_00_00_00;
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int l = r + g + b;
                if (l < rgbThresholdSum) {
                    // the mask is black if the lightness is lower than the threshold
                    pixels[x] = 0xFF_00_00_00;
                } else {
                    // the mask is set to a grayscale value representing the lightness
                    l /= 3;
                    pixels[x] = a | (l << 16) | (l << 8) | l;
                }
            }
            setRGB(mask, 0, y, width, 1, pixels);
        }

        if (blur != 0) {
            AbstractBufferedImageOp blurFilter;
            if (blur > 3) {
                blurFilter = new BoxBlurFilter(filterName, blur, blur, 3);
            } else {
                blurFilter = new GaussianFilter(filterName, blur);
            }
            blurFilter.setProgressTracker(pt);
            mask = blurFilter.filter(mask, null);
        }

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }
        int[] dstPixels;
        if (glintOnly) {
            dstPixels = new int[width * height];
        } else {
            dstPixels = getRGB(src, 0, 0, width, height, null);
        }

        // use staggered execution phases to prevent thread contention
        int maxSpread = Math.max(length, diagonalLength);
        int phaseCount = 2 * maxSpread + 1;

        for (int phase = 0; phase < phaseCount; phase++) {
            if (phase >= height) {
                break;
            }
            int rowsInPhase = (height - phase - 1) / phaseCount + 1;
            Future<?>[] rowFutures = new Future[rowsInPhase];
            int idx = 0;

            for (int y = phase; y < height; y += phaseCount) {
                int finalY = y;
                BufferedImage finalMask = mask;
                Runnable rowTask = () -> processRow(width, height, diagonalLength, orthogonalColors, diagonalColors, finalMask, dstPixels, finalY);
                rowFutures[idx++] = ThreadPool.submit(rowTask);
            }
            ThreadPool.waitFor(rowFutures, pt);
        }

        if (glintOnly) {
            postProcessGlintOnly(dstPixels);
        }

        setRGB(dst, 0, 0, width, height, dstPixels);

        finishProgressTracker();

        return dst;
    }

    private void postProcessGlintOnly(int[] dstPixels) {
        for (int i = 0; i < dstPixels.length; i++) {
            int p = dstPixels[i];
            int a = (p >>> 24);

            if (a == 0) {
                // fill completely empty areas with the transparent gradient-edge color
                dstPixels[i] = transparentEdgeColor;
            } else if (a < 255) {
                // un-premultiply: this mathematically recovers the EXACT gradient color
                // for this specific pixel, naturally handling arbitrary gradients and overlaps
                int r = Math.min(255, (((p >> 16) & 0xFF) * 255) / a);
                int g = Math.min(255, (((p >> 8) & 0xFF) * 255) / a);
                int b = Math.min(255, ((p & 0xFF) * 255) / a);

                dstPixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
    }

    private int[] calcArmColors(int length) {
        int[] colors = new int[length + 1];
        for (int i = 0; i <= length; i++) {
            int argb = colormap.getColor((float) i / length);
            int originalAlpha = argb >>> 24;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;

            // to work properly on transparent backgrounds, alpha should fade with brightness
            int brightness = Math.max(r, Math.max(g, b));
            int a = (originalAlpha * brightness) / 255;

            colors[i] = ((int) (amount * a) << 24) |
                ((int) (amount * r) << 16) |
                ((int) (amount * g) << 8) |
                (int) (amount * b);
        }
        return colors;
    }

    private void processRow(int width, int height, int diagonalLength,
                            int[] orthogonalColors, int[] diagonalColors,
                            BufferedImage mask, int[] dstPixels, int y) {
        int index = y * width;
        int[] pixels = new int[width];
        getRGB(mask, 0, y, width, 1, pixels);
        int ymin = Math.max(y - length, 0) - y;
        int ymax = Math.min(y + length, height - 1) - y;
        int ymin2 = Math.max(y - diagonalLength, 0) - y;
        int ymax2 = Math.min(y + diagonalLength, height - 1) - y;

        // draws 8-pointed starbursts (glints) pixel-by-pixel,
        // projecting outwards from a center point
        for (int x = 0; x < width; x++) {
            if ((pixels[x] & 0xFF) > threshold && (coverage > ThreadLocalRandom.current().nextFloat())) {
                int xmin = Math.max(x - length, 0) - x;
                int xmax = Math.min(x + length, width - 1) - x;
                int xmin2 = Math.max(x - diagonalLength, 0) - x;
                int xmax2 = Math.min(x + diagonalLength, width - 1) - x;

                // horizontal
                for (int i = 0, k = 0; i <= xmax; i++, k++) {
                    dstPixels[index + i] = PixelUtils.addPixels(dstPixels[index + i], orthogonalColors[k]);
                }
                for (int i = -1, k = 1; i >= xmin; i--, k++) {
                    dstPixels[index + i] = PixelUtils.addPixels(dstPixels[index + i], orthogonalColors[k]);
                }
                // vertical
                for (int i = 1, j = index + width, k = 1; i <= ymax; i++, j += width, k++) {
                    dstPixels[j] = PixelUtils.addPixels(dstPixels[j], orthogonalColors[k]);
                }
                for (int i = -1, j = index - width, k = 1; i >= ymin; i--, j -= width, k++) {
                    dstPixels[j] = PixelUtils.addPixels(dstPixels[j], orthogonalColors[k]);
                }

                // diagonals
                // SE
                int count = Math.min(xmax2, ymax2);
                for (int i = 1, j = index + width + 1, k = 1; i <= count; i++, j += width + 1, k++) {
                    dstPixels[j] = PixelUtils.addPixels(dstPixels[j], diagonalColors[k]);
                }
                // NW
                count = Math.min(-xmin2, -ymin2);
                for (int i = 1, j = index - width - 1, k = 1; i <= count; i++, j -= width + 1, k++) {
                    dstPixels[j] = PixelUtils.addPixels(dstPixels[j], diagonalColors[k]);
                }
                // NE
                count = Math.min(xmax2, -ymin2);
                for (int i = 1, j = index - width + 1, k = 1; i <= count; i++, j += -width + 1, k++) {
                    dstPixels[j] = PixelUtils.addPixels(dstPixels[j], diagonalColors[k]);
                }
                // SW
                count = Math.min(-xmin2, ymax2);
                for (int i = 1, j = index + width - 1, k = 1; i <= count; i++, j += width - 1, k++) {
                    dstPixels[j] = PixelUtils.addPixels(dstPixels[j], diagonalColors[k]);
                }
            }
            index++;
        }
    }
}
