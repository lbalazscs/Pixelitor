/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import java.awt.image.ShortLookupTable;
import java.util.Arrays;

/**
 * Lookup tables for the red, green, and blue color channels.
 */
public class RGBLookup {
    private static final int LUT_SIZE = 256;

    private short[] redLut;
    private short[] greenLut;
    private short[] blueLut;

    /**
     * Creates a new {@link RGBLookup} from existing lookup tables.
     */
    public RGBLookup(short[] redLut, short[] greenLut, short[] blueLut) {
        this.redLut = redLut;
        this.greenLut = greenLut;
        this.blueLut = blueLut;
    }

    /**
     * Creates a new {@link RGBLookup} by combining a base lookup with channel-specific lookups.
     */
    public RGBLookup(ChannelLookup base,
                     ChannelLookup r,
                     ChannelLookup g,
                     ChannelLookup b) {
        allocateArrays();

        for (short i = 0; i < LUT_SIZE; i++) {
            // apply the base lookup first, then the channel-specific one
            short baseValue = base.map(i);
            redLut[i] = r.map(baseValue);
            greenLut[i] = g.map(baseValue);
            blueLut[i] = b.map(baseValue);
        }
    }

    private void allocateArrays() {
        redLut = new short[LUT_SIZE];
        greenLut = new short[LUT_SIZE];
        blueLut = new short[LUT_SIZE];
    }

    /**
     * Returns a {@link FastLookupOp} representation of this RGBLookup.
     */
    public FastLookupOp asFastLookupOp() {
        return new FastLookupOp(new ShortLookupTable(0,
            new short[][]{redLut, greenLut, blueLut}));
    }

    /**
     * Creates an {@link RGBLookup} that performs a posterize effect.
     */
    public static RGBLookup createForPosterize(int numRedLevels, int numGreenLevels, int numBlueLevels) {
        short[] redLUT = createPosterizeLut(numRedLevels);
        short[] greenLUT = createPosterizeLut(numGreenLevels);
        short[] blueLUT = createPosterizeLut(numBlueLevels);

        return new RGBLookup(redLUT, greenLUT, blueLUT);
    }

    private static short[] createPosterizeLut(int numLevels) {
        short[] lut = new short[LUT_SIZE];
        if (numLevels <= 1) {
            // for a single level, map all colors to the middle value
            Arrays.fill(lut, (short) (LUT_SIZE / 2));
        } else {
            for (int i = 0; i < LUT_SIZE; i++) {
                // map input value i (0-255) to a discrete quantization level
                int level = numLevels * i / LUT_SIZE;
                // map the quantization level back to an output value (0-255)
                int quantizedValue = (LUT_SIZE - 1) * level / (numLevels - 1);
                lut[i] = (short) quantizedValue;
            }
        }
        return lut;
    }

    /**
     * Returns the red channel lookup table array.
     */
    public short[] getRedLut() {
        return redLut;
    }

    /**
     * Returns the green channel lookup table array.
     */
    public short[] getGreenLut() {
        return greenLut;
    }

    /**
     * Returns the blue channel lookup table array.
     */
    public short[] getBlueLut() {
        return blueLut;
    }
}
