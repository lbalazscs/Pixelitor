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

import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * A filter that blurs a copy of the image and additively blends it
 * back onto the original, brightening highlights into a soft halo.
 *
 * @author Jerry Huxtable
 */
public class GlowFilter extends AbstractBufferedImageOp {
    private final float amount;
    private final float radius;

    /**
     * Creates a new GlowFilter with the specified parameters.
     *
     * @param filterName the name of the filter
     * @param amount     the amount of glow (in the range [0, 1])
     * @param radius     the blur radius
     */
    public GlowFilter(String filterName, float amount, float radius) {
        super(filterName);

        assert amount >= 0 && radius >= 0;

        this.amount = amount;
        this.radius = radius;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        BufferedImage srcCopy = ImageUtils.copyImage(src);
        if (amount == 0.0f) {
            return srcCopy;
        }

        int width = src.getWidth();
        int height = src.getHeight();
        int[] inPixels = ImageUtils.getPixels(src);

        if (radius > 0) {
            // most of the time is spent here, so this filter doesn't
            // track progress; the blur filter manages its own
            // progress tracker (using the glow filter's name)
            srcCopy = new BoxBlurFilter(filterName, radius, radius, 3)
                .filter(srcCopy, srcCopy);
        }

        int[] outPixels = ImageUtils.getPixels(srcCopy);

        float f = 4 * amount;

        for (int i = 0; i < inPixels.length; i++) {
            int srcRgb = inPixels[i];
            int r1 = (srcRgb >> 16) & 0xFF;
            int g1 = (srcRgb >> 8) & 0xFF;
            int b1 = srcRgb & 0xFF;

            int blurRgb = outPixels[i];
            int r2 = (blurRgb >> 16) & 0xFF;
            int g2 = (blurRgb >> 8) & 0xFF;
            int b2 = blurRgb & 0xFF;

            r1 = PixelUtils.max255((int) (r1 + f * r2));
            g1 = PixelUtils.max255((int) (g1 + f * g2));
            b1 = PixelUtils.max255((int) (b1 + f * b2));

            outPixels[i] = (srcRgb & 0xFF_00_00_00) | (r1 << 16) | (g1 << 8) | b1;
        }

        if (dst == null) {
            return srcCopy;
        }

        setRGB(dst, 0, 0, width, height, outPixels);

        return dst;
    }
}
