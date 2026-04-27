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
 * A filter which adds blur to an image, producing a glowing effect.
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

        this.amount = amount;
        this.radius = radius;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage srcCopy = ImageUtils.copyImage(src);

        int[] inPixels = ImageUtils.getPixels(src);

        if (radius > 0) {
            // most of the time is spent here, so only
            // the blur manages its progress tracker
            BoxBlurFilter boxBlur = new BoxBlurFilter(filterName, radius, radius, 3);
            srcCopy = boxBlur.filter(srcCopy, srcCopy);
        }

        int[] outPixels = ImageUtils.getPixels(srcCopy);

        float a = 4 * amount;

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = inPixels[index];
                int r1 = (rgb1 >> 16) & 0xFF;
                int g1 = (rgb1 >> 8) & 0xFF;
                int b1 = rgb1 & 0xFF;

                int rgb2 = outPixels[index];
                int r2 = (rgb2 >> 16) & 0xFF;
                int g2 = (rgb2 >> 8) & 0xFF;
                int b2 = rgb2 & 0xFF;

                r1 = PixelUtils.max255((int) (r1 + a * r2));
                g1 = PixelUtils.max255((int) (g1 + a * g2));
                b1 = PixelUtils.max255((int) (b1 + a * b2));

                outPixels[index] = (rgb1 & 0xFF_00_00_00) | (r1 << 16) | (g1 << 8) | b1;
                index++;
            }
        }

        if (dst == null) {
            return srcCopy;
        }

        setRGB(dst, 0, 0, width, height, outPixels);

        return dst;
    }
}
