/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

    public GrayScaleLookup(int inputDarkValue, int inputLightValue,
                           int outputDarkValue, int outputLightValue) {
        double multiplier;
        double constant;

        int inputDiff = inputLightValue - inputDarkValue;
        if (inputDiff == 0) { // in Levels this should happen only if both are 0 or both are 255
            multiplier = 0;
            constant = 255 - inputDarkValue;
        } else {
            multiplier = (double) (outputLightValue - outputDarkValue)
                    / (double) inputDiff;
            constant = outputDarkValue - multiplier * inputDarkValue;
        }
        for (int i = 0; i < lut.length; i++) {
            lut[i] = (short) PixelUtils.clamp((int) (multiplier * i + constant));
        }
//        System.out.printf("GrayScaleLookup:: LUT: " +
//                        "0 -> %d, 1 -> %d, 128 -> %d, 254 -> %d, 255 -> %d%n",
//                lut[0], lut[1], lut[128], lut[254], lut[255]);
    }

    public short map(short input) {
        return lut[input];
    }

    public static GrayScaleLookup getIdentity() {
        return IDENTITY;
    }
}
