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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

/**
 * An image filter that creates a painted, smeared, or pointillist effect
 * by drawing numerous small, randomly placed shapes over the image.
 * <p>
 * The filter works by sampling a pixel's color from the original image
 * and drawing a shape (such as a line, cross, circle, square, or diamond)
 * of that color over the neighboring pixels. The density, maximum
 * distance (size), and blending mix of these shapes can be customized.
 */
public class SmearFilter extends WholeImageFilter {
    public static final int CROSSES = 0;
    public static final int LINES = 1;
    public static final int CIRCLES = 2;
    public static final int SQUARES = 3;
    public static final int DIAMONDS = 4;

    private final float angle;
    private final float density;
    private final int distance;
    private final Random random;
    private final int shape;
    private final float mix;

    /**
     * Constructs a new {@link SmearFilter}.
     *
     * @param filterName the name of the filter
     * @param shape      the shape used to smear pixels; one of
     *                   {@link #LINES}, {@link #CROSSES}, {@link #CIRCLES},
     *                   {@link #SQUARES}, or {@link #DIAMONDS}
     * @param distance   the maximum size (radius or half-length) of each
     *                   drawn shape, in pixels; must be positive
     * @param density    the relative number of shapes drawn per unit area, in the range [0, 1]
     * @param angle      the orientation of the shapes in radians; only
     *                   meaningful when {@code shape} is {@link #LINES}
     * @param mix        the blending weight of each drawn shape over the
     *                   existing pixels, in the range [0, 1]
     * @param random     the {@link Random} instance used for placing shapes
     */
    public SmearFilter(String filterName, int shape, int distance,
                       float density, float angle, float mix, Random random) {
        super(filterName);
        this.shape = shape;
        this.distance = distance;
        this.density = density;
        this.angle = angle;
        this.mix = mix;
        this.random = random;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        int[] outPixels = new int[width * height];
        System.arraycopy(inPixels, 0, outPixels, 0, width * height);

        switch (shape) {
            case CROSSES -> renderCrosses(width, height, inPixels, outPixels);
            case LINES -> renderLines(width, height, inPixels, outPixels);
            case SQUARES, CIRCLES, DIAMONDS -> renderShapes(width, height, inPixels, outPixels);
        }

        finishProgressTracker();

        return outPixels;
    }

    private void renderCrosses(int width, int height, int[] inPixels, int[] outPixels) {
        int numShapes = (int) (2 * density * width * height / (distance + 1));

        int stride = numShapes / 100 + 1;
        int workUnits = (numShapes + stride - 1) / stride;
        pt = createProgressTracker(workUnits);

        List<Future<?>> futures = new ArrayList<>(workUnits);

        int maxDistance = Math.max(1, distance);

        for (int i = 0; i < numShapes; i += stride) {
            int currentStride = Math.min(stride, numShapes - i);
            int[] xs = new int[currentStride];
            int[] ys = new int[currentStride];
            int[] lengths = new int[currentStride];

            for (int j = 0; j < currentStride; j++) {
                xs[j] = random.nextInt(width);
                ys[j] = random.nextInt(height);
                lengths[j] = random.nextInt(maxDistance) + 1;
            }

            Runnable r = () -> {
                for (int j = 0; j < currentStride; j++) {
                    renderOneCross(width, height, inPixels, outPixels, xs[j], ys[j], lengths[j]);
                }
            };
            futures.add(ThreadPool.submit(r));
        }
        ThreadPool.waitFor(futures, pt);
    }

    private void renderOneCross(int width, int height, int[] inPixels, int[] outPixels, int x, int y, int length) {
        int rgb = inPixels[y * width + x];

        int a2 = rgb >>> 24;
        int r2 = (rgb >> 16) & 0xFF;
        int g2 = (rgb >> 8) & 0xFF;
        int b2 = rgb & 0xFF;

        int mixInt = (int) (mix * 256);
        int invMixInt = 256 - mixInt;

        int startX = Math.max(0, x - length);
        int endX = Math.min(width - 1, x + length);
        int yOffset = y * width;
        for (int x1 = startX; x1 <= endX; x1++) {
            int offset = yOffset + x1;
            outPixels[offset] = ImageMath.mixColors(outPixels[offset], a2, r2, g2, b2, mixInt, invMixInt);
        }

        int startY = Math.max(0, y - length);
        int endY = Math.min(height - 1, y + length);
        for (int y1 = startY; y1 <= endY; y1++) {
            int offset = y1 * width + x;
            outPixels[offset] = ImageMath.mixColors(outPixels[offset], a2, r2, g2, b2, mixInt, invMixInt);
        }
    }

