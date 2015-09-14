/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.filters.levels;

import com.jhlabs.image.PixelUtils;

/**
 * A lookup table of 256 elements,
 * describing the adjustments made to a single channel
 */
public class GrayScaleLookup {
    private static final GrayScaleLookup DEFAULT = new GrayScaleLookup(0, 255, 0, 255);
    private final short[] mapping = new short[256];

    public GrayScaleLookup(int inputBlackValue, int inputWhiteValue,
                           int outputBlackValue, int outputWhiteValue) {
        for (int i = 0; i < mapping.length; i++) {
            double multiplier = ((double) (outputWhiteValue - outputBlackValue)) / ((double) (inputWhiteValue - inputBlackValue));
            double constant = (outputBlackValue) - (multiplier * inputBlackValue);
            mapping[i] = (short) PixelUtils.clamp((int) ((multiplier * i) + constant));
        }
    }

    public short mapValue(short input) {
        return mapping[input];
    }

    public static GrayScaleLookup getDefault() {
        return DEFAULT;
    }
}
