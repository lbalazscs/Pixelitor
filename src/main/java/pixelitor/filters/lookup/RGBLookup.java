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
 * A lookup table for each of the red, green, and blue color channels.
 */
public class RGBLookup {
    private static final int LUT_SIZE = 256;

    private short[] redLUT;
    private short[] greenLUT;
    private short[] blueLUT;

    /**
     * Creates a new {@link RGBLookup} from existing lookup tables.
     */
    public RGBLookup(short[] redLUT, short[] greenLUT, short[] blueLUT) {
        this.redLUT = redLUT;
        this.greenLUT = greenLUT;
        this.blueLUT = blueLUT;
    }

    /**
     * Creates a new {@link RGBLookup} by applying a single grayscale lookup to all channels.
     */
    public RGBLookup(GrayScaleLookup gray) {
        allocateArrays();

        for (short i = 0; i < LUT_SIZE; i++) {
            short value = gray.map(i);
            redLUT[i] = value;
            greenLUT[i] = value;
            blueLUT[i] = value;
        }
    }

    /**
     * Creates a new {@link RGBLookup} by combining a base lookup with channel-specific lookups.
     */
    public RGBLookup(GrayScaleLookup base,
                     GrayScaleLookup r,
                     GrayScaleLookup g,
                     GrayScaleLookup b) {
        allocateArrays();

        for (short i = 0; i < LUT_SIZE; i++) {
            // apply the base lookup first, then the channel-specific one
            short baseValue = base.map(i);
            redLUT[i] = r.map(baseValue);
            greenLUT[i] = g.map(baseValue);
            blueLUT[i] = b.map(baseValue);
        }
    }

    private void allocateArrays() {
        redLUT = new short[LUT_SIZE];
        greenLUT = new short[LUT_SIZE];
        blueLUT = new short[LUT_SIZE];
    }

    /**
     * Returns a {@link FastLookupOp} representation of this RGBLookup.
     */
    public FastLookupOp asFastLookupOp() {
        short[][] maps = new short[3][];
        maps[0] = redLUT;
        maps[1] = greenLUT;
        maps[2] = blueLUT;

        return new FastLookupOp(new ShortLookupTable(0, maps));
    }

    /**
     * Creates an {@link RGBLookup} that performs a posterize effect.
     */
    public static RGBLookup createForPosterize(int numRedLevels, int numGreenLevels, int numBlueLevels) {
        short[] redLUT = createPosterizeLUT(numRedLevels);
        short[] greenLUT = createPosterizeLUT(numGreenLevels);
        short[] blueLUT = createPosterizeLUT(numBlueLevels);

        return new RGBLookup(redLUT, greenLUT, blueLUT);
    }

    private static short[] createPosterizeLUT(int numLevels) {
        short[] lut = new short[LUT_SIZE];
        if (numLevels <= 1) {
            // for a single level, map all colors to the middle value
            Arrays.fill(lut, (short) (LUT_SIZE / 2));
        } else {
            for (int i = 0; i < LUT_SIZE; i++) {
                // map input value i (0-255) to a discrete quantization level
                int level = numLevels * i / LUT_SIZE;
                // map the quantization level back to an output value (0-255)
                int mapping = (LUT_SIZE - 1) * level / (numLevels - 1);
                lut[i] = (short) mapping;
            }
        }
        return lut;
    }

    /**
     * Applies the lookup tables to a packed RGB integer value.
     */
    public int mapRgb(int rgb) {
        // alpha channel is preserved
        int a = rgb >>> 24;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;

        r = redLUT[r];
        g = greenLUT[g];
        b = blueLUT[b];

        return a << 24 | r << 16 | g << 8 | b;
    }

    /**
     * Maps a single red channel value.
     */
    public int mapRed(int input) {
        return redLUT[input];
    }

    /**
     * Maps a single green channel value.
     */
    public int mapGreen(int input) {
        return greenLUT[input];
    }

    /**
     * Maps a single blue channel value.
     */
    public int mapBlue(int input) {
        return blueLUT[input];
    }

    /**
     * Returns the red channel lookup table array.
     */
    public short[] getRedLUT() {
        return redLUT;
    }

    /**
     * Returns the green channel lookup table array.
     */
    public short[] getGreenLUT() {
        return greenLUT;
    }

    /**
     * Returns the blue channel lookup table array.
     */
    public short[] getBlueLUT() {
        return blueLUT;
    }
}
