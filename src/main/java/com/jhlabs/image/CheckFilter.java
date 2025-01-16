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

/**
 * A Filter to draw grids and check patterns.
 */
public class CheckFilter extends PointFilter {
    private int xScale = 8;
    private int yScale = 8;
    private int foreground = 0xffffffff;
    private int background = 0xff000000;
    private int fuzziness = 0;
    private float angle = 0.0f;
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

    /**
     * Set the foreground color.
     *
     * @param foreground the color.
     */
    public void setForeground(int foreground) {
        this.foreground = foreground;
    }

    /**
     * Set the background color.
     *
     * @param background the color.
     */
    public void setBackground(int background) {
        this.background = background;
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
        this.angle = angle;
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
            nx += distortion * FastMath.sinQuick(ny + phase);
            ny += distortion * FastMath.sinQuick(nx + phase);
        }

        // guaranteed to be positive
        float pnx = nx + 100000;
        float pny = ny + 100000;

        // integer parts
        int inx = (int) pnx;
        int iny = (int) pny;

        float dx = pnx - inx;
        float dy = pny - iny;

        boolean needsAA = false;
        // the fuzziness condition is imperfect, because very small images benefit
        // from AA even with a small fuzziness, but in larger images with small
        // fuzziness, the AA is an artifact
        if ((!straight || distortion > 0) && fuzziness == 0) {
            needsAA = dx < aaThresholdX || dy < aaThresholdY || dx > upperAaThresholdX || dy > upperAaThresholdY;
        }

        float f;
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

        if (fuzziness != 0) {
            // The fuzziness implementation is based on
            // the checker of http://doup.github.io/sapo.js/

            // In order to implement fuzziness, we have to know
            // the distance from the cell border.
            // We "fold" the coordinates to simplify the edge
            // detection. dx and dy are now in the range [ 0, 0.5 ),
            // and only their lower part must be checked
            //
            dx = dx < 0.5f ? dx : 1.0f - dx;
            dy = dy < 0.5f ? dy : 1.0f - dy;

            // After this diagonal folding only the lower part of dy has to be checked
            if (dy > dx) {
                dy = dx;
            }

            float fuzz = (fuzziness / 100.0f);
            if (dy < fuzz) { // close to the border
                fuzz = ((dy / fuzz) / 2.0f) + 0.5f;
                fuzz = ImageMath.smoothStep(0.0f, 1.0f, fuzz);

                if (f == 1.0f) { // foreground color
                    f = fuzz;
                } else if (f == 0.0f) { // background color
                    f = 1.0f - fuzz;
                }
            }
        }

        if (f == 0.0f) {
            return foreground;
        }
        if (f == 1.0f) {
            return background;
        }

        return ImageMath.mixColors(f, foreground, background);
    }

    private float calcSubPixelInterpolation(float x, float y) {
        float nx = (m00 * x + m01 * y) / xScale;
        float ny = (m10 * x + m11 * y) / yScale;
        if (distortion > 0) {
            nx += distortion * FastMath.sinQuick(ny + phase);
            ny += distortion * FastMath.sinQuick(nx + phase);
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

