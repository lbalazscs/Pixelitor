/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.lookup.LookupFactory;
import pixelitor.utils.VisibleForTesting;

import java.awt.image.LookupTable;

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
                     GrayScaleLookup b,
                     GrayScaleLookup rg,
                     GrayScaleLookup rb,
                     GrayScaleLookup gb
    ) {
        allocateArrays();

        for (short i = 0; i < ARRAY_LENGTH; i++) {
            short rgbMapped = rgb.map(i);

            redLUT[i] = rgbMapped;
            redLUT[i] = r.map(redLUT[i]);
            redLUT[i] = rg.map(redLUT[i]);
            redLUT[i] = rb.map(redLUT[i]);

            greenLUT[i] = rgbMapped;
            greenLUT[i] = g.map(greenLUT[i]);
            greenLUT[i] = rg.map(greenLUT[i]);
            greenLUT[i] = gb.map(greenLUT[i]);

            blueLUT[i] = rgbMapped;
            blueLUT[i] = b.map(blueLUT[i]);
            blueLUT[i] = rb.map(blueLUT[i]);
            blueLUT[i] = gb.map(blueLUT[i]);
        }
    }

    private void allocateArrays() {
        redLUT = new short[ARRAY_LENGTH];
        greenLUT = new short[ARRAY_LENGTH];
        blueLUT = new short[ARRAY_LENGTH];
    }

    public LookupTable getLookupOp() {
        return LookupFactory.createLookupFrom3Arrays(redLUT, greenLUT, blueLUT);
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

    @VisibleForTesting
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

    @VisibleForTesting
    int mapRed(int input) {
        return redLUT[input];
    }

    @VisibleForTesting
    int mapGreen(int input) {
        return greenLUT[input];
    }

    @VisibleForTesting
    int mapBlue(int input) {
        return blueLUT[input];
    }
}
