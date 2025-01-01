/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] pixels = new int[horBlockSize * verBlockSize];
        int[] halfPixels = new int[horBlockSize * verBlockSize / 2];

        int imgWidth = src.getWidth();
        int imgHeight = src.getHeight();
        int verticalBlockNo = 0;
        for (int y = 0; y < imgHeight; y += verBlockSize) {
            verticalBlockNo++;

            int hOffset = 0;  // horizontal offset for alternate rows
            if (verticalBlockNo % 2 == 0) {
                hOffset = horBlockSize / 2;

                // process the left half-block at the start of the row
                replaceWithAverage(src, dst, imgWidth, imgHeight,
                    halfPixels, 0, y, horBlockSize / 2, verBlockSize);
            }

            // process the horizontal blocks in the row
            for (int x = hOffset; x < imgWidth; x += horBlockSize) {
                replaceWithAverage(src, dst, imgWidth, imgHeight,
                    pixels, x, y, horBlockSize, verBlockSize);
            }
        }

        return dst;
    }

    // replaces a block with its average color
    private static void replaceWithAverage(BufferedImage src, BufferedImage dst,
                                           int width, int height,
                                           int[] pixels, int x, int y,
                                           int hSize, int vSize) {
        int w = Math.min(hSize, width - x);
        int h = Math.min(vSize, height - y);

        getRGB(src, x, y, w, h, pixels);
        int r = 0, g = 0, b = 0;
        int rgb;
        int i = 0;

        // accumulate individual channel values
        for (int by = 0; by < h; by++) {
            for (int bx = 0; bx < w; bx++) {
                rgb = pixels[i];
                r += (rgb >> 16) & 0xff;
                g += (rgb >> 8) & 0xff;
                b += rgb & 0xff;
                i++;
            }
        }

        // calculate the average color of the block
        int t = w * h;
        rgb = ((r / t) << 16) | ((g / t) << 8) | (b / t);
        i = 0;

        // set all pixels in the block to the average color,
        // preserving the alpha channel
        for (int by = 0; by < h; by++) {
            for (int bx = 0; bx < w; bx++) {
                pixels[i] = (pixels[i] & 0xFF_00_00_00) | rgb;
                i++;
            }
        }
        setRGB(dst, x, y, w, h, pixels);
    }
}