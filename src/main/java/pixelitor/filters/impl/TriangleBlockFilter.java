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

package pixelitor.filters.impl;

import com.jhlabs.image.AbstractBufferedImageOp;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static com.jhlabs.image.ImageMath.HALF_SQRT_3;
import static com.jhlabs.image.ImageMath.SQRT_3;

/**
 * Pixelates an image using a grid of equilateral triangles,
 * filling each triangle with its average color.
 */
public class TriangleBlockFilter extends AbstractBufferedImageOp {
    private int size = 20;
    private int[][] triangleIndices; // cache for pixel-to-triangle mapping
    private Rectangle[] triangleBounds; // cache for triangle boundaries
    private int numTrianglesX;
    private int numTrianglesY;

    public TriangleBlockFilter(String filterName) {
        super(filterName);
    }

    public void setSize(int size) {
        if (this.size != size) {
            triangleIndices = null;
            triangleBounds = null;
        }
        this.size = size;
    }

    private void initializeCaches(int width, int height) {
        double h = size * HALF_SQRT_3;
        numTrianglesY = (int) Math.ceil(height / h);

        // first pass to determine the range of column indices
        int minColIndex = Integer.MAX_VALUE;
        int maxColIndex = Integer.MIN_VALUE;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point idx = getTriangleIndex(x, y, size);
                minColIndex = Math.min(minColIndex, idx.x);
                maxColIndex = Math.max(maxColIndex, idx.x);
            }
        }

        numTrianglesX = maxColIndex - minColIndex + 1;
        triangleIndices = new int[height][width];
        triangleBounds = new Rectangle[numTrianglesX * numTrianglesY];

        // second pass to fill the caches with adjusted indices
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point idx = getTriangleIndex(x, y, size);
                // adjust the column index to be non-negative
                int adjustedCol = idx.x - minColIndex;
                int triangleId = adjustedCol + idx.y * numTrianglesX;

                triangleIndices[y][x] = triangleId;

                if (triangleBounds[triangleId] == null) {
                    triangleBounds[triangleId] = new Rectangle(x, y, 1, 1);
                } else {
                    triangleBounds[triangleId].add(x, y);
                }
            }
        }
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int width = src.getWidth();
        int height = src.getHeight();

        if (triangleIndices == null || triangleBounds == null) {
            initializeCaches(width, height);
        }

        int[] triangleAverages = new int[numTrianglesX * numTrianglesY];

        // ensure that each triangle’s average color is calculated only once
        boolean[] processed = new boolean[numTrianglesX * numTrianglesY];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int triangleId = triangleIndices[y][x];

                if (!processed[triangleId]) {
                    triangleAverages[triangleId] = calculateTriangleAverageColor(src, triangleId);
                    processed[triangleId] = true;
                }

                int alpha = src.getRGB(x, y) & 0xff000000;
                dst.setRGB(x, y, alpha | triangleAverages[triangleId]);
            }
        }

        return dst;
    }

    private int calculateTriangleAverageColor(BufferedImage src, int triangleId) {
        Rectangle bounds = triangleBounds[triangleId];
        long totalR = 0, totalG = 0, totalB = 0;
        int count = 0;

        for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                if (triangleIndices[y][x] == triangleId) {
                    int rgb = src.getRGB(x, y);
                    totalR += (rgb >> 16) & 0xff;
                    totalG += (rgb >> 8) & 0xff;
                    totalB += rgb & 0xff;
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