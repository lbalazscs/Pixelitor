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
 * A filter which draws a four color gradient using bilinear interpolation.
 * The midpoint defines the bias or intersection of the horizontal and
 * vertical blends. The colors smoothly interpolate across the X and Y
 * axes between the four corners, creating a rectangular cross-fade effect.
 */
public class FourColorRectFilter extends FourColorFilter {
    private float[] yWeights;

    // precomputed blends based on the X-coordinate ratio
    private float[] topA, bottomA;
    private float[] topC1, bottomC1;
    private float[] topC2, bottomC2;
    private float[] topC3, bottomC3;

    public FourColorRectFilter(String filterName,
                               int colorNW, int colorNE, int colorSW, int colorSE,
                               InterpolationType interpolation, ColorSpaceType colorSpace,
                               double relCx, double relCy, int width, int height) {
        super(filterName, colorNW, colorNE, colorSW, colorSE, interpolation, colorSpace, relCx, relCy, width, height);

        precomputeInterpolationWeights();
    }

    private void precomputeInterpolationWeights() {
        // clamp the bias inside [0.001, 0.999] to avoid NaN/Infinity divisions if exactly 0.0 or 1.0
        float xBias = 1.0f - (float) ImageMath.clamp(relCx, 0.001, 0.999);
        float yBias = 1.0f - (float) ImageMath.clamp(relCy, 0.001, 0.999);

        topA = new float[width];
        bottomA = new float[width];
        topC1 = new float[width];
        bottomC1 = new float[width];
        topC2 = new float[width];
        bottomC2 = new float[width];
        topC3 = new float[width];
        bottomC3 = new float[width];

        // precalculating purely horizontal transitions severely drops hot-loop operations per-pixel
        for (int x = 0; x < width; x++) {
            float ratio = (float) x / width;
            if (xBias != 0.5f) {
                ratio = ImageMath.bias(ratio, xBias);
            }
            float fx = interpolation.calcInterpolatedWeight(ratio);

            topA[x] = ImageMath.lerp(fx, (float) aNW, (float) aNE);
            bottomA[x] = ImageMath.lerp(fx, (float) aSW, (float) aSE);
            topC1[x] = ImageMath.lerp(fx, cNW[0], cNE[0]);
            bottomC1[x] = ImageMath.lerp(fx, cSW[0], cSE[0]);
            topC2[x] = ImageMath.lerp(fx, cNW[1], cNE[1]);
            bottomC2[x] = ImageMath.lerp(fx, cSW[1], cSE[1]);
            topC3[x] = ImageMath.lerp(fx, cNW[2], cNE[2]);
            bottomC3[x] = ImageMath.lerp(fx, cSW[2], cSE[2]);
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

    @Override
    public int processPixel(int x, int y, int rgb) {
        float fy = yWeights[y];

        // interpolate alpha
        int a = (int) (ImageMath.lerp(fy, topA[x], bottomA[x]) + 0.5f);

        // interpolate color components in the chosen space
        float c1 = ImageMath.lerp(fy, topC1[x], bottomC1[x]);
        float c2 = ImageMath.lerp(fy, topC2[x], bottomC2[x]);
        float c3 = ImageMath.lerp(fy, topC3[x], bottomC3[x]);

        // convert back to sRGB for output
        return colorSpace.toSrgb(a, c1, c2, c3);
    }
}
