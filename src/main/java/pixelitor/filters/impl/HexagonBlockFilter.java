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

import static com.jhlabs.image.ImageMath.SQRT_3;

/**
 * A filter that pixelates an image by dividing it into a grid of regular hexagons.
 * The filter processes the image by mapping each pixel to its containing hexagon,
 * calculating the average color of all pixels within each hexagon, and then setting
 * all pixels in that hexagon to the calculated average color. The hexagons form
 * a tessellating pattern covering the entire image.
 */
public class HexagonBlockFilter extends AbstractBufferedImageOp {
    private final int size; // length of hexagon side

    private record Cache(int width, int height, int size, int[] indices, Rectangle[] bounds, int numX, int numY) {
    }

    private static volatile SoftReference<Cache> cacheRef = new SoftReference<>(null);

    public HexagonBlockFilter(String filterName, int size) {
        super(filterName);

        this.size = size;
    }

    private Cache getOrCreateCache(int width, int height) {
        Cache cache = cacheRef.get();
        if (cache != null && cache.width() == width && cache.height() == height && cache.size() == size) {
            return cache;
        }

        // first pass to determine the range of indices
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point idx = getHexagonIndex(x, y);
                minCol = Math.min(minCol, idx.x);
                maxCol = Math.max(maxCol, idx.x);
                minRow = Math.min(minRow, idx.y);
                maxRow = Math.max(maxRow, idx.y);
            }
        }

        int minColumnIndex = minCol;
        int minRowIndex = minRow;
        int numX = maxCol - minCol + 1;
        int numY = maxRow - minRow + 1;

        // initialize flattened 1D array to reduce object allocations
        int[] indices = new int[width * height];
        Rectangle[] bounds = new Rectangle[numX * numY];

        // second pass to fill the caches
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point idx = getHexagonIndex(x, y);
                int adjustedCol = idx.x - minColumnIndex;
                int adjustedRow = idx.y - minRowIndex;
                int hexId = adjustedCol + adjustedRow * numX;

                indices[y * width + x] = hexId;

                if (bounds[hexId] == null) {
                    bounds[hexId] = new Rectangle(x, y, 1, 1);
                } else {
                    bounds[hexId].add(x, y);
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

        int[] hexagonAverages = new int[numX * numY];
        boolean[] processed = new boolean[numX * numY];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int hexId = indices[y * width + x];

                if (!processed[hexId]) {
                    hexagonAverages[hexId] = calcHexagonAverageColor(src, hexId, cache);
                    processed[hexId] = true;
                }

                int alpha = src.getRGB(x, y) & 0xFF_00_00_00;
                dst.setRGB(x, y, alpha | hexagonAverages[hexId]);
            }
        }

        return dst;
    }

    private static int calcHexagonAverageColor(BufferedImage src, int hexId, Cache cache) {
        Rectangle bounds = cache.bounds()[hexId];
        int[] indices = cache.indices();
        int width = cache.width();

        long totalR = 0, totalG = 0, totalB = 0;
        int count = 0;

        for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                if (indices[y * width + x] == hexId) {
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

    private Point getHexagonIndex(int x, int y) {
        // convert pixel coordinates to axial coordinates
        double q = (2.0 / 3 * x) / size;
        double r = (-1.0 / 3 * x + SQRT_3 / 3 * y) / size;

        // round to the nearest hexagon center
        int q_grid = (int) Math.round(q);
        int r_grid = (int) Math.round(r);
        int s_grid = (int) Math.round(-q - r);

        // fix any rounding errors
        double q_diff = Math.abs(q_grid - q);
        double r_diff = Math.abs(r_grid - r);
        double s_diff = Math.abs(s_grid - (-q - r));

        if (q_diff > r_diff && q_diff > s_diff) {
            q_grid = -r_grid - s_grid;
        } else if (r_diff > s_diff) {
            r_grid = -q_grid - s_grid;
        }

        // convert to offset coordinates
        int col = q_grid;
        int row = r_grid + (q_grid + (q_grid & 1)) / 2;

        return new Point(col, row);
    }
}
