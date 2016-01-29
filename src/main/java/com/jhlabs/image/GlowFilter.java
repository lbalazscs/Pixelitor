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

    private float amount = 0.5f;

    /**
     * The blur radius.
     */
    protected float radius;

    public GlowFilter(String filterName) {
        super(filterName);
        radius = 2;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * Set the amount of glow.
     *
     * @param amount the amount
     * @min-value 0
     * @max-value 1
     * @see #getAmount
     */
    public void setAmount(float amount) {
        this.amount = amount;
    }

    /**
     * Get the amount of glow.
     *
     * @return the amount
     * @see #setAmount
     */
    public float getAmount() {
        return amount;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

//        if (dst == null) {
//            dst = createCompatibleDestImage(src, null);
//        }

        BufferedImage srcCopy = ImageUtils.copyImage(src);

        int[] inPixels = ImageUtils.getPixelsAsArray(src);


        if(radius > 0) {
            BoxBlurFilter boxBlur = new BoxBlurFilter(radius, radius, 3, filterName);
            srcCopy = boxBlur.filter(srcCopy, srcCopy);
        }

        int[] outPixels = ImageUtils.getPixelsAsArray(srcCopy);

        float a = 4 * amount;

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = inPixels[index];
                int r1 = (rgb1 >> 16) & 0xff;
                int g1 = (rgb1 >> 8) & 0xff;
                int b1 = rgb1 & 0xff;

                int rgb2 = outPixels[index];
                int r2 = (rgb2 >> 16) & 0xff;
                int g2 = (rgb2 >> 8) & 0xff;
                int b2 = rgb2 & 0xff;

                r1 = PixelUtils.max255((int) (r1 + a * r2));
                g1 = PixelUtils.max255((int) (g1 + a * g2));
                b1 = PixelUtils.max255((int) (b1 + a * b2));

                outPixels[index] = (rgb1 & 0xff000000) | (r1 << 16) | (g1 << 8) | b1;
                index++;
            }
        }

//        dst.setRGB(0, 0, width, height, inPixels, 0, width);
        setRGB(dst, 0, 0, width, height, outPixels);

        return dst;
    }

    public String toString() {
		return "Blur/Glow...";
	}
}
