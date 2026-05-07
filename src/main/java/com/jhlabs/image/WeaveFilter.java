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

import net.jafama.FastMath;

import java.awt.geom.Point2D;

/**
 * A filter that renders a customizable woven fabric texture, procedurally
 * simulating interwoven horizontal and vertical threads over the source image.
 */
public class WeaveFilter extends PointFilter {
    private static final double AA_THRESHOLD = 0.001;
    private static final float AA_TRANSITION_WIDTH = 1.0f;

    private final double cx, cy;

    private final double angle;
    private final double sin;
    private final double cos;

    private final float xSize;
    private final float ySize;
    private final float halfXSize;
    private final float halfYSize;

    private final float xGap;
    private final float yGap;
    private final int rows;
    private final int cols;
    private final boolean useImageColors;
    private final boolean roundThreads;
    private final boolean shadeCrossings;
    private final boolean antiAliasing;

    public static final int H_THREAD_COLOR = 0xFF_80_80_FF;
    public static final int V_THREAD_COLOR = 0xFF_FF_80_80;

    // variables precalculated in the constructor
    private final double totalXSize, totalYSize;
    private final byte[] cellProperties;
    private static final byte VER_ON_TOP_MASK = 1;
    private static final byte VER_CROSSING_MASK = 2;
    private static final byte HOR_CROSSING_MASK = 4;

    /**
     * Constructs a new WeaveFilter.
     *
     * @param filterName     The name of the filter, passed to the superclass.
     * @param matrix         The weave pattern from a 2D integer matrix. The matrix defines the over-under
     *                       sequence of threads. A '1' indicates the horizontal (X) thread is on top, and
     *                       a '0' indicates the vertical (Y) thread is on top.
     * @param xSize          The thickness of vertical threads (x size of the effect).
     * @param ySize          The thickness of horizontal threads (y size of the effect).
     * @param xGap           The gap between vertical threads (measured along the x axis).
     * @param yGap           The gap between horizontal threads (measured along the y axis).
     * @param angle          The angle of the weave pattern in radians.
     * @param center         The center of the image in pixels.
     * @param useImageColors Whether to use the source image colors for the threads.
     * @param roundThreads   Whether to give threads a rounded appearance.
     * @param shadeCrossings Whether to add shading to thread crossings.
     */
    public WeaveFilter(String filterName, int[][] matrix,
                       float xSize, float ySize,
                       float xGap, float yGap,
                       double angle, Point2D center,
                       boolean useImageColors, boolean roundThreads, boolean shadeCrossings) {
        super(filterName);

        this.rows = matrix.length;
        this.cols = matrix[0].length;

        this.xSize = xSize;
        this.ySize = ySize;
        this.halfXSize = xSize / 2;
        this.halfYSize = ySize / 2;

        this.xGap = xGap;
        this.yGap = yGap;

        this.angle = angle;
        this.sin = FastMath.sin(angle);
        this.cos = FastMath.cos(angle);

        this.cx = center.getX();
        this.cy = center.getY();

        this.useImageColors = useImageColors;
        this.roundThreads = roundThreads;
        this.shadeCrossings = shadeCrossings;

        // disable anti-aliasing for axis-aligned weaves to improve performance and sharpness
        this.antiAliasing = Math.abs(sin) > AA_THRESHOLD && Math.abs(cos) > AA_THRESHOLD;

        this.totalXSize = xSize + xGap;
        this.totalYSize = ySize + yGap;

        this.cellProperties = new byte[rows * cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                byte prop = 0;
                if (matrix[r][c] == 0) {
                    prop |= VER_ON_TOP_MASK;
                }
                if (matrix[r][c] != matrix[(r + 1) % rows][c]) {
                    prop |= VER_CROSSING_MASK;
                }
                if (matrix[r][c] != matrix[r][(c + 1) % cols]) {
                    prop |= HOR_CROSSING_MASK;
                }
                cellProperties[r * cols + c] = prop;
            }
        }
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        // apply rotation to the coordinates
        double weaveX = x;
        double weaveY = y;
        if (angle != 0) {
            double dx = x - cx;
            double dy = y - cy;
            weaveX = dx * cos - dy * sin + cx;
            weaveY = dx * sin + dy * cos + cy;
        }

        // ensure that the top-left cell in the UI grid selector
        // corresponds to the top-left corner of the image
        weaveX -= xGap / 2;
        weaveY -= yGap / 2;

