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

import com.jhlabs.image.Curve;
import com.jhlabs.image.PixelUtils;

import java.util.Objects;

/**
 * A lookup table that maps adjustments for a single color channel.
 */
public class ChannelLookup {
    static final int LUT_SIZE = 256;

    /**
     * Controls whether input values outside [inputDark, inputLight] are clipped.
     * false = the line through (inputDark, outputDark) and (inputLight, outputLight)
     * is extended beyond the input range and then clamped to 0..255 (current behavior);
     * true = input values outside [inputDark, inputLight] are clipped first,
     * so they map to outputDark / outputLight.
     * TODO false: old behavior, true: PS-style Levels
     */
    private static final boolean CLIP_INPUT_RANGE = false;

    private static final ChannelLookup IDENTITY = new ChannelLookup(0, 255, 0, 255);

    private final short[] lut;

    private ChannelLookup(short[] lut) {
        assert lut != null && lut.length == LUT_SIZE;
        this.lut = lut;
    }

    /**
     * Creates a new lookup table by mapping an input range to an output range.
     */
    public ChannelLookup(int inputDark, int inputLight, int outputDark, int outputLight) {
        this(createLevelsTable(inputDark, inputLight, outputDark, outputLight, CLIP_INPUT_RANGE));
    }

    /**
     * Creates a lookup table by mapping an input range to an output range.
     */
    public static ChannelLookup ofLevels(int inputDark, int inputLight, int outputDark, int outputLight) {
        return ofLevels(inputDark, inputLight, outputDark, outputLight, CLIP_INPUT_RANGE);
    }

    /**
     * Creates a lookup table by mapping an input range to an output range,
     * with an explicit clipInputRange setting.
     */
    static ChannelLookup ofLevels(int inputDark, int inputLight, int outputDark, int outputLight, boolean clipInputRange) {
        return new ChannelLookup(createLevelsTable(inputDark, inputLight, outputDark, outputLight, clipInputRange));
    }

    /**
     * Creates a lookup table from a 256-entry integer table.
     * Clamps values to 0..255.
     */
    public static ChannelLookup ofTable(int[] table) {
        Objects.requireNonNull(table, "table must not be null");
        if (table.length != LUT_SIZE) {
            throw new IllegalArgumentException(
                "table length must be " + LUT_SIZE + ", but was " + table.length);
        }

        short[] lut = new short[LUT_SIZE];
        for (int i = 0; i < LUT_SIZE; i++) {
            lut[i] = (short) PixelUtils.clamp(table[i]);
        }
        return new ChannelLookup(lut);
    }

    /**
     * Creates a lookup table from a spline curve.
     */
    public static ChannelLookup ofCurve(Curve curve) {
        Objects.requireNonNull(curve, "curve must not be null");
        return ofTable(curve.makeTable());
    }

    /**
     * Fills a 256-entry lookup table for the specified levels parameters.
     */
    static short[] createLevelsTable(int inputDark, int inputLight, int outputDark, int outputLight, boolean clipInputRange) {
        short[] lut = new short[LUT_SIZE];
        int inputRange = inputLight - inputDark;

        if (inputRange == 0) {
            // create a threshold effect at the value inputDark (== inputLight)
            short darkValue = (short) PixelUtils.clamp(outputDark);
            short lightValue = (short) PixelUtils.clamp(outputLight);
            for (int i = 0; i < LUT_SIZE; i++) {
                lut[i] = (i < inputDark) ? darkValue : lightValue;
            }
        } else {
            // create a linear mapping from the input range to the output range
            double multiplier = (double) (outputLight - outputDark) / inputRange;
            double offset = outputDark - multiplier * inputDark;
            int minInput = Math.min(inputDark, inputLight);
            int maxInput = Math.max(inputDark, inputLight);
            for (int i = 0; i < LUT_SIZE; i++) {
                int in = clipInputRange ? Math.clamp(i, minInput, maxInput) : i;
                lut[i] = (short) PixelUtils.clamp((int) Math.round(multiplier * in + offset));
            }
        }

        return lut;
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
    public static ChannelLookup getIdentity() {
        return IDENTITY;
    }
}
