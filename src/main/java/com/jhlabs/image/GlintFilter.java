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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * A filter that finds the brightest spots (highlights) in an image
 * and draws 8-pointed starbursts ("glints") on top of them.
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
    private final int diagonalLength;
    private final int[] orthogonalColors;
    private final int[] diagonalColors;

    /**
     * Creates a new {@code GlintFilter}.
     *
     * @param filterName the name of the filter (used for progress tracking/logging)
     * @param threshold  the threshold value in the range [0, 1]; pixels brighter than this
     *                   are considered highlights
     * @param coverage   the probability in the range [0, 1] that a highlight pixel
     *                   will generate a glint
     * @param amount     the intensity of the glint in the range [0, 1]
     * @param length     the length of the starburst rays in pixels
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

        assert threshold >= 0 && threshold <= 1;
        assert coverage >= 0 && coverage <= 1;
        assert amount >= 0 && amount <= 1;
        assert length >= 1;
        assert blur >= 0;

        this.threshold = (int) (255 * threshold);
        this.coverage = coverage;
        this.amount = amount;
        this.length = length;
        this.blur = blur;
        this.colormap = colormap;
        this.glintOnly = glintOnly;

        // use the outer edge of the colormap (1.0f) for the transparent background
        this.transparentEdgeColor = colormap.getColor(1.0f) & 0x00_FF_FF_FF;

        // the diagonal rays use a reduced per axis length so that
        // their Euclidean length matches the orthogonal rays
        this.diagonalLength = Math.max(1, (int) (length / ImageMath.SQRT_2));
        this.orthogonalColors = createRayColors(length);
        this.diagonalColors = createRayColors(diagonalLength);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        int threadCount = ThreadPool.getThreadCount();
        int bandHeight = Math.max(2 * length, (height + 2 * threadCount - 1) / (2 * threadCount));
        int bandCount = (height + bandHeight - 1) / bandHeight;

        if (blur != 0) {
            // width+height for the blur, then bandCount for glint processing
            pt = createProgressTracker(width + height + bandCount);
        } else {
            pt = createProgressTracker(bandCount);
        }

        BufferedImage mask = createHighlightMask(src);

        int[] dstPixels = glintOnly
            ? new int[width * height]
            : getRGB(src, 0, 0, width, height, null);

        drawGlints(mask, dstPixels, width, height, bandHeight, bandCount);

        if (glintOnly) {
            postProcessGlintOnly(dstPixels);
        }

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }
        setRGB(dst, 0, 0, width, height, dstPixels);

        finishProgressTracker();

        return dst;
    }

    /**
     * Extracts the highlight mask from the source image and optionally blurs it.
     */
    private BufferedImage createHighlightMask(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();

        // by extracting the highlights to a dedicated mask,
        // the highlights can be blurred before drawing the glints
        BufferedImage mask = new BufferedImage(width, height, TYPE_INT_ARGB);

        int[] pixels = new int[width];
        for (int y = 0; y < height; y++) {
            getRGB(src, 0, y, width, 1, pixels);
            for (int x = 0; x < width; x++) {
                int rgb = pixels[x];
                int lum = ImageMath.calcLuminanceInt(rgb);
                if (lum < threshold) {
                    // the mask is black if the lightness is lower than the threshold
                    pixels[x] = 0xFF_00_00_00;
                } else {
                    // the mask is set to an opaque grayscale value representing the lightness
                    pixels[x] = 0xFF_00_00_00 | (lum << 16) | (lum << 8) | lum;
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

        return mask;
    }

    /**
     * Draws glints on the destination pixels based on highlights in the mask
     * using two banded phases (even bands, then odd bands) to prevent write contention.
     */
    private void drawGlints(BufferedImage mask, int[] dstPixels, int width, int height,
                            int bandHeight, int bandCount) {
        // splitting into bands of height >= 2 * length ensures that bands of the
        // same parity write to non-overlapping rows and can safely run in parallel
        for (int parity = 0; parity < 2; parity++) {
            List<Future<?>> futures = new ArrayList<>();
            for (int band = parity; band < bandCount; band += 2) {
                int yStart = band * bandHeight;
                int yEnd = Math.min(yStart + bandHeight, height);
                futures.add(ThreadPool.submit(() -> {
                    // reuse a single row buffer across all rows in this band
                    int[] maskPixels = new int[width];
                    for (int y = yStart; y < yEnd; y++) {
                        processRow(mask, dstPixels, maskPixels, width, height, y);
                    }
                }));
            }
            ThreadPool.waitFor(futures.toArray(new Future<?>[0]), pt);
        }
    }

    private void postProcessGlintOnly(int[] dstPixels) {
        for (int i = 0; i < dstPixels.length; i++) {
            int p = dstPixels[i];
            int a = (p >>> 24);

            if (a == 0) {
                // fill completely empty areas with the transparent gradient-edge color
                dstPixels[i] = transparentEdgeColor;
            } else if (a < 255) {
                // The accumulated color is interpreted as premultiplied ARGB
                // and converted to straight alpha. The clamp is needed because
                // saturating additions can leave a channel larger than alpha.
                int r = Math.min(255, (((p >> 16) & 0xFF) * 255) / a);
                int g = Math.min(255, (((p >> 8) & 0xFF) * 255) / a);
                int b = Math.min(255, ((p & 0xFF) * 255) / a);

                dstPixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
    }

    // ray lengths must be >= 1 to prevent division by 0.
    private int[] createRayColors(int rayLength) {
        int[] colors = new int[rayLength + 1];
        for (int i = 0; i <= rayLength; i++) {
            int argb = colormap.getColor((float) i / rayLength);
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

    private void processRow(BufferedImage mask, int[] dstPixels, int[] maskPixels,
                            int width, int height, int y) {
        int rowStart = y * width;
        getRGB(mask, 0, y, width, 1, maskPixels);

        int up = Math.min(length, y);
        int down = Math.min(length, height - 1 - y);
        int diagUp = Math.min(diagonalLength, y);
        int diagDown = Math.min(diagonalLength, height - 1 - y);

        // draws 8-pointed starbursts (glints) pixel-by-pixel,
        // projecting outwards from a center point
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int x = 0; x < width; x++) {
            if ((maskPixels[x] & 0xFF) > threshold && (coverage > random.nextFloat())) {
                int left = Math.min(length, x);
                int right = Math.min(length, width - 1 - x);
                int diagLeft = Math.min(diagonalLength, x);
                int diagRight = Math.min(diagonalLength, width - 1 - x);

                int center = rowStart + x;
                dstPixels[center] = PixelUtils.addPixels(dstPixels[center], orthogonalColors[0]);
                drawRay(dstPixels, center, 1, right, orthogonalColors);
                drawRay(dstPixels, center, -1, left, orthogonalColors);
                drawRay(dstPixels, center, width, down, orthogonalColors);
                drawRay(dstPixels, center, -width, up, orthogonalColors);
                drawRay(dstPixels, center, width + 1, Math.min(diagRight, diagDown), diagonalColors); // SE
                drawRay(dstPixels, center, -width - 1, Math.min(diagLeft, diagUp), diagonalColors);   // NW
                drawRay(dstPixels, center, 1 - width, Math.min(diagRight, diagUp), diagonalColors);   // NE
                drawRay(dstPixels, center, width - 1, Math.min(diagLeft, diagDown), diagonalColors);  // SW
            }
        }
    }

    private static void drawRay(int[] dst, int center, int step, int rayLength, int[] colors) {
        for (int k = 1, p = center + step; k <= rayLength; k++, p += step) {
            dst[p] = PixelUtils.addPixels(dst[p], colors[k]);
        }
    }
}
