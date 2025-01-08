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

package pixelitor.filters;

import pixelitor.filters.util.FilterAction;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Used when colors of all pixels have to be changed
 * uniformly and independently of each other
 */
public interface RGBPixelOp {
    /**
     * Computes a new color from the given channel values
     * and returns it as a packed ARGB integer.
     */
    int changeRGB(int a, int r, int g, int b);

    default FilterAction toFilterAction(String name) {
        return new FilterAction(name, () -> new ExtractChannelFilter(this)).noGUI();
    }

    default BufferedImage filter(BufferedImage src,
                                 BufferedImage dest) {
        int[] srcPixels = ImageUtils.getPixels(src);
        int[] destPixels = ImageUtils.getPixels(dest);

        for (int i = 0; i < srcPixels.length; i++) {
            int rgb = srcPixels[i];

            int a = (rgb >>> 24) & 0xFF;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

            destPixels[i] = changeRGB(a, r, g, b);
        }

        return dest;
    }
}
