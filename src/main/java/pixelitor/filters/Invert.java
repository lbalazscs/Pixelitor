/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters;

import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Invert filter
 */
public class Invert extends Filter {

    public Invert() {
        super("Invert");
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        invertImage(src, dest);

        return dest;
    }

    public static void invertImage(BufferedImage src, BufferedImage dest) {
        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        boolean simple = !src.isAlphaPremultiplied();

        for (int i = 0; i < destData.length; i++) {
            int srcPixel = srcData[i];
//            int alpha = srcPixel & 0xFF000000;
            int a = (srcPixel >>> 24) & 0xFF;

            if (a == 255 || simple) {
                destData[i] = srcPixel ^ 0x00FFFFFF;  // invert the r, g, b values
            } else if (a == 0) {
                destData[i] = 0;
            } else {
                int r = (srcPixel >>> 16) & 0xFF;
                int g = (srcPixel >>> 8) & 0xFF;
                int b = srcPixel & 0xFF;

                // unpremultiply
                float f = 255.0f / a;
                int ur = (int) (r * f);
                int ug = (int) (g * f);
                int ub = (int) (b * f);

                // TODO these checks shouldn't be necessary
                if (ur > 255) {
                    ur = 255;
                }
                if (ug > 255) {
                    ug = 255;
                }
                if (ub > 255) {
                    ub = 255;
                }

                // invert
                ur = 255 - ur;
                ug = 255 - ug;
                ub = 255 - ub;

                // premultiply
                float f2 = a * (1.0f / 255.0f);
                r = (int) (ur * f2);
                g = (int) (ug * f2);
                b = (int) (ub * f2);

                r = ImageUtils.limitTo8Bits(r);
                g = ImageUtils.limitTo8Bits(g);
                b = ImageUtils.limitTo8Bits(b);

                destData[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
    }

    @Override
    public void randomizeSettings() {
        // nothing to randomize
    }
}