/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters.levels;

import pixelitor.filters.lookup.LookupFactory;

import java.awt.image.LookupTable;

/**
 * Manages 3 lookup tables, corresponding to the
 * R, G, B channels of a pixel-by-pixel adjustment
 */
public class RGBLookup {
    private static final int ARRAY_LENGTH = 256;

    private short[] redMap;
    private short[] greenMap;
    private short[] blueMap;

    public RGBLookup() {
        allocateArrays();
    }

    public RGBLookup(short[] redMap, short[] greenMap, short[] blueMap) {
        this.redMap = redMap;
        this.greenMap = greenMap;
        this.blueMap = blueMap;
    }

    public RGBLookup(GrayScaleLookup rgb) {
        allocateArrays();

        for (short i = 0; i < ARRAY_LENGTH; i++) {
            short val = rgb.mapValue(i);
            redMap[i] = val;
            greenMap[i] = val;
            blueMap[i] = val;
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
            short rgbMapped = rgb.mapValue(i);

            redMap[i] = rgbMapped;
            redMap[i] = r.mapValue(redMap[i]);
            redMap[i] = rg.mapValue(redMap[i]);
            redMap[i] = rb.mapValue(redMap[i]);

            greenMap[i] = rgbMapped;
            greenMap[i] = g.mapValue(greenMap[i]);
            greenMap[i] = rg.mapValue(greenMap[i]);
            greenMap[i] = gb.mapValue(greenMap[i]);

            blueMap[i] = rgbMapped;
            blueMap[i] = b.mapValue(blueMap[i]);
            blueMap[i] = rb.mapValue(blueMap[i]);
            blueMap[i] = gb.mapValue(blueMap[i]);
        }
    }

    private void allocateArrays() {
        redMap = new short[ARRAY_LENGTH];
        greenMap = new short[ARRAY_LENGTH];
        blueMap = new short[ARRAY_LENGTH];
    }

    public LookupTable getLookupOp() {
        return LookupFactory.createLookupFrom3Arrays(redMap, greenMap, blueMap);
    }

    public void initFromPosterize(int numLevels) {
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            int mapping = (ARRAY_LENGTH - 1) * (numLevels * i / ARRAY_LENGTH) / (numLevels - 1);
            redMap[i] = (short) mapping;
            greenMap[i] = (short) mapping;
            blueMap[i] = (short) mapping;
        }
    }


    // From here test-only methods

    int mapRGBValue(int rgb) {
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = (rgb) & 0xFF;

        r = redMap[r];
        g = greenMap[g];
        b = blueMap[b];

        rgb = (a << 24) | (r << 16) | (g << 8) | b;

        return rgb;
    }

    int mapRed(int input) {
        return redMap[input];
    }

    int mapGreen(int input) {
        return greenMap[input];
    }

    int mapBlue(int input) {
        return blueMap[input];
    }
}
