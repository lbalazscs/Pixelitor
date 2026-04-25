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

import pixelitor.utils.ColorSpaces;

/**
 * A filter which draws a gradient interpolated between four colors defined at the corners of the image.
 */
public class FourColorFilter extends PointFilter {

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
                float x5 = x4 * ratio;
                float x6 = x5 * ratio;
                float x7 = x6 * ratio;
                return 35 * x4 - 84 * x5 + 70 * x6 - 20 * x7;
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

    private final InterpolationType interpolation;
    private final ColorSpaceType colorSpace;
    private final double relCx;
    private final double relCy;

    private int width;
    private int height;

    // horizontal and vertical interpolation weights
    private float[] xWeights;
    private float[] yWeights;

    // A, R, G, B components in the corners
    private final int aNW, rNW, gNW, bNW;
    private final int aNE, rNE, gNE, bNE;
    private final int aSW, rSW, gSW, bSW;
    private final int aSE, rSE, gSE, bSE;

    // corner colors in the working color space
    private final float[] cNW = new float[3];
    private final float[] cNE = new float[3];
    private final float[] cSW = new float[3];
    private final float[] cSE = new float[3];

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
     */
    public FourColorFilter(String filterName,
                           int colorNW, int colorNE, int colorSW, int colorSE,
                           InterpolationType interpolation, ColorSpaceType colorSpace,
                           double relCx, double relCy) {
        super(filterName);

        this.interpolation = interpolation;
        this.colorSpace = colorSpace;
        this.relCx = relCx;
        this.relCy = relCy;

        this.aNW = colorNW >>> 24;
        this.rNW = (colorNW >> 16) & 0xFF;
        this.gNW = (colorNW >> 8) & 0xFF;
        this.bNW = colorNW & 0xFF;

        this.aNE = colorNE >>> 24;
        this.rNE = (colorNE >> 16) & 0xFF;
        this.gNE = (colorNE >> 8) & 0xFF;
        this.bNE = colorNE & 0xFF;

        this.aSW = colorSW >>> 24;
        this.rSW = (colorSW >> 16) & 0xFF;
        this.gSW = (colorSW >> 8) & 0xFF;
        this.bSW = colorSW & 0xFF;

        this.aSE = colorSE >>> 24;
        this.rSE = (colorSE >> 16) & 0xFF;
        this.gSE = (colorSE >> 8) & 0xFF;
        this.bSE = colorSE & 0xFF;
    }

    @Override
    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        convertCornerColors();
        precomputeInterpolationWeights();
        super.setDimensions(width, height);
    }

    private void precomputeInterpolationWeights() {
        // clamp the bias inside [0.001, 0.999] to avoid NaN/Infinity divisions if exactly 0.0 or 1.0
        float xBias = 1.0f - (float) ImageMath.clamp(relCx, 0.001, 0.999);
        float yBias = 1.0f - (float) ImageMath.clamp(relCy, 0.001, 0.999);

        xWeights = new float[width];
        for (int x = 0; x < width; x++) {
            float ratio = (float) x / width;
            if (xBias != 0.5f) {
                ratio = ImageMath.bias(ratio, xBias);
            }
            xWeights[x] = interpolation.calcInterpolatedWeight(ratio);
        }

        yWeights = new float[height];
        for (int y = 0; y < height; y++) {
            float ratio = (float) y / height;
            if (yBias != 0.5f) {
                ratio = ImageMath.bias(ratio, yBias);
            }
            yWeights[y] = interpolation.calcInterpolatedWeight(ratio);
        }
    }

    // the corner colors are converted once at setup time into the chosen space
    private void convertCornerColors() {
        switch (colorSpace) {
            case SRGB -> {
                setCorner(cNW, rNW, gNW, bNW);
                setCorner(cNE, rNE, gNE, bNE);
                setCorner(cSW, rSW, gSW, bSW);
                setCorner(cSE, rSE, gSE, bSE);
            }
            case LINEAR_RGB -> {
                setCornerLinear(cNW, rNW, gNW, bNW);
                setCornerLinear(cNE, rNE, gNE, bNE);
                setCornerLinear(cSW, rSW, gSW, bSW);
                setCornerLinear(cSE, rSE, gSE, bSE);
            }
            case OKLAB -> {
                setCornerOklab(cNW, (aNW << 24) | (rNW << 16) | (gNW << 8) | bNW);
                setCornerOklab(cNE, (aNE << 24) | (rNE << 16) | (gNE << 8) | bNE);
                setCornerOklab(cSW, (aSW << 24) | (rSW << 16) | (gSW << 8) | bSW);
                setCornerOklab(cSE, (aSE << 24) | (rSE << 16) | (gSE << 8) | bSE);
            }
        }
    }

    private static void setCorner(float[] corner, int r, int g, int b) {
        corner[0] = r;
        corner[1] = g;
        corner[2] = b;
    }

    private static void setCornerLinear(float[] corner, int r, int g, int b) {
        corner[0] = (float) ColorSpaces.SRGB_TO_LINEAR_LUT[r];
        corner[1] = (float) ColorSpaces.SRGB_TO_LINEAR_LUT[g];
        corner[2] = (float) ColorSpaces.SRGB_TO_LINEAR_LUT[b];
    }

    private static void setCornerOklab(float[] corner, int srgb) {
        float[] oklab = ColorSpaces.srgbToOklab(srgb);
        corner[0] = oklab[0];
        corner[1] = oklab[1];
        corner[2] = oklab[2];
    }

    private static float bilerp(float fx, float fy, float nw, float ne, float sw, float se) {
        float top = ImageMath.lerp(fx, nw, ne);
        float bottom = ImageMath.lerp(fx, sw, se);
        return ImageMath.lerp(fy, top, bottom);
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        float fx = xWeights[x];
        float fy = yWeights[y];

        // interpolate alpha
        int a = (int) (bilerp(fx, fy, aNW, aNE, aSW, aSE) + 0.5f);

        // interpolate color components in the chosen space
        float c1 = bilerp(fx, fy, cNW[0], cNE[0], cSW[0], cSE[0]);
        float c2 = bilerp(fx, fy, cNW[1], cNE[1], cSW[1], cSE[1]);
        float c3 = bilerp(fx, fy, cNW[2], cNE[2], cSW[2], cSE[2]);

        // convert back to sRGB for output
        return colorSpace.toSrgb(a, c1, c2, c3);
    }
}
