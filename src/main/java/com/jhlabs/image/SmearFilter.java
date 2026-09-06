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

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

/**
 * An image filter that creates a painted, smeared, or pointillist effect
 * by drawing numerous small, randomly placed shapes over the image.
 * <p>
 * The filter works by sampling a pixel's color from the original image
 * and drawing a shape (such as a line, cross, circle, square, diamond, hexagon,
 * triangle, 4-point star, or 5-point star) of that color over the neighboring pixels. The density,
 * maximum distance (size), and blending mix of these shapes can be customized.
 */
public class SmearFilter extends WholeImageFilter {
    public static final int CROSSES = 0;
    public static final int LINES = 1;
    public static final int CIRCLES = 2;
    public static final int SQUARES = 3;
    public static final int DIAMONDS = 4;
    public static final int HEXAGONS = 5;
    public static final int TRIANGLES = 6;
    public static final int ASTROIDS = 7;
    public static final int STARS = 8;

    private final float angle;
    private final float density;
    private final int distance;
    private final Random random;
    private final int shape;
    private final int mixInt;
    private final int invMixInt;

    /**
     * Constructs a new {@link SmearFilter}.
     *
     * @param filterName the name of the filter
     * @param shape      the shape used to smear pixels; one of
     *                   {@link #LINES}, {@link #CROSSES}, {@link #CIRCLES},
     *                   {@link #SQUARES}, {@link #DIAMONDS}, {@link #HEXAGONS},
     *                   {@link #TRIANGLES}, {@link #ASTROIDS}, or {@link #STARS}
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

        assert distance >= 0;
        assert density >= 0 && density <= 1;
        assert mix >= 0 && mix <= 1;

        this.shape = shape;
        this.distance = distance;
        this.density = density;
        this.angle = angle;
        this.mixInt = (int) (mix * 256);
        this.invMixInt = 256 - mixInt;
        this.random = random;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        int[] outPixels = new int[width * height];
        System.arraycopy(inPixels, 0, outPixels, 0, width * height);
        if (distance == 0 || density == 0.0f || mixInt == 0) {
            return outPixels;
        }

        switch (shape) {
            case CROSSES -> renderCrosses(width, height, inPixels, outPixels);
            case LINES -> renderLines(width, height, inPixels, outPixels);
            case SQUARES, CIRCLES, DIAMONDS, HEXAGONS, TRIANGLES,
                 ASTROIDS, STARS -> renderShapes(width, height, inPixels, outPixels);
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

        for (int i = 0; i < numShapes; i += stride) {
            int currentStride = Math.min(stride, numShapes - i);
            int[] xs = new int[currentStride];
            int[] ys = new int[currentStride];
            int[] lengths = new int[currentStride];

            for (int j = 0; j < currentStride; j++) {
                xs[j] = random.nextInt(width);
                ys[j] = random.nextInt(height);
                lengths[j] = random.nextInt(distance) + 1;
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

        for (int i = 0; i < numShapes; i += stride) {
            int currentStride = Math.min(stride, numShapes - i);
            int[] sxs = new int[currentStride];
            int[] sys = new int[currentStride];
            int[] lengths = new int[currentStride];

            for (int j = 0; j < currentStride; j++) {
                sxs[j] = random.nextInt(width);
                sys[j] = random.nextInt(height);
                lengths[j] = random.nextInt(distance);
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
        int offsetX = (int) (length * cos);
        int offsetY = (int) (length * sin);

        int a2 = rgb >>> 24;
        int r2 = (rgb >> 16) & 0xFF;
        int g2 = (rgb >> 8) & 0xFF;
        int b2 = rgb & 0xFF;

        int x0 = sx - offsetX;
        int y0 = sy - offsetY;
        int x1 = sx + offsetX;
        int y1 = sy + offsetY;

        // Bresenham's line algorithm from (x0, y0) to (x1, y1)
        int stepX = x1 < x0 ? -1 : 1;
        int stepY = y1 < y0 ? -1 : 1;
        int absDx = Math.abs(x1 - x0);
        int absDy = Math.abs(y1 - y0);
        int x = x0;
        int y = y0;

        if (x >= 0 && x < width && y >= 0 && y < height) {
            int offset = y * width + x;
            outPixels[offset] = ImageMath.mixColors(outPixels[offset], a2, r2, g2, b2, mixInt, invMixInt);
        }

        if (absDx > absDy) { // the line is more horizontal, so the X-axis is the driving axis
            int d = 2 * absDy - absDx;
            int incrE = 2 * absDy;
            int incrNE = 2 * (absDy - absDx);

            while (x != x1) {
                if (d <= 0) {
                    d += incrE;
                } else {
                    d += incrNE;
                    y += stepY;
                }
                x += stepX;
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    int offset = y * width + x;
                    outPixels[offset] = ImageMath.mixColors(outPixels[offset], a2, r2, g2, b2, mixInt, invMixInt);
                }
            }
        } else { // the line is more vertical, making the Y-axis the driving axis
            int d = 2 * absDx - absDy;
            int incrE = 2 * absDx;
            int incrNE = 2 * (absDx - absDy);

            while (y != y1) {
                if (d <= 0) {
                    d += incrE;
                } else {
                    d += incrNE;
                    x += stepX;
                }
                y += stepY;
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    int offset = y * width + x;
                    outPixels[offset] = ImageMath.mixColors(outPixels[offset], a2, r2, g2, b2, mixInt, invMixInt);
                }
            }
        }
    }

    private void renderShapes(int width, int height, int[] inPixels, int[] outPixels) {
        int radius = distance + 1;

        ShapeMask shapeMask = switch (shape) {
            case SQUARES -> createSquareMask(radius);
            case CIRCLES -> createCircleMask(radius);
            case DIAMONDS -> createDiamondMask(radius);
            case HEXAGONS -> createHexagonMask(radius);
            case TRIANGLES -> createTriangleMask(radius);
            case ASTROIDS -> createAstroidMask(radius);
            case STARS -> createStarMask(radius, angle);
            default -> throw new IllegalArgumentException("Unknown shape: " + shape);
        };

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
                    renderOneShape(width, height, inPixels, outPixels, shapeMask, sxs[j], sys[j]);
                }
            };
            futures.add(ThreadPool.submit(r));
        }
        ThreadPool.waitFor(futures, pt);
    }

    private void renderOneShape(int width, int height, int[] inPixels, int[] outPixels, ShapeMask shapeMask, int sx, int sy) {
        int rgb = inPixels[sy * width + sx];

        int a2 = rgb >>> 24;
        int r2 = (rgb >> 16) & 0xFF;
        int g2 = (rgb >> 8) & 0xFF;
        int b2 = rgb & 0xFF;

        int startX = sx - shapeMask.originX();
        int startY = sy - shapeMask.originY();
        int minX = Math.max(0, startX);
        int maxX = Math.min(width, startX + shapeMask.width());
        int minY = Math.max(0, startY);
        int maxY = Math.min(height, startY + shapeMask.height());

        boolean[][] mask = shapeMask.mask();
        for (int y = minY; y < maxY; y++) {
            int maskY = y - startY;
            boolean[] maskRow = mask[maskY];
            int yOffset = y * width;
            for (int x = minX; x < maxX; x++) {
                int maskX = x - startX;
                if (maskRow[maskX]) {
                    int offset = yOffset + x;
                    outPixels[offset] = ImageMath.mixColors(outPixels[offset], a2, r2, g2, b2, mixInt, invMixInt);
                }
            }
        }
    }

    private static ShapeMask createSquareMask(int radius) {
        int size = 2 * radius + 1;
        boolean[][] mask = new boolean[size][size];
        for (int y = 0; y < size; y++) {
            Arrays.fill(mask[y], true);
        }
        return new ShapeMask(size, size, radius, radius, mask);
    }

    private static ShapeMask createCircleMask(int radius) {
        int size = 2 * radius + 1;
        int radius2 = radius * radius;
        boolean[][] mask = new boolean[size][size];

        for (int y = 0; y < size; y++) {
            int dy = y - radius;
            int dy2 = dy * dy;
            for (int x = 0; x < size; x++) {
                int dx = x - radius;
                if (dx * dx + dy2 <= radius2) {
                    mask[y][x] = true;
                }
            }
        }

        return new ShapeMask(size, size, radius, radius, mask);
    }

    private static ShapeMask createDiamondMask(int radius) {
        int size = 2 * radius + 1;
        boolean[][] mask = new boolean[size][size];

        for (int y = 0; y < size; y++) {
            int dy = Math.abs(y - radius);
            for (int x = 0; x < size; x++) {
                int dx = Math.abs(x - radius);
                if (dx + dy <= radius) {
                    mask[y][x] = true;
                }
            }
        }

        return new ShapeMask(size, size, radius, radius, mask);
    }

    private static ShapeMask createHexagonMask(int radius) {
        int size = 2 * radius + 1;
        boolean[][] mask = new boolean[size][size];
        double h = radius * ImageMath.COS_30;

        for (int y = 0; y < size; y++) {
            int dy = Math.abs(y - radius);
            for (int x = 0; x < size; x++) {
                int dx = Math.abs(x - radius);
                if (dy <= h && (dx * ImageMath.COS_30 + dy * 0.5) <= h) {
                    mask[y][x] = true;
                }
            }
        }

        return new ShapeMask(size, size, radius, radius, mask);
    }

    private static ShapeMask createTriangleMask(int radius) {
        int size = 2 * radius + 1;
        boolean[][] mask = new boolean[size][size];

        for (int y = 0; y < size; y++) {
            int dy = y - radius;
            for (int x = 0; x < size; x++) {
                int dx = Math.abs(x - radius);
                if (dy >= -radius && dy <= radius / 2 && (dx * ImageMath.SQRT_3 - dy <= radius)) {
                    mask[y][x] = true;
                }
            }
        }

        // TODO the triangle should be rotated around its centroid
        return new ShapeMask(size, size, radius, radius, mask);
    }

    private static ShapeMask createAstroidMask(int radius) {
        int size = 2 * radius + 1;
        boolean[][] mask = new boolean[size][size];
        double sqrtRadius = Math.sqrt(radius);

        for (int y = 0; y < size; y++) {
            int dy = Math.abs(y - radius);
            double sqrtDy = Math.sqrt(dy);
            for (int x = 0; x < size; x++) {
                int dx = Math.abs(x - radius);
                if (dx + dy <= radius && Math.sqrt(dx) + sqrtDy <= sqrtRadius) {
                    mask[y][x] = true;
                }
            }
        }

        return new ShapeMask(size, size, radius, radius, mask);
    }

    private static ShapeMask createStarMask(int radius, double startAngle) {
        // regular pentagram inner radius ratio: (3 - sqrt(5)) / 2 ≈ 0.381966
        double innerRadius = radius * ((3.0 - Math.sqrt(5.0)) / 2.0);

        Path2D.Double star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double angleOffset = -Math.PI / 2.0 + i * (Math.PI / 5.0);
            double r = (i % 2 == 0) ? radius : innerRadius;
            double angle = startAngle + angleOffset;
            double x = radius + r * Math.cos(angle);
            double y = radius + r * Math.sin(angle);
            if (i == 0) {
                star.moveTo(x, y);
            } else {
                star.lineTo(x, y);
            }
        }
        star.closePath();

        return ShapeMask.fromShape(star, radius);
    }

    public record ShapeMask(int width, int height, int originX, int originY, boolean[][] mask) {
        public static ShapeMask fromShape(Shape shape, int radius) {
            int size = 2 * radius + 1;
            boolean[][] mask = new boolean[size][size];

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    mask[y][x] = shape.contains(x, y);
                }
            }

            return new ShapeMask(size, size, radius, radius, mask);
        }
    }
}
