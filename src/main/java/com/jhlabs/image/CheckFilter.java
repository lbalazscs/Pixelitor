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
 * A filter that draws a checkerboard pattern.
 */
public class CheckFilter extends PointFilter {
    private int xScale = 8;
    private int yScale = 8;
    private int[] colors = {0xffffffff, 0xff000000};
    private int fuzziness = 0;
    private float m00 = 1.0f;
    private float m01 = 0.0f;
    private float m10 = 0.0f;
    private float m11 = 1.0f;

    private float aaThresholdX = 0.05f;
    private float aaThresholdY = 0.05f;
    private float upperAaThresholdX;
    private float upperAaThresholdY;

    private int aaRes = 2;
    private int aaRes2 = aaRes * aaRes;
    private float aaShift;
    private float invAaRes;

    private boolean straight;
    private double distortion;
    private double phase;

    private void setAaRes(int aaRes) {
        this.aaRes = aaRes;
        aaRes2 = aaRes * aaRes;
        aaShift = 0.5f - 1.0f / (2 * aaRes);
        invAaRes = 1.0f / aaRes;
    }

    public CheckFilter(String filterName) {
        super(filterName);
    }

    public void setColors(int[] colors) {
        if (colors == null || colors.length < 2) {
            throw new IllegalArgumentException("colors = " + Arrays.toString(colors));
        }
        this.colors = colors;
    }

    /**
     * Set the X scale of the texture.
     *
     * @param xScale the scale.
     */
    public void setXScale(int xScale) {
        this.xScale = xScale;
        aaThresholdX = 1.0f / xScale;
        upperAaThresholdX = 1.0f - aaThresholdX;
    }

    /**
     * Set the Y scale of the texture.
     *
     * @param yScale the scale.
     */
    public void setYScale(int yScale) {
        this.yScale = yScale;
        aaThresholdY = 1.0f / yScale;
        upperAaThresholdY = 1.0f - aaThresholdY;
    }

    /**
     * Set the fuzziness of the texture.
     *
     * @param fuzziness the fuzziness.
     */
    public void setFuzziness(int fuzziness) {
        this.fuzziness = fuzziness;
    }

    /**
     * Set the angle of the texture.
     *
     * @param angle the angle of the texture.
     * @angle
     */
    public void setAngle(float angle) {
        float cos = (float) FastMath.cos(angle);
        float sin = (float) FastMath.sin(angle);
        m00 = cos;
        m01 = sin;
        m10 = -sin;
        m11 = cos;

        // no AA is necessary if the angle is 0, 90, 180 or 270 grades
        straight = ((Math.abs(sin) < 0.0000001) || (Math.abs(cos) < 0.0000001));

        boolean hasDistortion = distortion > 0;
        if (!straight || hasDistortion) {
            // the necessary AA quality depends on the angle
            float minSinCos = Math.min(Math.abs(sin), Math.abs(cos));
            if ((minSinCos > 0.5) || (hasDistortion && distortion > 0.5)) {
                setAaRes(3);
            } else if (minSinCos > 0.15 || (hasDistortion && distortion > 0.2)) {
                setAaRes(4);
            } else if (minSinCos > 0.1 || (hasDistortion && distortion > 0.1)) {
                setAaRes(5);
            } else if (minSinCos > 0.07 || (hasDistortion && distortion > 0.05)) {
                setAaRes(6);
            } else if (minSinCos > 0.03 || (hasDistortion && distortion > 0.02)) {
                setAaRes(7);
            } else {
                setAaRes(8);
            }
        }
    }

    public void setDistortion(double distortion) {
        this.distortion = distortion;
    }

