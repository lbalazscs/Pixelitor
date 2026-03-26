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

import java.util.Arrays;

/**
 * A filter that renders a checkerboard pattern with optional
 * rotation, distortion, fuzziness, and multiple colors.
 */
public class CheckFilter extends PointFilter {
    // offset used to safely translate coordinates to
    // positive integers for modulo arithmetic
    private static final int OFFSET = 100_000;

    private final int[] colors;

    private final int fuzziness;
    private final float fuzzThreshold;

    private final float m00;
    private final float m01;
    private final float m10;
    private final float m11;

    private final float aaThresholdX;
    private final float aaThresholdY;
    private final float upperAaThresholdX;
    private final float upperAaThresholdY;

    private final boolean couldNeedAA;
    private final int aaRes;
    private final float[] subPixelOffsets;
    private final float invAaRes2;

    private final double distortion;
    private final double phase;

    /**
     * Constructs a CheckFilter.
     *
     * @param filterName the name of the filter.
     * @param colors     the array of colors. Must contain at least two colors.
     * @param xScale     the X scale of the texture.
     * @param yScale     the Y scale of the texture.
     * @param fuzziness  the fuzziness of the texture.
     * @param angle      the angle of the texture.
     * @param distortion the distortion amount.
     * @param phase      the phase offset used for the wave distortion.
     */
    public CheckFilter(String filterName, int[] colors, int xScale, int yScale, int fuzziness, float angle, double distortion, double phase) {
        super(filterName);

        if (colors == null || colors.length < 2) {
            throw new IllegalArgumentException("colors = " + Arrays.toString(colors));
        }
        this.colors = colors;

        this.aaThresholdX = 1.0f / xScale;
        this.upperAaThresholdX = 1.0f - aaThresholdX;

        this.aaThresholdY = 1.0f / yScale;
        this.upperAaThresholdY = 1.0f - aaThresholdY;

        this.fuzziness = fuzziness;
        this.fuzzThreshold = fuzziness / 100.0f;

        this.distortion = distortion;
        this.phase = phase;

        double cos = FastMath.cos(angle);
        double sin = FastMath.sin(angle);

        // scale the transformation matrix
        this.m00 = (float) (cos / xScale);
        this.m01 = (float) (sin / xScale);
        this.m10 = (float) (-sin / yScale);
        this.m11 = (float) (cos / yScale);

        // no AA is necessary if the angle is 0, 90, 180 or 270 degrees
        boolean straight = ((Math.abs(sin) < 0.0000001) || (Math.abs(cos) < 0.0000001));
        this.couldNeedAA = (!straight || distortion > 0) && fuzziness == 0;
        if (couldNeedAA) {
            this.aaRes = calcAaRes(distortion, sin, cos, straight);
            this.invAaRes2 = 1.0f / (aaRes * aaRes);

            // precomputed sub-pixel sample positions as offsets around the pixel
            this.subPixelOffsets = new float[aaRes];
            float invAaRes = 1.0f / aaRes;
            float aaSampleOffset = 0.5f - 1.0f / (2 * aaRes);
            for (int i = 0; i < aaRes; i++) {
                this.subPixelOffsets[i] = invAaRes * i - aaSampleOffset;
            }
        } else {
            // final variables still have to be initialized
            this.aaRes = 0;
            this.invAaRes2 = 0.0f;
            this.subPixelOffsets = null;
        }
    }

