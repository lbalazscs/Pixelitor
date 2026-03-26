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
 * A Filter to pixellate images.
 */
public class BlockFilter extends AbstractBufferedImageOp {
    private final int blockSize;

    /**
     * Constructs a BlockFilter.
     *
     * @param filterName the name of the filter
     * @param blockSize the number of pixels along each block edge
     */
    public BlockFilter(String filterName, int blockSize) {
        super(filterName);
        this.blockSize = blockSize;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        // pixel data for a single block
        int[] pixels = new int[blockSize * blockSize];

        for (int y = 0; y < height; y += blockSize) {
            for (int x = 0; x < width; x += blockSize) {
                int w = Math.min(blockSize, width - x);
                int h = Math.min(blockSize, height - y);
                getRGB(src, x, y, w, h, pixels);
                int r = 0, g = 0, b = 0;
                int argb;
                int i = 0;

                // accumulate individual channel values
                for (int by = 0; by < h; by++) {
                    for (int bx = 0; bx < w; bx++) {
                        argb = pixels[i];
                        r += (argb >> 16) & 0xFF;
                        g += (argb >> 8) & 0xFF;
                        b += argb & 0xFF;
                        i++;
                    }
                }

                // calculate the average color of the block
                int t = w * h;
                argb = ((r / t) << 16) | ((g / t) << 8) | (b / t);

                // set all pixels in the block to the average color,
                // preserving the alpha channel
                i = 0;
                for (int by = 0; by < h; by++) {
                    for (int bx = 0; bx < w; bx++) {
                        pixels[i] = (pixels[i] & 0xFF_00_00_00) | argb;
                        i++;
                    }
                }
                setRGB(dst, x, y, w, h, pixels);
            }
        }

        return dst;
    }
}
