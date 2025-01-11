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

import static com.jhlabs.image.ImageMath.SQRT_3;

/**
 * A filter that pixelates an image by dividing it into a grid of regular hexagons.
 * The filter processes the image by mapping each pixel to its containing hexagon,
 * calculating the average color of all pixels within each hexagon, and then setting
 * all pixels in that hexagon to the calculated average color. The hexagons form
 * a tessellating pattern covering the entire image.
 */
public class HexagonBlockFilter extends AbstractBufferedImageOp {
    private int size = 20; // length of hexagon side
    private int[][] hexagonIndices; // cache for pixel-to-hexagon mapping
    private Rectangle[] hexagonBounds; // cache for hexagon boundaries
    private int numHexagonsX;
    private int numHexagonsY;

    public HexagonBlockFilter(String filterName) {
        super(filterName);
    }

    public void setSize(int size) {
        if (this.size != size) {
            hexagonIndices = null;
            hexagonBounds = null;
        }
        this.size = size;
    }

    // Builds the two cache arrays by analyzing the image dimensions.
    private void initializeCaches(int width, int height) {
        // first pass to determine the range of indices
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;

        // find the range of indices
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
        numHexagonsX = maxCol - minCol + 1;
        numHexagonsY = maxRow - minRow + 1;

        // initialize arrays
        hexagonIndices = new int[height][width];
        hexagonBounds = new Rectangle[numHexagonsX * numHexagonsY];

        // second pass to fill the caches
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point idx = getHexagonIndex(x, y);
                int adjustedCol = idx.x - minColumnIndex;
                int adjustedRow = idx.y - minRowIndex;
                int hexId = adjustedCol + adjustedRow * numHexagonsX;

                hexagonIndices[y][x] = hexId;

                if (hexagonBounds[hexId] == null) {
                    hexagonBounds[hexId] = new Rectangle(x, y, 1, 1);
                } else {
                    Rectangle bounds = hexagonBounds[hexId];
                    bounds.add(x, y);
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

        if (hexagonIndices == null || hexagonBounds == null) {
            initializeCaches(width, height);
        }

        int[] hexagonAverages = new int[numHexagonsX * numHexagonsY];

        // ensure that each hexagon’s average color is calculated only once
        boolean[] processed = new boolean[numHexagonsX * numHexagonsY];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int hexId = hexagonIndices[y][x];

                if (!processed[hexId]) {
                    hexagonAverages[hexId] = calcHexagonAverageColor(src, hexId);
                    processed[hexId] = true;
                }

                int alpha = src.getRGB(x, y) & 0xff000000;
                dst.setRGB(x, y, alpha | hexagonAverages[hexId]);
            }
        }

        return dst;
    }

    // Calculates the average RGB color of all pixels in a given hexagon.
    private int calcHexagonAverageColor(BufferedImage src, int hexId) {
        Rectangle bounds = hexagonBounds[hexId];
        long totalR = 0, totalG = 0, totalB = 0;
        int count = 0;

        for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                if (hexagonIndices[y][x] == hexId) {
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

    // Converts pixel coordinates (x, y) into axial hexagon grid coordinates.
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