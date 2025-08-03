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

public class WeaveFilter extends PointFilter {
    private static final double AA_THRESHOLD = 0.001;
    private static final float AA_TRANSITION_WIDTH = 1.0f;

    private double centerX, centerY;
    private double angle, sin, cos;

    private float xWidth = 16;
    private float yWidth = 16;
    private float xGap = 6;
    private float yGap = 6;
    private int rows = 4;
    private int cols = 4;
    private static final int X_THREAD_COLOR = 0xffff8080;
    private static final int Y_THREAD_COLOR = 0xff8080ff;
    private boolean useImageColors = true;
    private boolean roundThreads = false;
    private boolean shadeCrossings = true;
    private boolean antiAliasing = true;

    public static final int PLAIN_PATTERN = 0;
    private final int[][] PLAIN = {
        {0, 1, 0, 1},
        {1, 0, 1, 0},
        {0, 1, 0, 1},
        {1, 0, 1, 0},
    };

    public static final int BASKET_PATTERN = 1;
    public static final int[][] BASKET = {
        {1, 1, 0, 0},
        {1, 1, 0, 0},
        {0, 0, 1, 1},
        {0, 0, 1, 1},
    };

    public static final int TWILL_PATTERN = 2;
    public static final int[][] TWILL = {
        {0, 0, 0, 1},
        {0, 0, 1, 0},
        {0, 1, 0, 0},
        {1, 0, 0, 0},
    };

    public static final int CROWFOOT_PATTERN = 3;
    public static final int[][] CROWFOOT = {
        {0, 1, 1, 1},
        {1, 0, 1, 1},
        {1, 1, 0, 1},
        {1, 1, 1, 0},
    };

    private final int[][][] ALL_PATTERNS = {PLAIN, BASKET, TWILL, CROWFOOT};

    private int[][] matrix = PLAIN;

    public WeaveFilter(String filterName) {
        super(filterName);
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        // apply rotation to the coordinates
        double weaveX = x;
        double weaveY = y;
        if (angle != 0) {
            double dx = x - centerX;
            double dy = y - centerY;
            weaveX = dx * cos - dy * sin + centerX;
            weaveY = dx * sin + dy * cos + centerY;
        }

        // it looks better if the effect is shifted a bit
        weaveX += (xWidth + xGap / 2);
        weaveY += (yWidth + yGap / 2);

        // the cell of the weave grid the current pixel falls into
        double totalXWidth = xWidth + xGap;
        double totalYWidth = yWidth + yGap;
        int gridX = (int) Math.floor(weaveX / totalXWidth);
        int gridY = (int) Math.floor(weaveY / totalYWidth);

        // the position of the current pixel within the cell
        double xInCell = ImageMath.mod(weaveX, totalXWidth);
        double yInCell = ImageMath.mod(weaveY, totalYWidth);

        // determine which thread is on top from the weave pattern
        int matrixRow = ImageMath.mod(gridY, rows);
        int matrixCol = ImageMath.mod(gridX, cols);
        int patternValue = matrix[matrixRow][matrixCol];
        boolean isXThreadOnTop = (patternValue == 1);

        // calculate roundness factor for threads
        double dX = 0, dY = 0;
        if (roundThreads) {
            // normalized distance from the center of a thread
            dX = Math.abs(xWidth / 2 - xInCell) / xWidth / 2;
            dY = Math.abs(yWidth / 2 - yInCell) / yWidth / 2;
        }

        // calculate shading factor for gaps
        double cX = 0, cY = 0;
        if (shadeCrossings) {
            cX = ImageMath.smoothStep(xWidth / 2, xWidth / 2 + xGap, Math.abs(xWidth / 2 - xInCell));
            cY = ImageMath.smoothStep(yWidth / 2, yWidth / 2 + yGap, Math.abs(yWidth / 2 - yInCell));
        }

        // get the base color for the threads
        int baseColorX, baseColorY;
        if (useImageColors) {
            baseColorX = baseColorY = rgb;
        } else {
            baseColorX = X_THREAD_COLOR;
            baseColorY = Y_THREAD_COLOR;
        }

        // determine if we are at a horizontal or vertical crossing
        boolean isVerticalCrossing = patternValue != matrix[(matrixRow + 1) % rows][matrixCol];
        boolean isHorizontalCrossing = patternValue != matrix[matrixRow][(matrixCol + 1) % cols];

        // calculate the final shaded color for the horizontal thread (X)
        int finalColorX = baseColorX;
        if (shadeCrossings) {
            if (isVerticalCrossing) {
                double shade = cY;
                if (!isXThreadOnTop) { // x thread is underneath
                    shade = 1.0 - shade;
                }
                shade *= 0.5;
                finalColorX = ImageMath.mixColors(shade, finalColorX, 0xff000000);
            } else if (!isXThreadOnTop) { // not a crossing, but x thread is underneath
                finalColorX = ImageMath.mixColors(0.5, finalColorX, 0xff000000);
            }
        }
        if (roundThreads) {
            finalColorX = ImageMath.mixColors(2 * dX, finalColorX, 0xff000000);
        }

        // calculate the final shaded color for the vertical thread (Y)
        int finalColorY = baseColorY;
        if (shadeCrossings) {
            if (isHorizontalCrossing) {
                double shade = cX;
                if (isXThreadOnTop) { // y thread is underneath
                    shade = 1.0 - shade;
                }
                shade *= 0.5;
                finalColorY = ImageMath.mixColors(shade, finalColorY, 0xff000000);
            } else if (isXThreadOnTop) { // not a crossing, but y thread is underneath
                finalColorY = ImageMath.mixColors(0.5, finalColorY, 0xff000000);
            }
        }
        if (roundThreads) {
            finalColorY = ImageMath.mixColors(2 * dY, finalColorY, 0xff000000);
        }

        // calculate thread coverage using a smooth pulse for anti-aliasing
        float fx, fy;
        if (antiAliasing) {
            fx = ImageMath.smoothPulse(0, AA_TRANSITION_WIDTH, xWidth - AA_TRANSITION_WIDTH, xWidth, (float) xInCell);
            fy = ImageMath.smoothPulse(0, AA_TRANSITION_WIDTH, yWidth - AA_TRANSITION_WIDTH, yWidth, (float) yInCell);
        } else {
            fx = (xInCell < xWidth) ? 1.0f : 0.0f;
            fy = (yInCell < yWidth) ? 1.0f : 0.0f;
        }

        // layer the threads according to the weave pattern
        int topColor, bottomColor;
        float topCoverage, bottomCoverage;
        if (isXThreadOnTop) {
            topColor = finalColorX;
            bottomColor = finalColorY;
            topCoverage = fx;
            bottomCoverage = fy;
        } else {
            topColor = finalColorY;
            bottomColor = finalColorX;
            topCoverage = fy;
            bottomCoverage = fx;
        }

        // blend the bottom thread with the transparent background, then blend the top thread over it
        int pixelColor = ImageMath.mixColors(bottomCoverage, 0x00000000, bottomColor);
        pixelColor = ImageMath.mixColors(topCoverage, pixelColor, topColor);

        return pixelColor;
    }

