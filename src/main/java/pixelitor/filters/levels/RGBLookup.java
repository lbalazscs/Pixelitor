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
 * Manages 3 lookup arrays, corresponding to the
 * R, G, B channels of a pixel-by-pixel adjustment
 */
public class RGBLookup {
    private static final int ARRAY_LENGTH = 256;

    private short[] finalRedMapping;
    private short[] finalGreenMapping;
    private short[] finalBlueMapping;

    public RGBLookup() {
        allocateArrays();
    }

    public RGBLookup(short[] finalRedMapping, short[] finalGreenMapping, short[] finalBlueMapping) {
        this.finalRedMapping = finalRedMapping;
        this.finalGreenMapping = finalGreenMapping;
        this.finalBlueMapping = finalBlueMapping;
    }

    public RGBLookup(GrayScaleLookup rgb) {
        allocateArrays();

        for (short i = 0; i < ARRAY_LENGTH; i++) {
            short val = rgb.mapValue(i);
            finalRedMapping[i] = val;
            finalGreenMapping[i] = val;
            finalBlueMapping[i] = val;
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

            finalRedMapping[i] = rgbMapped;
            finalRedMapping[i] = r.mapValue(finalRedMapping[i]);
            finalRedMapping[i] = rg.mapValue(finalRedMapping[i]);
            finalRedMapping[i] = rb.mapValue(finalRedMapping[i]);

            finalGreenMapping[i] = rgbMapped;
            finalGreenMapping[i] = g.mapValue(finalGreenMapping[i]);
            finalGreenMapping[i] = rg.mapValue(finalGreenMapping[i]);
            finalGreenMapping[i] = gb.mapValue(finalGreenMapping[i]);

            finalBlueMapping[i] = rgbMapped;
            finalBlueMapping[i] = b.mapValue(finalBlueMapping[i]);
            finalBlueMapping[i] = rb.mapValue(finalBlueMapping[i]);
            finalBlueMapping[i] = gb.mapValue(finalBlueMapping[i]);
        }
    }

    private void allocateArrays() {
        finalRedMapping = new short[ARRAY_LENGTH];
        finalGreenMapping = new short[ARRAY_LENGTH];
        finalBlueMapping = new short[ARRAY_LENGTH];
    }

    // this is used only by the test class

    public int mapRGBValue(int rgb) {
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = (rgb) & 0xFF;

        r = finalRedMapping[r];
        g = finalGreenMapping[g];
        b = finalBlueMapping[b];

        rgb = (a << 24) | (r << 16) | (g << 8) | b;

        return rgb;
    }


    public LookupTable getLookupOp() {
        return LookupFactory.createLookupFrom3Arrays(finalRedMapping, finalGreenMapping, finalBlueMapping);
    }

    public void initFromPosterize(int numLevels) {
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            int mapping = (ARRAY_LENGTH - 1) * (numLevels * i / ARRAY_LENGTH) / (numLevels - 1);
            finalRedMapping[i] = (short) mapping;
            finalGreenMapping[i] = (short) mapping;
            finalBlueMapping[i] = (short) mapping;
        }
    }
}
