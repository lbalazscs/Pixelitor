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
import pixelitor.colors.ColorSpaces;

import java.util.Arrays;

/**
 * An abstract superclass for point filters which draw a four color
 * gradient. It manages the shared state for this family of filters,
 * including the four corner colors, color space conversions,
 * interpolation math, and the relative center coordinates.
 * Subclasses are responsible for defining the specific spatial
 * algorithm used to blend the colors across the image.
 */
public abstract class FourColorFilter extends PointFilter {
    private static final double EPSILON = 1.0e-6;

    public enum InterpolationType {
        LINEAR("Linear") {
            @Override
            public float calcInterpolatedWeight(float ratio) {
                return ratio;
            }
        }, CUBIC("Cubic") {
            @Override
            public float calcInterpolatedWeight(float ratio) {
                return ImageMath.smoothStep01(ratio);
            }
        }, QUINTIC("Quintic") {
            @Override
            public float calcInterpolatedWeight(float ratio) {
                return ImageMath.smootherStep01(ratio);
            }
        }, SEPTIC("Septic") {
            @Override
            public float calcInterpolatedWeight(float ratio) {
                float x2 = ratio * ratio;
                float x4 = x2 * x2;
                // Horner's method for faster evaluation of 35x^4 - 84x^5 + 70x^6 - 20x^7
                return x4 * (35 - ratio * (84 - ratio * (70 - 20 * ratio)));
            }
        };

        private final String displayName;

        InterpolationType(String displayName) {
            this.displayName = displayName;
        }

