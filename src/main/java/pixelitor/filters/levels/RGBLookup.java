/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import java.awt.image.LookupTable;
import java.awt.image.ShortLookupTable;

/**
 * Manages 3 lookup tables, corresponding to the
 * R, G, B channels of a pixel-by-pixel adjustment
 */
public class RGBLookup {
    private static final int ARRAY_LENGTH = 256;

    private short[] redLUT;
    private short[] greenLUT;
    private short[] blueLUT;

    public RGBLookup() {
        allocateArrays();
    }

    public RGBLookup(short[] redLUT, short[] greenLUT, short[] blueLUT) {
        this.redLUT = redLUT;
        this.greenLUT = greenLUT;
        this.blueLUT = blueLUT;
    }

    public RGBLookup(GrayScaleLookup rgb) {
        allocateArrays();

        for (short i = 0; i < ARRAY_LENGTH; i++) {
            short val = rgb.map(i);
            redLUT[i] = val;
            greenLUT[i] = val;
            blueLUT[i] = val;
        }
    }

    public RGBLookup(GrayScaleLookup rgb,
                     GrayScaleLookup r,
                     GrayScaleLookup g,
                     GrayScaleLookup b
    ) {
        allocateArrays();

        for (short i = 0; i < ARRAY_LENGTH; i++) {
            short rgbMapped = rgb.map(i);

            redLUT[i] = rgbMapped;
            redLUT[i] = r.map(redLUT[i]);

            greenLUT[i] = rgbMapped;
            greenLUT[i] = g.map(greenLUT[i]);

            blueLUT[i] = rgbMapped;
            blueLUT[i] = b.map(blueLUT[i]);
        }
    }

    private void allocateArrays() {
        redLUT = new short[ARRAY_LENGTH];
        greenLUT = new short[ARRAY_LENGTH];
        blueLUT = new short[ARRAY_LENGTH];
    }

    public LookupTable getLookupOp() {
        return createLUT(redLUT, greenLUT, blueLUT);
    }

    private static LookupTable createLUT(
        short[] redMap, short[] greenMap, short[] blueMap) {

        short[][] maps = new short[3][256];
        maps[0] = redMap;
        maps[1] = greenMap;
        maps[2] = blueMap;
        return new ShortLookupTable(0, maps);
    }

    public void initFromPosterize(int numRedLevels, int numGreenLevels, int numBlueLevels) {
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            int mapping = (ARRAY_LENGTH - 1) * (numRedLevels * i / ARRAY_LENGTH) / (numRedLevels - 1);
            redLUT[i] = (short) mapping;
        }
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            int mapping = (ARRAY_LENGTH - 1) * (numGreenLevels * i / ARRAY_LENGTH) / (numGreenLevels - 1);
            greenLUT[i] = (short) mapping;
        }
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            int mapping = (ARRAY_LENGTH - 1) * (numBlueLevels * i / ARRAY_LENGTH) / (numBlueLevels - 1);
            blueLUT[i] = (short) mapping;
        }
    }

    int mapRGBValue(int rgb) {
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;

        r = redLUT[r];
        g = greenLUT[g];
        b = blueLUT[b];

        rgb = a << 24 | r << 16 | g << 8 | b;

        return rgb;
    }

    int mapRed(int input) {
        return redLUT[input];
    }

    int mapGreen(int input) {
        return greenLUT[input];
    }

    int mapBlue(int input) {
        return blueLUT[input];
    }
}
