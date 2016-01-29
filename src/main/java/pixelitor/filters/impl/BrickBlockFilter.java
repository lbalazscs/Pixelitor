/*
 * Copyright 2016 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters.impl;

import com.jhlabs.image.AbstractBufferedImageOp;

import java.awt.image.BufferedImage;

/**
 * Pixelates in brick-style.
 */
public class BrickBlockFilter extends AbstractBufferedImageOp {

    private int horizontalBlockSize = 10;
    private int verticalBlockSize = 10;

    public BrickBlockFilter(String filterName) {
        super(filterName);
    }

    public void setHorizontalBlockSize(int horizontalBlockSize) {
        this.horizontalBlockSize = horizontalBlockSize;
    }

    public void setVerticalBlockSize(int verticalBlockSize) {
        this.verticalBlockSize = verticalBlockSize;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] pixels = new int[horizontalBlockSize * verticalBlockSize];
        int[] smallPixels = new int[horizontalBlockSize * verticalBlockSize / 2];
        int verticalCount = 0;

        for (int y = 0; y < height; y += verticalBlockSize) {
            verticalCount++;

            int hShift = 0;
            if ((verticalCount % 2) == 0) {
                hShift = (horizontalBlockSize / 2);

                replaceWithAverage(src, dst, width, height, smallPixels, y, 0, horizontalBlockSize / 2, verticalBlockSize);
            }

            for (int x = hShift; x < width; x += horizontalBlockSize) {
                replaceWithAverage(src, dst, width, height, pixels, y, x, horizontalBlockSize, verticalBlockSize);
            }
        }

        return dst;
    }

    private static void replaceWithAverage(BufferedImage src, BufferedImage dst, int width, int height, int[] pixels, int y, int x, int hSize, int vSize) {
        int w = Math.min(hSize, width - x);
        int h = Math.min(vSize, height - y);
        int t = w * h;

        getRGB(src, x, y, w, h, pixels);
        int r = 0, g = 0, b = 0;
        int argb;
        int i = 0;
        for (int by = 0; by < h; by++) {
            for (int bx = 0; bx < w; bx++) {
                argb = pixels[i];
                r += (argb >> 16) & 0xff;
                g += (argb >> 8) & 0xff;
                b += argb & 0xff;
                i++;
            }
        }
        argb = ((r / t) << 16) | ((g / t) << 8) | (b / t);
        i = 0;
        for (int by = 0; by < h; by++) {
            for (int bx = 0; bx < w; bx++) {
                pixels[i] = (pixels[i] & 0xff000000) | argb;
                i++;
            }
        }
        setRGB(dst, x, y, w, h, pixels);
    }
}