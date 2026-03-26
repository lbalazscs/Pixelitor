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
    private int blurMethod = BOX3_BLUR;

    private float threshold;
    private float softness = 0;
    private float radius = 5;
    private float lowerThreshold;
    private float upperThreshold;
    private int light = 0xFF_FF_FF_FF;
    private int dark = 0xFF_00_00_00;

    public StampFilter(String filterName) {
        super(filterName);
    }

    /**
     * Sets the radius of the effect.
     *
     * @param radius the radius (must be >= 0)
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * Sets the threshold value.
     *
     * @param threshold the threshold value
     */
    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    /**
     * Sets the softness of the effect.
     *
     * @param softness the softness (in the range [0, 1])
     */
    public void setSoftness(float softness) {
        this.softness = softness;
    }

    /**
     * Sets the color to be used for pixels above the upper threshold.
     *
     * @param light the color
     */
    public void setLight(int light) {
        this.light = light;
    }

    /**
     * Sets the color to be used for pixels below the lower threshold.
     *
     * @param dark the color
     */
    public void setDark(int dark) {
        this.dark = dark;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (blurMethod == BOX3_BLUR) {
            if ((src.getWidth() == 1) || (src.getHeight() == 1)) {
                // avoid ArrayIndexOutOfBoundsException in BoxBlurFilter
                return src;
            }
            dst = new BoxBlurFilter(radius, radius, 3, filterName).filter(src, null);
        } else if (blurMethod == GAUSSIAN_BLUR) {
            dst = new GaussianFilter(filterName, radius).filter(src, null);
        } else {
            throw new IllegalStateException("blurMethod = " + blurMethod);
        }

        lowerThreshold = 255 * (threshold - softness * 0.5f);
        upperThreshold = 255 * (threshold + softness * 0.5f);
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

    public void setBlurMethod(int blurMethod) {
        this.blurMethod = blurMethod;
    }
}
