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

package pixelitor.filters.lookup;

import pixelitor.filters.Filter;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Extracts the luminosity channel
 */
public class Luminosity extends Filter {
    public static final String NAME = "Luminosity";

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        for (int i = 0; i < destData.length; i++) {
            int rgb = srcData[i];
            int alpha = rgb & 0xFF000000;
            if (alpha == 0) {
                destData[i] = 0;
            } else {
                int lum = LuminanceLookup.getLuminosity(rgb);
                destData[i] = alpha | (lum << 16) | (lum << 8) | lum;
            }
        }

        return dest;
    }

    @Override
    public void randomizeSettings() {
        // nothing to randomize
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}