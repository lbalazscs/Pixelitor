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

package pixelitor.filters.lookup;

import pixelitor.filters.Filter;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * A filer that extracts the luminosity channel.
 */
public class Luminosity extends Filter {
    public static final String NAME = "Luminosity";

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int[] srcPixels = ImageUtils.getPixels(src);
        int[] destPixels = ImageUtils.getPixels(dest);

        for (int i = 0; i < destPixels.length; i++) {
            int rgb = srcPixels[i];
            int alpha = rgb & 0xFF_00_00_00;
            if (alpha == 0) {
                destPixels[i] = 0;
            } else {
                int lum = (int) LuminanceLookup.from(rgb);
                destPixels[i] = alpha | lum << 16 | lum << 8 | lum;
            }
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}