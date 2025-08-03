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
    private float centerX, centerY;
    private double angle, sin, cos;

    private float xWidth = 16;
    private float yWidth = 16;
    private float xGap = 6;
    private float yGap = 6;
    private int rows = 4;
    private int cols = 4;
    private static final int rgbX = 0xffff8080;
    private static final int rgbY = 0xff8080ff;
    private boolean useImageColors = true;
    private boolean roundThreads = false;
    private boolean shadeCrossings = true;

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

    public void setAngle(double angle) {
        this.angle = angle;
        this.sin = FastMath.sin(angle);
        this.cos = FastMath.cos(angle);
    }

    public void setXGap(float xGap) {
        this.xGap = xGap;
    }

    public void setXWidth(float xWidth) {
        this.xWidth = xWidth;
    }

    public void setYWidth(float yWidth) {
        this.yWidth = yWidth;
    }

    public void setYGap(float yGap) {
        this.yGap = yGap;
    }

    public void setPattern(int pattern) {
        setCrossings(ALL_PATTERNS[pattern]);
    }

    private void setCrossings(int[][] matrix) {
        this.matrix = matrix;
        cols = matrix.length;
        rows = matrix.length;
    }

    public void setUseImageColors(boolean useImageColors) {
        this.useImageColors = useImageColors;
    }

    public void setRoundThreads(boolean roundThreads) {
        this.roundThreads = roundThreads;
    }

    public void setShadeCrossings(boolean shadeCrossings) {
        this.shadeCrossings = shadeCrossings;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
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
        int gridX = (int) (weaveX / (xWidth + xGap));
        int gridY = (int) (weaveY / (yWidth + yGap));

        // the position of the current pixel within the cell
        double xInCell = ImageMath.mod(weaveX, xWidth + xGap);
        double yInCell = ImageMath.mod(weaveY, yWidth + yGap);

        // whether the current pixel falls within a vertical/horizontal thread
        boolean inX = xInCell < xWidth;
        boolean inY = yInCell < yWidth;

        // vertical and horizontal roundness factor
        double dX, dY;
        if (roundThreads) {
            // set to the the normalized distance from the center of a thread
            dX = Math.abs(xWidth / 2 - xInCell) / xWidth / 2;
            dY = Math.abs(yWidth / 2 - yInCell) / yWidth / 2;
        } else {
            dX = dY = 0;
        }

        // shading factors for smooth shadows in the gaps where one thread passes under another
        double cX, cY;
        if (shadeCrossings) {
            cX = ImageMath.smoothStep(xWidth / 2, xWidth / 2 + xGap, Math.abs(xWidth / 2 - xInCell));
            cY = ImageMath.smoothStep(yWidth / 2, yWidth / 2 + yGap, Math.abs(yWidth / 2 - yInCell));
        } else {
            cX = cY = 0;
        }

        // the final color for the vertical/horizontal threads
        int lrgbX, lrgbY;
        if (useImageColors) {
            lrgbX = lrgbY = rgb;
        } else {
            lrgbX = rgbX;
            lrgbY = rgbY;
        }

        int v; // the final calculated ARGB integer value for the current pixel that will be returned

        int matrixY = ImageMath.mod(gridX, cols);
        int matrixX = ImageMath.mod(gridY, rows);
        int m = matrix[matrixX][matrixY];
        if (inX) {
            if (inY) {
                // if the pixel is within both a horizontal and vertical thread area,
                // the matrix is consulted to decide which thread's color to draw
                v = m == 1 ? lrgbX : lrgbY;
                v = ImageMath.mixColors(2 * (m == 1 ? dX : dY), v, 0xff000000);
            } else {
                if (shadeCrossings) {
                    if (m != matrix[(matrixX + 1) % rows][matrixY]) {
                        if (m == 0) {
                            cY = 1 - cY;
                        }
                        cY *= 0.5f;
                        lrgbX = ImageMath.mixColors(cY, lrgbX, 0xff000000);
                    } else if (m == 0) {
                        lrgbX = ImageMath.mixColors(0.5f, lrgbX, 0xff000000);
                    }
                }
                v = ImageMath.mixColors(2 * dX, lrgbX, 0xff000000);
            }
        } else if (inY) {
            if (shadeCrossings) {
                if (m != matrix[matrixX][(matrixY + 1) % cols]) {
                    if (m == 1) {
                        cX = 1 - cX;
                    }
                    cX *= 0.5f;
                    lrgbY = ImageMath.mixColors(cX, lrgbY, 0xff000000);
                } else if (m == 1) {
                    lrgbY = ImageMath.mixColors(0.5f, lrgbY, 0xff000000);
                }
            }
            v = ImageMath.mixColors(2 * dY, lrgbY, 0xff000000);
        } else {
            v = 0x00000000;
        }
        return v;
    }

    @Override
    public void setDimensions(int width, int height) {
        centerX = width / 2.0f;
        centerY = height / 2.0f;
    }

    @Override
    public String toString() {
        return "Texture/Weave...";
    }
}


