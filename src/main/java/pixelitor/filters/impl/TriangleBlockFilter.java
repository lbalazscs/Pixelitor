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

package pixelitor.filters.impl;

import com.jhlabs.image.AbstractBufferedImageOp;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;

import static com.jhlabs.image.ImageMath.HALF_SQRT_3;
import static com.jhlabs.image.ImageMath.SQRT_3;

/**
 * Pixelates an image using a grid of equilateral triangles,
 * filling each triangle with its average color.
 */
public class TriangleBlockFilter extends AbstractBufferedImageOp {
    private final int size;

    private record Cache(int width, int height, int size, int[] indices, Rectangle[] bounds, int numX, int numY) {
    }

    private static volatile SoftReference<Cache> cacheRef = new SoftReference<>(null);

    public TriangleBlockFilter(String filterName, int size) {
        super(filterName);

        this.size = size;
    }

    private Cache getOrCreateCache(int width, int height) {
        Cache cache = cacheRef.get();
        if (cache != null && cache.width() == width && cache.height() == height && cache.size() == size) {
            return cache;
        }

        double h = size * HALF_SQRT_3;
        int numY = (int) Math.ceil(height / h);

        int minColIndex = Integer.MAX_VALUE;
        int maxColIndex = Integer.MIN_VALUE;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point idx = getTriangleIndex(x, y, size);
                minColIndex = Math.min(minColIndex, idx.x);
                maxColIndex = Math.max(maxColIndex, idx.x);
            }
        }

        int numX = maxColIndex - minColIndex + 1;
        int[] indices = new int[width * height];
        Rectangle[] bounds = new Rectangle[numX * numY];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point idx = getTriangleIndex(x, y, size);
                int adjustedCol = idx.x - minColIndex;
                int triangleId = adjustedCol + idx.y * numX;

                indices[y * width + x] = triangleId;

                if (bounds[triangleId] == null) {
                    bounds[triangleId] = new Rectangle(x, y, 1, 1);
                } else {
                    bounds[triangleId].add(x, y);
                }
            }
        }

        cache = new Cache(width, height, size, indices, bounds, numX, numY);
        cacheRef = new SoftReference<>(cache);
        return cache;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int width = src.getWidth();
        int height = src.getHeight();

        Cache cache = getOrCreateCache(width, height);
        int[] indices = cache.indices();
        int numX = cache.numX();
        int numY = cache.numY();

        int[] triangleAverages = new int[numX * numY];
        boolean[] processed = new boolean[numX * numY];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int triangleId = indices[y * width + x];

                if (!processed[triangleId]) {
                    triangleAverages[triangleId] = calculateTriangleAverageColor(src, triangleId, cache);
                    processed[triangleId] = true;
                }

                int alpha = src.getRGB(x, y) & 0xFF_00_00_00;
                dst.setRGB(x, y, alpha | triangleAverages[triangleId]);
            }
        }

        return dst;
    }

    private static int calculateTriangleAverageColor(BufferedImage src, int triangleId, Cache cache) {
        Rectangle bounds = cache.bounds()[triangleId];
        int[] indices = cache.indices();
        int width = cache.width();

        long totalR = 0, totalG = 0, totalB = 0;
        int count = 0;

        for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                if (indices[y * width + x] == triangleId) {
                    int rgb = src.getRGB(x, y);
                    totalR += (rgb >> 16) & 0xFF;
                    totalG += (rgb >> 8) & 0xFF;
                    totalB += rgb & 0xFF;
                    count++;
                }
            }
        }

        if (count == 0) {
            return 0;
        }

        int avgR = (int) (totalR / count);
        int avgG = (int) (totalG / count);
        int avgB = (int) (totalB / count);

        return (avgR << 16) | (avgG << 8) | avgB;
    }

    private static Point getTriangleIndex(int x, int y, int size) {
        double h = size * HALF_SQRT_3;
        int row = (int) Math.floor(y / h);
        double relativeY = y - row * h;
        int col;

        if (row % 2 == 0) {
            if (relativeY < h - SQRT_3 * Math.abs(x % size - size / 2.0)) {
                col = (int) Math.floor(x / (double) size);
                return new Point(2 * col, row); // upward triangle
            } else {
                col = (int) Math.floor((x - size / 2.0) / (double) size);
                return new Point(2 * col + 1, row); // downward triangle
            }
        } else {
            if (relativeY < h - SQRT_3 * Math.abs((x + size / 2.0) % size - size / 2.0)) {
                col = (int) Math.floor((x - size / 2.0) / (double) size);
                return new Point(2 * col + 1, row); // upward triangle
            } else {
                col = (int) Math.floor(x / (double) size);
                return new Point(2 * col, row); // downward triangle
            }
        }
    }
}
