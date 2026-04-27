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

import java.awt.image.BufferedImage;

/**
 * A filter which produces a rubber-stamp type of effect by performing a thresholded blur.
 */
public class StampFilter extends PointFilter {
    public static final int BOX3_BLUR = 2;
    public static final int GAUSSIAN_BLUR = 3;

    private final int blurMethod;
    private final double threshold;
    private final double softness;
    private final float radius;
    private final int light;
    private final int dark;

    private float lowerThreshold;
    private float upperThreshold;

    /**
     * Creates a new {@link StampFilter}.
     *
     * @param filterName the name of the filter
     * @param radius     the blur radius controlling the spread of the stamp effect
     * @param threshold  the luminance threshold that separates dark from light regions,
     *                   expressed as a fraction in the range [0, 1]
     * @param softness   the width of the transition zone around the threshold,
     *                   expressed as a fraction in the range [0, 1]; 0 produces a hard edge
     * @param light      the ARGB color applied to pixels above the upper threshold
     * @param dark       the ARGB color applied to pixels below the lower threshold
     * @param blurMethod the blur algorithm to use
     */
    public StampFilter(String filterName, float radius, double threshold,
                       double softness, int light, int dark, int blurMethod) {
        super(filterName);
        this.radius = radius;
        this.threshold = threshold;
        this.softness = softness;
        this.light = light;
        this.dark = dark;
        this.blurMethod = blurMethod;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (blurMethod == BOX3_BLUR) {
            dst = new BoxBlurFilter(filterName, radius, radius, 3).filter(src, null);
        } else if (blurMethod == GAUSSIAN_BLUR) {
            dst = new GaussianFilter(filterName, radius).filter(src, null);
        } else {
            throw new IllegalStateException("blurMethod = " + blurMethod);
        }

        lowerThreshold = (float) (255 * (threshold - softness * 0.5));
        upperThreshold = (float) (255 * (threshold + softness * 0.5));
        return super.filter(dst, dst);
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        int origA = rgb & 0xFF_00_00_00;
        int lum = ImageMath.calcLuminanceInt(rgb);
        float f = ImageMath.smoothStep(lowerThreshold, upperThreshold, lum);

        int mixed = ImageMath.mixColors(f, dark, light);
        // TODO This keeps the blurred alpha, but it would be
        //   better to keep the original src alpha
        return origA | (mixed & 0x00_FF_FF_FF);
    }
}