    // determines the anti-aliasing resolution
    private static int calcAaRes(double distortion, double sin, double cos, boolean straight) {
        if (straight && distortion <= 0) {
            return 2; // default
        }

        // the necessary AA resolution scales with how difficult the angle
        // is: near-diagonal angles need less than almost aligned angles
        double minSinCos = Math.min(Math.abs(sin), Math.abs(cos));
        if (minSinCos > 0.5 || distortion > 0.5) {
            return 3;
        } else if (minSinCos > 0.15 || distortion > 0.2) {
            return 4;
        } else if (minSinCos > 0.1 || distortion > 0.1) {
            return 5;
        } else if (minSinCos > 0.07 || distortion > 0.05) {
            return 6;
        } else if (minSinCos > 0.03 || distortion > 0.02) {
            return 7;
        }
        return 8;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        // transform into "checker space" where each unit square is one tile
        float nx = m00 * x + m01 * y;
        float ny = m10 * x + m11 * y;

        if (distortion > 0) {
            nx += (float) (distortion * FastMath.sinQuick(ny + phase));
            ny += (float) (distortion * FastMath.sinQuick(nx + phase));
        }

        // guaranteed to be positive
        float pnx = nx + OFFSET;
        float pny = ny + OFFSET;

        // integer parts: which tile the current pixel falls in
        int inx = (int) pnx;
        int iny = (int) pny;

        // fractional part
        float dxFrac = pnx - inx;
        float dyFrac = pny - iny;

        int actualTileX = inx - OFFSET;
        int actualTileY = iny - OFFSET;
        int tileSum = actualTileX + actualTileY; // to cycle colors along diagonals
        int numColors = colors.length;

        if (fuzziness != 0) {
            // The fuzziness implementation is based on
            // the checker of http://doup.github.io/sapo.js/

            // fold coordinates to get distance from nearest edge
            // (0.0 = tile edge and 0.5 = tile center)
            float dxFolded = dxFrac < 0.5f ? dxFrac : 1.0f - dxFrac;
            float dyFolded = dyFrac < 0.5f ? dyFrac : 1.0f - dyFrac;
            float minFoldedDist = Math.min(dxFolded, dyFolded);

            if (minFoldedDist < fuzzThreshold) { // pixel is in the fuzzy region
                // the color of the tile the pixel is in
                int currentColor = colors[Math.floorMod(tileSum, numColors)];

                // determine the color of the specific adjacent tile towards which fuzzing occurs
                int neighborSum;
                if (dxFolded <= dyFolded) { // closer to a vertical edge
                    if (dxFrac < 0.5f) { // pixel is in the left half
                        neighborSum = tileSum - 1; // neighbor is to the left
                    } else { // pixel is in the right half
                        neighborSum = tileSum + 1; // neighbor is to the right
                    }
                } else { // closer to a horizontal edge
                    if (dyFrac < 0.5f) { // pixel is in the top half
                        neighborSum = tileSum - 1; // neighbor is above
                    } else { // pixel is in the bottom half
                        neighborSum = tileSum + 1; // neighbor is below
                    }
                }
                int neighborColor = colors[Math.floorMod(neighborSum, numColors)];
                float mixingProportion = 0.5f + 0.5f * (minFoldedDist / fuzzThreshold);
                return ImageMath.mixColors(mixingProportion, neighborColor, currentColor);
            }
        }

        // this part is reached if:
        // 1. fuzziness == 0
        // 2. fuzziness != 0 but pixel is outside the fuzz band

        boolean needsAA = false;
        // the fuzziness condition is imperfect: very small images
        // benefit from AA even with low fuzziness, while in large
        // images a small fuzziness may cause AA artifacts
        if (couldNeedAA) {
            needsAA = dxFrac < aaThresholdX || dyFrac < aaThresholdY || dxFrac > upperAaThresholdX || dyFrac > upperAaThresholdY;
        }

        float f; // blend factor
        if (needsAA) {
            float p = 0; // accumulator for sub-pixel samples
            for (int i = 0; i < aaRes; i++) {
                float yy = y + subPixelOffsets[i];
                for (int j = 0; j < aaRes; j++) {
                    float xx = x + subPixelOffsets[j];
                    p += calcSubPixelInterpolation(xx, yy);
                }
            }
            f = p * invAaRes2; // normalize into the range [0, 1]
        } else {
            f = ((inx & 1) == (iny & 1)) ? 0.0f : 1.0f;
        }

        int colorIndexEven; // sum for the even-sum-type diagonal in the pair
        int colorIndexOdd;  // sum for the odd-sum-type diagonal in the pair

        // determine the sums for the even/odd pair of diagonals related to the current tile
        if ((tileSum & 1) == 0) {
            colorIndexEven = tileSum;
            colorIndexOdd = tileSum + 1;
        } else {
            colorIndexEven = tileSum - 1;
            colorIndexOdd = tileSum;
        }

        // the color indices for the colors array
        int index0 = Math.floorMod(colorIndexEven, numColors);
        int index1 = Math.floorMod(colorIndexOdd, numColors);

        int color0 = colors[index0]; // color for "even sum type" diagonal
        int color1 = colors[index1]; // color for "odd sum type" diagonal

        if (f == 0.0f) {
            return color0;
        }
        if (f == 1.0f) {
            return color1;
        }
        return ImageMath.mixColors(f, color0, color1);
    }

    /**
     * Returns either 0.0f or 1.0f for each sample point, representing
     * which of the two checker "color types" a sub-pixel position falls into.
     */
    private float calcSubPixelInterpolation(float x, float y) {
        float nx = m00 * x + m01 * y;
        float ny = m10 * x + m11 * y;
        
        if (distortion > 0) {
            nx += (float) (distortion * FastMath.sinQuick(ny + phase));
            ny += (float) (distortion * FastMath.sinQuick(nx + phase));
        }

        // guaranteed to be positive
        float pnx = nx + OFFSET;
        float pny = ny + OFFSET;

        // integer parts
        int inx = (int) pnx;
        int iny = (int) pny;

        return ((inx & 1) == (iny & 1)) ? 0.0f : 1.0f;
    }
}