    private void renderLines(int width, int height, int[] inPixels, int[] outPixels) {
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);

        int numShapes = (int) (density * width * height);

        int stride = numShapes / 100 + 1;
        int workUnits = (numShapes + stride - 1) / stride;
        pt = createProgressTracker(workUnits);

        List<Future<?>> futures = new ArrayList<>(workUnits);

        int maxDistance = Math.max(1, distance);

        for (int i = 0; i < numShapes; i += stride) {
            int currentStride = Math.min(stride, numShapes - i);
            int[] sxs = new int[currentStride];
            int[] sys = new int[currentStride];
            int[] lengths = new int[currentStride];

            for (int j = 0; j < currentStride; j++) {
                sxs[j] = random.nextInt(width);
                sys[j] = random.nextInt(height);
                lengths[j] = random.nextInt(maxDistance);
            }

            Runnable r = () -> {
                for (int j = 0; j < currentStride; j++) {
                    renderOneLine(width, height, inPixels, outPixels, sin, cos, sxs[j], sys[j], lengths[j]);
                }
            };
            futures.add(ThreadPool.submit(r));
        }
        ThreadPool.waitFor(futures, pt);
    }

    private void renderOneLine(int width, int height, int[] inPixels, int[] outPixels, float sin, float cos, int sx, int sy, int length) {
        int rgb = inPixels[sy * width + sx];
        int dx = (int) (length * cos);
        int dy = (int) (length * sin);

        int a2 = rgb >>> 24;
        int r2 = (rgb >> 16) & 0xFF;
        int g2 = (rgb >> 8) & 0xFF;
        int b2 = rgb & 0xFF;

        int mixInt = (int) (mix * 256);
        int invMixInt = 256 - mixInt;

        int x0 = sx - dx;
        int y0 = sy - dy;
        int x1 = sx + dx;
        int y1 = sy + dy;

        int ddx = x1 < x0 ? -1 : 1;
        int ddy = y1 < y0 ? -1 : 1;
        dx = Math.abs(x1 - x0);
        dy = Math.abs(y1 - y0);
        int x = x0;
        int y = y0;

        if (x >= 0 && x < width && y >= 0 && y < height) {
            int offset = y * width + x;
            outPixels[offset] = ImageMath.mixColors(outPixels[offset], a2, r2, g2, b2, mixInt, invMixInt);
        }

        if (dx > dy) {
            int d = 2 * dy - dx;
            int incrE = 2 * dy;
            int incrNE = 2 * (dy - dx);

            while (x != x1) {
                if (d <= 0) {
                    d += incrE;
                } else {
                    d += incrNE;
                    y += ddy;
                }
                x += ddx;
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    int offset = y * width + x;
                    outPixels[offset] = ImageMath.mixColors(outPixels[offset], a2, r2, g2, b2, mixInt, invMixInt);
                }
            }
        } else {
            int d = 2 * dx - dy;
            int incrE = 2 * dx;
            int incrNE = 2 * (dx - dy);

            while (y != y1) {
                if (d <= 0) {
                    d += incrE;
                } else {
                    d += incrNE;
                    x += ddx;
                }
                y += ddy;
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    int offset = y * width + x;
                    outPixels[offset] = ImageMath.mixColors(outPixels[offset], a2, r2, g2, b2, mixInt, invMixInt);
                }
            }
        }
    }

    private void renderShapes(int width, int height, int[] inPixels, int[] outPixels) {
        int radius = distance + 1;
        int radius2 = radius * radius;

        int numShapes = (int) (2 * density * width * height / radius);

        int stride = numShapes / 100 + 1;
        int workUnits = (numShapes + stride - 1) / stride;
        pt = createProgressTracker(workUnits);

        List<Future<?>> futures = new ArrayList<>(workUnits);

        for (int i = 0; i < numShapes; i += stride) {
            int currentStride = Math.min(stride, numShapes - i);
            int[] sxs = new int[currentStride];
            int[] sys = new int[currentStride];

            for (int j = 0; j < currentStride; j++) {
                sxs[j] = random.nextInt(width);
                sys[j] = random.nextInt(height);
            }

            Runnable r = () -> {
                for (int j = 0; j < currentStride; j++) {
                    renderOneShape(width, height, inPixels, outPixels, radius, radius2, sxs[j], sys[j]);
                }
            };
            futures.add(ThreadPool.submit(r));
        }
        ThreadPool.waitFor(futures, pt);
    }

    private void renderOneShape(int width, int height, int[] inPixels, int[] outPixels, int radius, int radius2, int sx, int sy) {
        int rgb = inPixels[sy * width + sx];

        int a2 = rgb >>> 24;
        int r2 = (rgb >> 16) & 0xFF;
        int g2 = (rgb >> 8) & 0xFF;
        int b2 = rgb & 0xFF;

        int mixInt = (int) (mix * 256);
        int invMixInt = 256 - mixInt;

        int minSx = Math.max(0, sx - radius);
        int maxSx = Math.min(width, sx + radius + 1);
        int minSy = Math.max(0, sy - radius);
        int maxSy = Math.min(height, sy + radius + 1);

        switch (shape) {
            case CIRCLES ->
                makeCircle(width, outPixels, radius2, sx, sy, a2, r2, g2, b2, mixInt, invMixInt, minSx, maxSx, minSy, maxSy);
            case SQUARES -> makeSquare(width, outPixels, a2, r2, g2, b2, mixInt, invMixInt, minSx, maxSx, minSy, maxSy);
            case DIAMONDS ->
                makeDiamond(width, outPixels, radius, sx, sy, a2, r2, g2, b2, mixInt, invMixInt, minSx, maxSx, minSy, maxSy);
        }
    }

    private static void makeCircle(int width, int[] outPixels, int radius2, int sx, int sy, int a2, int r2, int g2, int b2, int mixInt, int invMixInt, int minSx, int maxSx, int minSy, int maxSy) {
        for (int y = minSy; y < maxSy; y++) {
            int dsy = y - sy;
            int dsy2 = dsy * dsy;
            int yOffset = y * width;
            for (int x = minSx; x < maxSx; x++) {
                int dsx = x - sx;
                if (dsx * dsx + dsy2 <= radius2) {
                    int offset = yOffset + x;
                    outPixels[offset] = ImageMath.mixColors(outPixels[offset], a2, r2, g2, b2, mixInt, invMixInt);
                }
            }
        }
    }

    private static void makeSquare(int width, int[] outPixels, int a2, int r2, int g2, int b2, int mixInt, int invMixInt, int minSx, int maxSx, int minSy, int maxSy) {
        for (int y = minSy; y < maxSy; y++) {
            int offset = y * width + minSx;
            for (int x = minSx; x < maxSx; x++) {
                outPixels[offset] = ImageMath.mixColors(outPixels[offset], a2, r2, g2, b2, mixInt, invMixInt);
                offset++;
            }
        }
    }

    private static void makeDiamond(int width, int[] outPixels, int radius, int sx, int sy, int a2, int r2, int g2, int b2, int mixInt, int invMixInt, int minSx, int maxSx, int minSy, int maxSy) {
        for (int y = minSy; y < maxSy; y++) {
            int dy = Math.abs(y - sy);
            int yOffset = y * width;
            for (int x = minSx; x < maxSx; x++) {
                int dx = Math.abs(x - sx);
                if (dx + dy <= radius) {
                    int offset = yOffset + x;
                    outPixels[offset] = ImageMath.mixColors(outPixels[offset], a2, r2, g2, b2, mixInt, invMixInt);
                }
            }
        }
    }
}