    @Override
    public void setDimensions(int width, int height) {
        centerX = width / 2.0;
        centerY = height / 2.0;
    }

    /**
     * Sets the angle of the weave pattern.
     */
    public void setAngle(double angle) {
        this.angle = angle;
        this.sin = FastMath.sin(angle);
        this.cos = FastMath.cos(angle);

        // disable anti-aliasing for axis-aligned weaves to improve performance and sharpness
        this.antiAliasing = Math.abs(sin) > AA_THRESHOLD && Math.abs(cos) > AA_THRESHOLD;
    }

    /**
     * Sets the gap between horizontal threads.
     */
    public void setXGap(float xGap) {
        this.xGap = xGap;
    }

    /**
     * Sets the width of horizontal threads.
     */
    public void setXWidth(float xWidth) {
        this.xWidth = xWidth;
    }

    /**
     * Sets the width of vertical threads.
     */
    public void setYWidth(float yWidth) {
        this.yWidth = yWidth;
    }

    /**
     * Sets the gap between vertical threads.
     */
    public void setYGap(float yGap) {
        this.yGap = yGap;
    }

    /**
     * Sets the weave pattern from a set of predefined patterns.
     */
    public void setPattern(int pattern) {
        setCrossings(ALL_PATTERNS[pattern]);
    }

    private void setCrossings(int[][] matrix) {
        this.matrix = matrix;
        cols = matrix.length;
        rows = matrix.length;
    }

    /**
     * Sets whether to use the source image colors for the threads.
     */
    public void setUseImageColors(boolean useImageColors) {
        this.useImageColors = useImageColors;
    }

    /**
     * Sets whether to give threads a rounded appearance.
     */
    public void setRoundThreads(boolean roundThreads) {
        this.roundThreads = roundThreads;
    }

    /**
     * Sets whether to add shading to thread crossings.
     */
    public void setShadeCrossings(boolean shadeCrossings) {
        this.shadeCrossings = shadeCrossings;
    }

    @Override
    public String toString() {
        return "Texture/Weave...";
    }
}
