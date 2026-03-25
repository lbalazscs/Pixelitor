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
    public static final int INTERPOLATION_LINEAR = 0;
    public static final int INTERPOLATION_CUBIC = 1;
    public static final int INTERPOLATION_QUINTIC = 2;
    public static final int INTERPOLATION_SEPTIC = 3;

    public static final int SPACE_OKLAB = 0;
    public static final int SPACE_LINEAR_RGB = 1;
    public static final int SPACE_SRGB = 2;

    private final int interpolation;
    private final int colorSpace;

    private int width;
    private int height;

    // horizontal and vertical interpolation weights
    private float[] fxWeight;
    private float[] fyWeight;

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
     * Constructs a FourColorFilter.
     *
     * @param filterName    the name of the filter.
     * @param colorNW       the color at the North-West corner.
     * @param colorNE       the color at the North-East corner.
     * @param colorSW       the color at the South-West corner.
     * @param colorSE       the color at the South-East corner.
     * @param interpolation the interpolation type.
     * @param colorSpace    the color space for interpolation.
     */
    public FourColorFilter(String filterName, int colorNW, int colorNE, int colorSW, int colorSE, int interpolation, int colorSpace) {
        super(filterName);

        this.interpolation = interpolation;
        this.colorSpace = colorSpace;

        this.aNW = colorNW >>> 24;
        this.rNW = (colorNW >> 16) & 0xff;
        this.gNW = (colorNW >> 8) & 0xff;
        this.bNW = colorNW & 0xff;

        this.aNE = colorNE >>> 24;
        this.rNE = (colorNE >> 16) & 0xff;
        this.gNE = (colorNE >> 8) & 0xff;
        this.bNE = colorNE & 0xff;

        this.aSW = colorSW >>> 24;
        this.rSW = (colorSW >> 16) & 0xff;
        this.gSW = (colorSW >> 8) & 0xff;
        this.bSW = colorSW & 0xff;

        this.aSE = colorSE >>> 24;
        this.rSE = (colorSE >> 16) & 0xff;
        this.gSE = (colorSE >> 8) & 0xff;
        this.bSE = colorSE & 0xff;
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
        fxWeight = new float[width];
        for (int x = 0; x < width; x++) {
            fxWeight[x] = calcInterpolatedWeight((float) x / width);
        }

        fyWeight = new float[height];
        for (int y = 0; y < height; y++) {
            fyWeight[y] = calcInterpolatedWeight((float) y / height);
        }
    }

    private float calcInterpolatedWeight(float ratio) {
        return switch (interpolation) {
            case INTERPOLATION_CUBIC -> ImageMath.smoothStep01(ratio);
            case INTERPOLATION_QUINTIC -> ImageMath.smootherStep01(ratio);
            case INTERPOLATION_SEPTIC -> septicWeight(ratio);
            default -> ratio;
        };
    }

    private static float septicWeight(float ratio) {
        float x2 = ratio * ratio;
        float x4 = x2 * x2;
        float x5 = x4 * ratio;
        float x6 = x5 * ratio;
        float x7 = x6 * ratio;
        return 35 * x4 - 84 * x5 + 70 * x6 - 20 * x7;
    }

    // the corner colors are converted once at setup time into the chosen space
    private void convertCornerColors() {
        switch (colorSpace) {
            case SPACE_SRGB -> {
                setCorner(cNW, rNW, gNW, bNW);
                setCorner(cNE, rNE, gNE, bNE);
                setCorner(cSW, rSW, gSW, bSW);
                setCorner(cSE, rSE, gSE, bSE);
            }
            case SPACE_LINEAR_RGB -> {
                setCornerLinear(cNW, rNW, gNW, bNW);
                setCornerLinear(cNE, rNE, gNE, bNE);
                setCornerLinear(cSW, rSW, gSW, bSW);
                setCornerLinear(cSE, rSE, gSE, bSE);
            }
            case SPACE_OKLAB -> {
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
        float fx = fxWeight[x];
        float fy = fyWeight[y];

        // interpolate alpha
        int a = (int) (bilerp(fx, fy, aNW, aNE, aSW, aSE) + 0.5f);

        // interpolate color components in the chosen space
        float c1 = bilerp(fx, fy, cNW[0], cNE[0], cSW[0], cSE[0]);
        float c2 = bilerp(fx, fy, cNW[1], cNE[1], cSW[1], cSE[1]);
        float c3 = bilerp(fx, fy, cNW[2], cNE[2], cSW[2], cSE[2]);

        // convert back to sRGB for output
        return switch (colorSpace) {
            case SPACE_SRGB -> {
                int r = ImageMath.clamp((int) (c1 + 0.5f), 0, 255);
                int g = ImageMath.clamp((int) (c2 + 0.5f), 0, 255);
                int b = ImageMath.clamp((int) (c3 + 0.5f), 0, 255);
                yield (a << 24) | (r << 16) | (g << 8) | b;
            }
            case SPACE_LINEAR_RGB -> {
                int r = ColorSpaces.linearToSrgbInt(c1);
                int g = ColorSpaces.linearToSrgbInt(c2);
                int b = ColorSpaces.linearToSrgbInt(c3);
                yield (a << 24) | (r << 16) | (g << 8) | b;
            }
            case SPACE_OKLAB -> {
                int srgb = ColorSpaces.oklabToSrgb(c1, c2, c3);
                yield (a << 24) | (srgb & 0x00_FF_FF_FF);
            }
            default -> throw new IllegalStateException("Unexpected value: " + colorSpace);
        };
    }
}
