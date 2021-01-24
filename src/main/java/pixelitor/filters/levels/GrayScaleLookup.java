/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
    private static final GrayScaleLookup IDENTITY = new GrayScaleLookup(
        0, 255, 0, 255);
    private final short[] lut = new short[256];

    public GrayScaleLookup(int inputDark, int inputLight,
                           int outputDark, int outputLight) {
        double multiplier;
        double constant;

        int inputDiff = inputLight - inputDark;
        if (inputDiff == 0) { // in Levels this should happen only if both are 0 or both are 255
            multiplier = 0;
            constant = 255 - inputDark;
        } else {
            multiplier = (outputLight - outputDark)
                / (double) inputDiff;
            constant = outputDark - multiplier * inputDark;
        }
        for (int i = 0; i < lut.length; i++) {
            lut[i] = (short) PixelUtils.clamp((int) (multiplier * i + constant));
        }
    }

    public short map(short input) {
        return lut[input];
    }

    public static GrayScaleLookup getIdentity() {
        return IDENTITY;
    }
}
