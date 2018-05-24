/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

/**
 * Fast calculations for pixel luminances
 */
public final class LuminanceLookup {
    private static final int TABLE_SIZE = 256;

    private static final float RED_CONTRIBUTION = 0.2126f;
    private static final float GREEN_CONTRIBUTION = 0.7152f;
    private static final float BLUE_CONTRIBUTION = 0.0722f;

    private static final float[] redLumTable = new float[TABLE_SIZE];
    private static final float[] greenLumTable = new float[TABLE_SIZE];
    private static final float[] blueLumTable = new float[TABLE_SIZE];

    // the multiplications are done only once
    static {
        for (int i = 0; i < TABLE_SIZE; i++) {
            redLumTable[i] = i * RED_CONTRIBUTION;
            greenLumTable[i] = i * GREEN_CONTRIBUTION;
            blueLumTable[i] = i * BLUE_CONTRIBUTION;
        }
    }

    private LuminanceLookup() {
    }

    public static int getLuminosity(int r, int g, int b) {
        return (int) (redLumTable[r] + greenLumTable[g] + blueLumTable[b]);
    }

    public static int getLuminosity(int rgb) {
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = (rgb) & 0xFF;
        return (int) (redLumTable[r] + greenLumTable[g] + blueLumTable[b]);
    }
}