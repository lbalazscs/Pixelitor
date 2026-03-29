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
 * A filter which subtracts Gaussian blur from an image, sharpening it.
 *
 * @author Jerry Huxtable
 */
public class UnsharpFilter extends GaussianFilter {
    private float amount = 0.5f;
    private int threshold = 1;

    public UnsharpFilter(String filterName) {
        super(filterName);
    }

    /**
     * Sets the threshold value.
     *
     * @param threshold the threshold value
     */
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    /**
     * Sets the amount of sharpening.
     *
     * @param amount the amount (in the range [0, 1])
     */
    public void setAmount(float amount) {
        this.amount = amount;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        pt = createProgressTracker(width + height);

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] inPixels = new int[width * height];
        //        src.getRGB(0, 0, width, height, inPixels, 0, width);
        getRGB(src, 0, 0, width, height, inPixels);

        int[] outPixels = new int[width * height];
        if (radius > 0) {
            convolveAndTranspose(kernel, inPixels, outPixels, width, height, premultiplyAlpha, false, pt);
            convolveAndTranspose(kernel, outPixels, inPixels, height, width, false, premultiplyAlpha, pt);
        }

        // src.getRGB(0, 0, width, height, outPixels, 0, width);
        // TODO system.arraycopy would be even faster
        getRGB(src, 0, 0, width, height, outPixels);

        float a = 4 * amount;

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = outPixels[index];
                int r1 = (rgb1 >> 16) & 0xFF;
                int g1 = (rgb1 >> 8) & 0xFF;
                int b1 = rgb1 & 0xFF;

                int rgb2 = inPixels[index];
                int r2 = (rgb2 >> 16) & 0xFF;
                int g2 = (rgb2 >> 8) & 0xFF;
                int b2 = rgb2 & 0xFF;

                if (Math.abs(r1 - r2) >= threshold) {
                    r1 = PixelUtils.clamp((int) ((a + 1) * (r1 - r2) + r2));
                }
                if (Math.abs(g1 - g2) >= threshold) {
                    g1 = PixelUtils.clamp((int) ((a + 1) * (g1 - g2) + g2));
                }
                if (Math.abs(b1 - b2) >= threshold) {
                    b1 = PixelUtils.clamp((int) ((a + 1) * (b1 - b2) + b2));
                }

                inPixels[index] = (rgb1 & 0xFF_00_00_00) | (r1 << 16) | (g1 << 8) | b1;
                index++;
            }
        }

        dst.setRGB(0, 0, width, height, inPixels, 0, width);

        finishProgressTracker();

        return dst;
    }
}