        public abstract float calcInterpolatedWeight(float ratio);

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum ColorSpaceType {
        OKLAB("Oklab") {
            @Override
            public int toSrgb(int a, float c1, float c2, float c3) {
                int srgb = ColorSpaces.oklabToSrgb(c1, c2, c3);
                return (a << 24) | (srgb & 0x00_FF_FF_FF);
            }
        }, LINEAR_RGB("Linear RGB") {
            @Override
            public int toSrgb(int a, float c1, float c2, float c3) {
                int r = ColorSpaces.linearToSrgbInt(c1);
                int g = ColorSpaces.linearToSrgbInt(c2);
                int b = ColorSpaces.linearToSrgbInt(c3);
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
        }, SRGB("sRGB") {
            @Override
            public int toSrgb(int a, float c1, float c2, float c3) {
                int r = ImageMath.clamp((int) (c1 + 0.5f), 0, 255);
                int g = ImageMath.clamp((int) (c2 + 0.5f), 0, 255);
                int b = ImageMath.clamp((int) (c3 + 0.5f), 0, 255);
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
        };

        private final String displayName;

        ColorSpaceType(String displayName) {
            this.displayName = displayName;
        }

        public abstract int toSrgb(int a, float c1, float c2, float c3);

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * A helper record to bind an angle to its corresponding
     * corner color and allow sorting by angle.
     */
    protected record Corner(double angle, double dist, int a, float c1, float c2,
                            float c3) implements Comparable<Corner> {
        private Corner(double angle, double dist, int a, float[] c) {
            this(angle, dist, a, c[0], c[1], c[2]);
        }

        @Override
        public int compareTo(Corner o) {
            return Double.compare(this.angle, o.angle);
        }
    }

    protected final InterpolationType interpolation;
    protected final ColorSpaceType colorSpace;
    protected final double relCx;
    protected final double relCy;

    protected final int width;
    protected final int height;
    protected final double cx;
    protected final double cy;

    // corner alphas (only components we need to retain as integers)
    protected final int aNW, aNE, aSW, aSE;

    // corner colors in the working color space
    protected final float[] cNW = new float[3];
    protected final float[] cNE = new float[3];
    protected final float[] cSW = new float[3];
    protected final float[] cSE = new float[3];

    // fields used only by the angular and polar subclasses
    protected Corner[] corners;
    protected double invRange01, invRange12, invRange23, invRange30;

    /**
     * Constructs a {@link FourColorFilter}.
     *
     * @param filterName    the name of the filter.
     * @param colorNW       the color at the North-West corner.
     * @param colorNE       the color at the North-East corner.
     * @param colorSW       the color at the South-West corner.
     * @param colorSE       the color at the South-East corner.
     * @param interpolation the interpolation type.
     * @param colorSpace    the color space for interpolation.
     * @param relCx         the relative X coordinate (0.0 to 1.0) of the midpoint.
     * @param relCy         the relative Y coordinate (0.0 to 1.0) of the midpoint.
     * @param width         the width of the generated image (in pixels).
     * @param height        the height of the generated image (in pixels).
     */
    protected FourColorFilter(String filterName,
                              int colorNW, int colorNE, int colorSW, int colorSE,
                              InterpolationType interpolation, ColorSpaceType colorSpace,
                              double relCx, double relCy, int width, int height) {
        super(filterName);

        this.interpolation = interpolation;
        this.colorSpace = colorSpace;
        this.relCx = relCx;
        this.relCy = relCy;

        this.aNW = colorNW >>> 24;
        this.aNE = colorNE >>> 24;
        this.aSW = colorSW >>> 24;
        this.aSE = colorSE >>> 24;

        convertAndSetCorner(cNW, colorNW);
        convertAndSetCorner(cNE, colorNE);
        convertAndSetCorner(cSW, colorSW);
        convertAndSetCorner(cSE, colorSE);

        this.width = width;
        this.height = height;

        this.cx = relCx * width;
        this.cy = relCy * height;
    }

    // the corner colors are converted once at setup time into the chosen space
    private void convertAndSetCorner(float[] corner, int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        switch (colorSpace) {
            case SRGB -> {
                corner[0] = r;
                corner[1] = g;
                corner[2] = b;
            }
            case LINEAR_RGB -> {
                corner[0] = (float) ColorSpaces.SRGB_TO_LINEAR_LUT[r];
                corner[1] = (float) ColorSpaces.SRGB_TO_LINEAR_LUT[g];
                corner[2] = (float) ColorSpaces.SRGB_TO_LINEAR_LUT[b];
            }
            case OKLAB -> {
                float[] oklab = ColorSpaces.srgbToOklab(argb);
                corner[0] = oklab[0];
                corner[1] = oklab[1];
                corner[2] = oklab[2];
            }
        }
    }

    /**
     * Computes the alpha at the center using bilinear interpolation.
     */
    protected float calcWeightedCenterAlpha() {
        return ImageMath.bilerp((float) relCx, (float) relCy, aNW, aNE, aSW, aSE);
    }

    /**
     * Computes the color at the center using bilinear interpolation.
     */
    protected float[] calcWeightedCenterColor() {
        float fx = (float) relCx;
        float fy = (float) relCy;

        float[] cCenter = new float[3];
        cCenter[0] = ImageMath.bilerp(fx, fy, cNW[0], cNE[0], cSW[0], cSE[0]);
        cCenter[1] = ImageMath.bilerp(fx, fy, cNW[1], cNE[1], cSW[1], cSE[1]);
        cCenter[2] = ImageMath.bilerp(fx, fy, cNW[2], cNE[2], cSW[2], cSE[2]);
        return cCenter;
    }

    /**
     * Shared setup for filters that rely on angular sweeping logic.
     */
    protected void setupAngularData(boolean needsDist) {
        // calculate the angle for each corner relative to the center
        double angleNW = ImageMath.mod(FastMath.atan2(-cy, -cx), Math.PI * 2.0);
        double angleNE = ImageMath.mod(FastMath.atan2(-cy, width - cx), Math.PI * 2.0);
        double angleSW = ImageMath.mod(FastMath.atan2(height - cy, -cx), Math.PI * 2.0);
        double angleSE = ImageMath.mod(FastMath.atan2(height - cy, width - cx), Math.PI * 2.0);

        // calculate distance from center to each corner
        double distNW = 0, distNE = 0, distSW = 0, distSE = 0;
        if (needsDist) {
            double cxr = width - cx;
            double cyr = height - cy;
            distNW = Math.sqrt(cx * cx + cy * cy);
            distNE = Math.sqrt(cxr * cxr + cy * cy);
            distSW = Math.sqrt(cx * cx + cyr * cyr);
            distSE = Math.sqrt(cxr * cxr + cyr * cyr);
        }

        corners = new Corner[4];
        corners[0] = new Corner(angleNW, distNW, aNW, cNW);
        corners[1] = new Corner(angleNE, distNE, aNE, cNE);
        corners[2] = new Corner(angleSW, distSW, aSW, cSW);
        corners[3] = new Corner(angleSE, distSE, aSE, cSE);

        // sort them by angle ascending to easily locate the bounding segments
        // (the order changes if the gradient center is outside canvas bounds!)
        Arrays.sort(corners);

        double r01 = corners[1].angle - corners[0].angle;
        invRange01 = r01 <= EPSILON ? 0.0 : 1.0 / r01;

        double r12 = corners[2].angle - corners[1].angle;
        invRange12 = r12 <= EPSILON ? 0.0 : 1.0 / r12;

        double r23 = corners[3].angle - corners[2].angle;
        invRange23 = r23 <= EPSILON ? 0.0 : 1.0 / r23;

        double r30 = (corners[0].angle + Math.PI * 2.0) - corners[3].angle;
        invRange30 = r30 <= EPSILON ? 0.0 : 1.0 / r30;
    }

    /**
     * Determines which angular segment a pixel belongs to.
     */
    protected int calcAngularSegment(double angle) {
        // exploit the fact that the corners array is sorted by angle in ascending order
        if (angle < corners[0].angle) {
            return 3; // wrapped around the 0 / 2π seam
        }
        if (angle < corners[1].angle) {
            return 0;
        }
        if (angle < corners[2].angle) {
            return 1;
        }
        if (angle < corners[3].angle) {
            return 2;
        }
        return 3;
    }
}
