/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import java.awt.image.LookupTable;
import java.awt.image.ShortLookupTable;

/**
 * A class with static methods that produce LookupTable objects for image
 * filters
 */
public final class LookupFactory {

    /**
     * Utility class with static methods
     */
    private LookupFactory() {
    }

    public static LookupTable createLookupForRemoveRed() {
        short[][] lookupData = new short[3][256];
        lookupData[0] = getNullMapping();
        lookupData[1] = getDefaultMapping();
        lookupData[2] = getDefaultMapping();
        return new ShortLookupTable(0, lookupData);
    }

    public static LookupTable createLookupForOnlyRed() {
        short[][] lookupData = new short[3][256];
        lookupData[0] = getDefaultMapping();
        lookupData[1] = getNullMapping();
        lookupData[2] = getNullMapping();
        return new ShortLookupTable(0, lookupData);
    }

    public static LookupTable createLookupForRemoveGreen() {
        short[][] lookupData = new short[3][256];
        lookupData[0] = getDefaultMapping();
        lookupData[1] = getNullMapping();
        lookupData[2] = getDefaultMapping();
        return new ShortLookupTable(0, lookupData);
    }

    public static LookupTable createLookupForOnlyGreen() {
        short[][] lookupData = new short[3][256];
        lookupData[0] = getNullMapping();
        lookupData[1] = getDefaultMapping();
        lookupData[2] = getNullMapping();
        return new ShortLookupTable(0, lookupData);
    }


    public static LookupTable createLookupForRemoveBlue() {
        short[][] lookupData = new short[3][256];
        lookupData[0] = getDefaultMapping();
        lookupData[1] = getDefaultMapping();
        lookupData[2] = getNullMapping();
        return new ShortLookupTable(0, lookupData);
    }

    public static LookupTable createLookupForOnlyBlue() {
        short[][] lookupData = new short[3][256];
        lookupData[0] = getNullMapping();
        lookupData[1] = getNullMapping();
        lookupData[2] = getDefaultMapping();
        return new ShortLookupTable(0, lookupData);
    }

    public static LookupTable createLookupFrom3Arrays(short[] redMappings, short[] greenMappings, short[] blueMappings) {
        short[][] lookupData = new short[3][256];
        lookupData[0] = redMappings;
        lookupData[1] = greenMappings;
        lookupData[2] = blueMappings;
        return new ShortLookupTable(0, lookupData);
    }

    private static short[] getDefaultMapping() {
        short[] lookupData = new short[256];
        for (int i = 0; i < 256; i++) {
            lookupData[i] = (short) i;
        }
        return lookupData;
    }

    private static short[] getNullMapping() {
        short[] lookupData = new short[256];
        for (int i = 0; i < 256; i++) {
            lookupData[i] = 0;
        }
        return lookupData;
    }


}
