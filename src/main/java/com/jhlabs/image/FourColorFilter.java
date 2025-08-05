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

    private int interpolation = INTERPOLATION_LINEAR;
    private int colorSpace = SPACE_LINEAR_RGB;

    private int width;
    private int height;

    // a, R, G, B components in the corners
    private int aNW, rNW, gNW, bNW;
    private int aNE, rNE, gNE, bNE;
    private int aSW, rSW, gSW, bSW;
    private int aSE, rSE, gSE, bSE;

    // corner colors in the working color space
    private final float[] cNW = new float[3];
    private final float[] cNE = new float[3];
    private final float[] cSW = new float[3];
    private final float[] cSE = new float[3];

    public FourColorFilter(String filterName) {
        super(filterName);

        setColorNW(0xffff0000);
        setColorNE(0xffff00ff);
        setColorSW(0xff0000ff);
        setColorSE(0xff00ffff);
    }

    /**
     * Sets the color at the North-West corner.
     */
    public void setColorNW(int color) {
        aNW = (color >> 24) & 0xff;
        rNW = (color >> 16) & 0xff;
        gNW = (color >> 8) & 0xff;
        bNW = color & 0xff;
    }

    /**
     * Sets the color at the North-East corner.
     */
    public void setColorNE(int color) {
        aNE = (color >> 24) & 0xff;
        rNE = (color >> 16) & 0xff;
        gNE = (color >> 8) & 0xff;
        bNE = color & 0xff;
    }

    /**
     * Sets the color at the South-West corner.
     */
    public void setColorSW(int color) {
        aSW = (color >> 24) & 0xff;
        rSW = (color >> 16) & 0xff;
        gSW = (color >> 8) & 0xff;
        bSW = color & 0xff;
    }

    /**
     * Sets the color at the South-East corner.
     */
    public void setColorSE(int color) {
        aSE = (color >> 24) & 0xff;
        rSE = (color >> 16) & 0xff;
        gSE = (color >> 8) & 0xff;
        bSE = color & 0xff;
    }

    /**
     * Sets the interpolation type.
     */
    public void setInterpolation(int interpolation) {
        this.interpolation = interpolation;
    }

    /**
     * Sets the color space for interpolation.
     */
    public void setColorSpace(int colorSpace) {
        this.colorSpace = colorSpace;
    }

    @Override
    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        convertCornerColors();
        super.setDimensions(width, height);
    }

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

    private float interpolate(float a, float b, float ratio) {
        return switch (interpolation) {
            case INTERPOLATION_CUBIC -> qubic(a, b, ratio);
            case INTERPOLATION_QUINTIC -> quintic(a, b, ratio);
            case INTERPOLATION_SEPTIC -> septic(a, b, ratio);
            default -> a * (1 - ratio) + b * ratio;
        };
    }

    private static float qubic(float a, float b, float ratio) {
        float x2 = ratio * ratio;
        float x3 = ratio * x2;
        float p = 3 * x2 - 2 * x3;

        return a * (1 - p) + b * p;
    }

    private static float quintic(float a, float b, float ratio) {
        float x2 = ratio * ratio;
        float x3 = x2 * ratio;
        float x4 = x3 * ratio;
        float x5 = x4 * ratio;
        float p = 6 * x5 - 15 * x4 + 10 * x3;

        return a * (1 - p) + b * p;
    }

    private static float septic(float a, float b, float ratio) {
        float x2 = ratio * ratio;
        float x3 = x2 * ratio;
        float x4 = x3 * ratio;
        float x5 = x4 * ratio;
        float x6 = x5 * ratio;
        float x7 = x6 * ratio;
        return a + (-35 * a + 35 * b) * x4 + (84 * a - 84 * b) * x5 + (-70 * a + 70 * b) * x6 + (20 * a - 20 * b) * x7;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        // calculate normalized x and y coordinates (0.0 to 1.0)
        float fx = (float) x / width;
        float fy = (float) y / height;
        float p, q;

        // alpha is always interpolated in a linear way
        p = interpolate(aNW, aNE, fx);
        q = interpolate(aSW, aSE, fx);
        int a = (int) (interpolate(p, q, fy) + 0.5f);

        // interpolate color components in the chosen space
        p = interpolate(cNW[0], cNE[0], fx);
        q = interpolate(cSW[0], cSE[0], fx);
        float c1 = interpolate(p, q, fy);

        p = interpolate(cNW[1], cNE[1], fx);
        q = interpolate(cSW[1], cSE[1], fx);
        float c2 = interpolate(p, q, fy);

        p = interpolate(cNW[2], cNE[2], fx);
        q = interpolate(cSW[2], cSE[2], fx);
        float c3 = interpolate(p, q, fy);

        return switch (colorSpace) {
            case SPACE_SRGB -> {
                int r = ImageMath.clamp((int) (c1 + 0.5f), 0, 255);
                int g = ImageMath.clamp((int) (c2 + 0.5f), 0, 255);
                int b = ImageMath.clamp((int) (c3 + 0.5f), 0, 255);
                yield (a << 24) | (r << 16) | (g << 8) | b;
            }
            case SPACE_LINEAR_RGB -> {
                int r = ColorSpaces.linearToSRGBInt(c1);
                int g = ColorSpaces.linearToSRGBInt(c2);
                int b = ColorSpaces.linearToSRGBInt(c3);
                yield (a << 24) | (r << 16) | (g << 8) | b;
            }
            case SPACE_OKLAB -> {
                int srgb = ColorSpaces.oklabToSrgb(new float[]{c1, c2, c3});
                yield (a << 24) | (srgb & 0x00FFFFFF);
            }
            default -> throw new IllegalStateException("Unexpected value: " + colorSpace);
        };
    }

    @Override
    public String toString() {
        return "Texture/Four Color Fill...";
    }
}
