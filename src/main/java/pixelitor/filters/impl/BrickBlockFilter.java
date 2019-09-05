/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.jhlabsproxies.JHPixelate;

import java.awt.image.BufferedImage;

/**
 * One of the implementations of the {@link JHPixelate} filter.
 * Pixelates in brick-style.
 */
public class BrickBlockFilter extends AbstractBufferedImageOp {
    private int horBlockSize = 10;
    private int verBlockSize = 10;

    public BrickBlockFilter(String filterName) {
        super(filterName);
    }

    public void setHorBlockSize(int horBlockSize) {
        this.horBlockSize = horBlockSize;
    }

    public void setVerBlockSize(int verBlockSize) {
        this.verBlockSize = verBlockSize;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] pixels = new int[horBlockSize * verBlockSize];
        int[] smallPixels = new int[horBlockSize * verBlockSize / 2];
        int verticalCount = 0;

        for (int y = 0; y < height; y += verBlockSize) {
            verticalCount++;

            int hShift = 0;
            if (verticalCount % 2 == 0) {
                hShift = horBlockSize / 2;
                replaceWithAverage(src, dst, width, height,
                        smallPixels, 0, y, horBlockSize / 2, verBlockSize);
            }

            for (int x = hShift; x < width; x += horBlockSize) {
                replaceWithAverage(src, dst, width, height,
                        pixels, x, y, horBlockSize, verBlockSize);
            }
        }

        return dst;
    }

    private static void replaceWithAverage(BufferedImage src, BufferedImage dst,
                                           int width, int height,
                                           int[] pixels, int x, int y,
                                           int hSize, int vSize) {
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