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

import pixelitor.colors.Colors;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Edge detection via the Laplacian operator.
 *
 * @author Jerry Huxtable
 */
public class LaplaceFilter extends AbstractBufferedImageOp {
    public LaplaceFilter(String filterName) {
        super(filterName);
    }

    // converts each pixel to a grayscale brightness
    private static void brightness(int[] row) {
        for (int i = 0; i < row.length; i++) {
            int rgb = row[i];
            int r = rgb >> 16 & 0xFF;
            int g = rgb >> 8 & 0xFF;
            int b = rgb & 0xFF;
            row[i] = (r + g + b) / 3;
        }
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }
        int width = src.getWidth();
        int height = src.getHeight();

        if (width <= 1 || height <= 1) {
            // return a black image
            Colors.fillWith(Color.BLACK, dst);
            return dst;
        }

        pt = createProgressTracker(2 * height);

        int[] pixels = new int[width];

        // a sliding 3-row window over the image
        int[] prevRow = getRGB(src, 0, 0, width, 1, null);
        int[] currRow = getRGB(src, 0, 0, width, 1, null);
        int[] nextRow = null;

        brightness(prevRow);
        brightness(currRow);

        // first pass: compute Laplacian and gradient + store sign + edge strength
        for (int y = 0; y < height; y++) {
            if (y < height - 1) {
                nextRow = getRGB(src, 0, y + 1, width, 1, null);
                brightness(nextRow);
            }
            pixels[0] = pixels[width - 1] = 0xFF_00_00_00;//FIXME
            for (int x = 1; x < width - 1; x++) {
                // applies the 3×3 Laplacian kernel
                // (1  1  1
                //  1 -8  1
                //  1  1  1)
                // to every interior pixel
                int sum = prevRow[x - 1] + prevRow[x] + prevRow[x + 1] +
                    currRow[x - 1] - (8 * currRow[x]) + currRow[x + 1] +
                    nextRow[x - 1] + nextRow[x] + nextRow[x + 1];

                int center = currRow[x];

                // local pixel values around center pixel
                int left = currRow[x - 1];
                int right = currRow[x + 1];
                int top = prevRow[x];
                int bottom = nextRow[x];

                // estimate gradient magnitude: if neighbors differ
                // strongly from center => strong edge
                int max = Math.max(Math.max(left, top), Math.max(bottom, right));
                int min = Math.min(Math.min(left, top), Math.min(bottom, right));
                int gradient = Math.max(max - center, center - min) / 2;

                // encode the sign into the pixel value
                int r = sum > 0
                    ? gradient // one side of the edge: store 0-127
                    : (128 + gradient); // other side: store 128–255
                pixels[x] = r;
            }
            setRGB(dst, 0, y, width, 1, pixels);

            // rolling buffer: reuse the row arrays
            int[] t = prevRow;
            prevRow = currRow;
            currRow = nextRow;
            nextRow = t; // will be passed to getRGB as a reusable buffer

            pt.unitDone();
        }

        prevRow = getRGB(dst, 0, 0, width, 1, prevRow);
        currRow = getRGB(dst, 0, 0, width, 1, currRow);

        // second pass: detect zero-crossings (where sign flips => edges)
        for (int y = 0; y < height; y++) {
            if (y < height - 1) {
                nextRow = getRGB(dst, 0, y + 1, width, 1, nextRow);
            }
            pixels[0] = pixels[width - 1] = 0xFF_00_00_00;//FIXME
            for (int x = 1; x < width - 1; x++) {
                boolean hasNeighborAbove128 =
                    (prevRow[x - 1] > 128) || (prevRow[x] > 128) || (prevRow[x + 1] > 128) ||
                    (currRow[x - 1] > 128) || (currRow[x + 1] > 128) ||
                    (nextRow[x - 1] > 128) || (nextRow[x] > 128) || (nextRow[x + 1] > 128);

                // detect sign change: keeps the pixel if it's on
                // one side and a neighbor is on the opposite side
                int v = currRow[x];
                v = (v < 128 && hasNeighborAbove128) ? v : 0;
                pixels[x] = 0xFF_00_00_00 | (v << 16) | (v << 8) | v;
            }
            setRGB(dst, 0, y, width, 1, pixels);
            int[] t = prevRow;
            prevRow = currRow;
            currRow = nextRow;
            nextRow = t;

            pt.unitDone();
        }

        finishProgressTracker();

        return dst;
    }
}