    public void setPhase(double phase) {
        this.phase = phase;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        float nx = (m00 * x + m01 * y) / xScale;
        float ny = (m10 * x + m11 * y) / yScale;
        if (distortion > 0) {
            nx += (float) (distortion * FastMath.sinQuick(ny + phase));
            ny += (float) (distortion * FastMath.sinQuick(nx + phase));
        }

        // guaranteed to be positive
        float pnx = nx + 100000;
        float pny = ny + 100000;

        // integer parts
        int inx = (int) pnx;
        int iny = (int) pny;

        // fractional part
        float dxFrac = pnx - inx;
        float dyFrac = pny - iny;

        boolean needsAA = false;
        // the fuzziness condition is imperfect, because very small images benefit
        // from AA even with a small fuzziness, but in larger images with small
        // fuzziness, the AA is an artifact
        if ((!straight || distortion > 0) && fuzziness == 0) {
            needsAA = dxFrac < aaThresholdX || dyFrac < aaThresholdY || dxFrac > upperAaThresholdX || dyFrac > upperAaThresholdY;
        }

        float f; // blend factor
        if (needsAA) {
            float p = 0;

            for (int i = 0; i < aaRes; i++) {
                float yy = y + invAaRes * i - aaShift;
                for (int j = 0; j < aaRes; j++) {
                    float xx = x + invAaRes * j - aaShift;
                    p += calcSubPixelInterpolation(xx, yy);
                }
            }
            f = p / aaRes2;
        } else {
            f = ((inx % 2) == (iny % 2)) ? 0.0f : 1.0f;
        }

        int actualTileX = inx - 100000;
        int actualTileY = iny - 100000;
        int tileSum = actualTileX + actualTileY;
        int numColors = colors.length;

        if (fuzziness != 0) {
            // The fuzziness implementation is based on
            // the checker of http://doup.github.io/sapo.js/

            // fold coordinates to get distance from nearest edge
            float dxFolded = dxFrac < 0.5f ? dxFrac : 1.0f - dxFrac;
            float dyFolded = dyFrac < 0.5f ? dyFrac : 1.0f - dyFrac;
            float minFoldedDist = Math.min(dxFolded, dyFolded);

            float fuzzThreshold = (fuzziness / 100.0f);
            if (minFoldedDist < fuzzThreshold) { // pixel is in the fuzzy region
                // the color of the tile the pixel is in
                int currentColor = colors[(tileSum % numColors + numColors) % numColors];

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
                int neighborColor = colors[(neighborSum % numColors + numColors) % numColors];
                float mixingProportion = 0.5f + 0.5f * (minFoldedDist / fuzzThreshold);
                return ImageMath.mixColors(mixingProportion, neighborColor, currentColor);
            }
        }

        // this part is reached if:
        // 1. fuzziness == 0
        // 2. fuzziness != 0 but pixel is outside the fuzz band

        int colorIndexEven; // absolute sum for the even-sum-type diagonal in the pair
        int colorIndexOdd;  // absolute sum for the odd-sum-type diagonal in the pair

        // determine the absolute sums for the even/odd pair of diagonals related to the current tile
        if (((tileSum % 2) + 2) % 2 == 0) {
            colorIndexEven = tileSum;
            colorIndexOdd = tileSum + 1;
        } else {
            colorIndexEven = tileSum - 1;
            colorIndexOdd = tileSum;
        }

        // the color indices for the colors array
        int index0 = (colorIndexEven % numColors + numColors) % numColors;
        int index1 = (colorIndexOdd % numColors + numColors) % numColors;

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

    private float calcSubPixelInterpolation(float x, float y) {
        float nx = (m00 * x + m01 * y) / xScale;
        float ny = (m10 * x + m11 * y) / yScale;
        if (distortion > 0) {
            nx += (float) (distortion * FastMath.sinQuick(ny + phase));
            ny += (float) (distortion * FastMath.sinQuick(nx + phase));
        }

        // guaranteed to be positive
        float pnx = nx + 100000;
        float pny = ny + 100000;

        // integer parts
        int inx = (int) pnx;
        int iny = (int) pny;

        return ((inx % 2) == (iny % 2)) ? 0.0f : 1.0f;
    }

    @Override
    public String toString() {
        return "Texture/Checkerboard...";
    }
}