        // the cell of the weave grid the current pixel falls into
        int gridX = (int) Math.floor(weaveX / totalXSize);
        int gridY = (int) Math.floor(weaveY / totalYSize);

        // the position of the current pixel within the cell
        double xInCell = ImageMath.mod(weaveX, totalXSize);
        double yInCell = ImageMath.mod(weaveY, totalYSize);

        // calculate thread coverage using a smooth pulse for anti-aliasing
        float fx, fy;
        if (antiAliasing) {
            fx = ImageMath.smoothPulse(0, AA_TRANSITION_WIDTH, xSize - AA_TRANSITION_WIDTH, xSize, (float) xInCell);
            fy = ImageMath.smoothPulse(0, AA_TRANSITION_WIDTH, ySize - AA_TRANSITION_WIDTH, ySize, (float) yInCell);
        } else {
            fx = (xInCell < xSize) ? 1.0f : 0.0f;
            fy = (yInCell < ySize) ? 1.0f : 0.0f;
        }

        if (fx == 0.0f && fy == 0.0f) {
            return 0x00_00_00_00; // early return for transparent gaps
        }

        // determine which thread is on top from the weave pattern
        int matrixRow = Math.floorMod(gridY, rows);
        int matrixCol = Math.floorMod(gridX, cols);

        byte prop = cellProperties[matrixRow * cols + matrixCol];
        boolean isVerThreadOnTop = (prop & VER_ON_TOP_MASK) != 0;
        boolean isHorCrossing = (prop & HOR_CROSSING_MASK) != 0;
        boolean isVerCrossing = (prop & VER_CROSSING_MASK) != 0;

        // calculate roundness factor for threads
        double roundnessX = 0, roundnessY = 0;
        if (roundThreads) {
            // the shadow is maximal at the thread edges and minimal inside
            roundnessX = Math.abs(xInCell - halfXSize) / xSize;
            roundnessY = Math.abs(yInCell - halfYSize) / ySize;
        }

        // get the base color for the threads
        int baseColorX, baseColorY;
        if (useImageColors) {
            baseColorX = baseColorY = rgb;
        } else {
            baseColorX = V_THREAD_COLOR;
            baseColorY = H_THREAD_COLOR;
        }

        // layer the threads according to the weave pattern
        float topCoverage, bottomCoverage;
        if (isVerThreadOnTop) {
            topCoverage = fx;
            bottomCoverage = fy;
        } else {
            topCoverage = fy;
            bottomCoverage = fx;
        }

        boolean bottomVisible = bottomCoverage > 0.0f && topCoverage < 1.0f;
        boolean topVisible = topCoverage > 0.0f;

        int outColor = 0x00_00_00_00;

        if (bottomVisible) {
            int bottomColor = isVerThreadOnTop
                ? calcThreadColor(baseColorY, isHorCrossing, xInCell, halfXSize, xGap, false, roundnessY)
                : calcThreadColor(baseColorX, isVerCrossing, yInCell, halfYSize, yGap, false, roundnessX);
            outColor = ImageMath.mixColors(bottomCoverage, 0x00_00_00_00, bottomColor);
        }

        if (topVisible) {
            int topColor = isVerThreadOnTop
                ? calcThreadColor(baseColorX, isVerCrossing, yInCell, halfYSize, yGap, true, roundnessX)
                : calcThreadColor(baseColorY, isHorCrossing, xInCell, halfXSize, xGap, true, roundnessY);
            outColor = ImageMath.mixColors(topCoverage, outColor, topColor);
        }

        return outColor;
    }

    private int calcThreadColor(int baseColor, boolean isCrossing, double posInCell, double halfSize, double gap, boolean isTopThread, double roundness) {
        int threadColor = baseColor;
        if (shadeCrossings) {
            if (isCrossing) {
                double shade = ImageMath.smoothStep(halfSize, halfSize + gap, Math.abs(halfSize - posInCell));
                if (!isTopThread) {
                    shade = 1.0 - shade;
                }
                shade *= 0.5;
                threadColor = ImageMath.mixColors(shade, threadColor, 0xFF_00_00_00);
            } else if (!isTopThread) {
                threadColor = ImageMath.mixColors(0.5, threadColor, 0xFF_00_00_00);
            }
        }
        if (roundThreads) {
            threadColor = ImageMath.mixColors(roundness, threadColor, 0xFF_00_00_00);
        }
        return threadColor;
    }
}
