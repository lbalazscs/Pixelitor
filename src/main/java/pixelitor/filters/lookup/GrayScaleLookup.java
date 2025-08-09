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

import com.jhlabs.image.PixelUtils;

/**
 * A lookup table that maps adjustments for a single color channel.
 */
public class GrayScaleLookup {
    private static final int TABLE_SIZE = 256;
    private static final GrayScaleLookup IDENTITY = new GrayScaleLookup(0, 255, 0, 255);

    private final short[] lut = new short[TABLE_SIZE];

    /**
     * Creates a new lookup table by mapping an input range to an output range.
     */
    public GrayScaleLookup(int inputDark, int inputLight, int outputDark, int outputLight) {
        int inputRange = inputLight - inputDark;

        if (inputRange == 0) {
            // create a threshold effect at the specified value
            short darkValue = (short) PixelUtils.clamp(outputDark);
            short lightValue = (short) PixelUtils.clamp(outputLight);
            for (int i = 0; i < TABLE_SIZE; i++) {
                lut[i] = (i < inputDark) ? darkValue : lightValue;
            }
        } else {
            // create a linear mapping from the input range to the output range
            double multiplier = (double) (outputLight - outputDark) / inputRange;
            double offset = outputDark - multiplier * inputDark;
            for (int i = 0; i < TABLE_SIZE; i++) {
                lut[i] = (short) PixelUtils.clamp((int) (multiplier * i + offset));
            }
        }
    }

    /**
     * Maps an input value to its corresponding output value from the lookup table.
     */
    public short map(int input) {
        return lut[input];
    }

    public short[] getTable() {
        return lut;
    }

    /**
     * Returns a shared instance of an identity lookup table where output equals input.
     */
    public static GrayScaleLookup getIdentity() {
        return IDENTITY;
    }
}