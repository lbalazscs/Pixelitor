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
import java.awt.image.ColorModel;

/**
 * A filter which acts as a superclass for filters which need to have the whole image in memory
 * to do their stuff.
 */
public abstract class WholeImageFilter extends AbstractBufferedImageOp {
    /**
     * Construct a WholeImageFilter.
     */
    protected WholeImageFilter(String filterName) {
        super(filterName);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        if (dst == null) {
            ColorModel dstCM = src.getColorModel();
            dst = new BufferedImage(dstCM, dstCM
                    .createCompatibleWritableRaster(width, height), dstCM
                    .isAlphaPremultiplied(), null);
        }

        int[] inPixels = getRGB(src, 0, 0, width, height, null);
        inPixels = filterPixels(width, height, inPixels);
        setRGB(dst, 0, 0, width, height, inPixels);

        return dst;
    }

    /**
     * Actually filter the pixels.
     *
     * @param width    the image width
     * @param height   the image height
     * @param inPixels the image pixels
     * @return the output pixels
     */
    protected abstract int[] filterPixels(int width, int height, int[] inPixels);
}

