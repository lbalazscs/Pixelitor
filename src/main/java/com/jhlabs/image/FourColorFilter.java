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

/**
 * A filter which draws a gradient interpolated between four colors defined at the corners of the image.
 */
public class FourColorFilter extends PointFilter {
    public static final int INTERPOLATION_LINEAR = 0;
    public static final int INTERPOLATION_CUBIC = 1;
    public static final int INTERPOLATION_QUINTIC = 2;
    public static final int INTERPOLATION_SEPTIC = 3;
    private int interpolation = INTERPOLATION_LINEAR;

    public static final int SPACE_SRGB = 0;
    public static final int SPACE_LINEAR = 1;
    private boolean linearSpace = true;

    private int width;
    private int height;

    // A, R, G, B components in the corners
    private int aNW, rNW, gNW, bNW;
    private int aNE, rNE, gNE, bNE;
    private int aSW, rSW, gSW, bSW;
    private int aSE, rSE, gSE, bSE;

    public FourColorFilter(String filterName) {
        super(filterName);

        setColorNW(0xffff0000);
        setColorNE(0xffff00ff);
        setColorSW(0xff0000ff);
        setColorSE(0xff00ffff);
    }

    public void setColorNW(int color) {
        aNW = (color >> 24) & 0xff;
        rNW = (color >> 16) & 0xff;
        gNW = (color >> 8) & 0xff;
        bNW = color & 0xff;
    }

    public void setColorNE(int color) {
        aNE = (color >> 24) & 0xff;
        rNE = (color >> 16) & 0xff;
        gNE = (color >> 8) & 0xff;
        bNE = color & 0xff;
    }

    public void setColorSW(int color) {
        aSW = (color >> 24) & 0xff;
        rSW = (color >> 16) & 0xff;
        gSW = (color >> 8) & 0xff;
        bSW = color & 0xff;
    }

    public void setColorSE(int color) {
        aSE = (color >> 24) & 0xff;
        rSE = (color >> 16) & 0xff;
        gSE = (color >> 8) & 0xff;
        bSE = color & 0xff;
    }

    public void setInterpolation(int interpolation) {
        this.interpolation = interpolation;
    }

    public void setLinearSpace(boolean linearSpace) {
        this.linearSpace = linearSpace;
    }

    @Override
    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        super.setDimensions(width, height);
    }

    private float interpolate(float a, float b, float ratio) {
        return switch (interpolation) {
            case INTERPOLATION_CUBIC -> qubic(a, b, ratio);
            case INTERPOLATION_QUINTIC -> quintic(a, b, ratio);
            case INTERPOLATION_SEPTIC -> septic(a, b, ratio);
            default -> a * (1 - ratio) + b * ratio;
        };
    }

    private float gammaInterpolate(float a, float b, float ratio) {
        if (!linearSpace) {
            return interpolate(a, b, ratio);
        }

        // approximated gamma calculation, use 2 instead of 2.2
        float aLinear = a * a / 65025.0f; // 255 * 255 = 65025
        float bLinear = b * b / 65025.0f;
        float resultLinear = interpolate(aLinear, bLinear, ratio);

        return (float) (Math.sqrt(resultLinear) * 255.0f);
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
    public int filterRGB(int x, int y, int rgb) {
        // calculate normalized x and y coordinates (0.0 to 1.0)
        float fx = (float) x / width;
        float fy = (float) y / height;
        float p, q;

        // interpolate each component horizontally, then vertically
        p = interpolate(aNW, aNE, fx);
        q = interpolate(aSW, aSE, fx);
        int a = (int) (interpolate(p, q, fy) + 0.5f);

        p = gammaInterpolate(rNW, rNE, fx);
        q = gammaInterpolate(rSW, rSE, fx);
        int r = (int) (gammaInterpolate(p, q, fy) + 0.5f);

        p = gammaInterpolate(gNW, gNE, fx);
        q = gammaInterpolate(gSW, gSE, fx);
        int g = (int) (gammaInterpolate(p, q, fy) + 0.5f);

        p = gammaInterpolate(bNW, bNE, fx);
        q = gammaInterpolate(bSW, bSE, fx);
        int b = (int) (gammaInterpolate(p, q, fy) + 0.5f);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public String toString() {
        return "Texture/Four Color Fill...";
    }
}
