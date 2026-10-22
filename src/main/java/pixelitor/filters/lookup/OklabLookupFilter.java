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

import com.jhlabs.image.PointFilter;
import pixelitor.colors.ColorSpaces;

import java.util.Objects;

/**
 * A {@link PointFilter} that applies lookup-table adjustments in the Oklab color space.
 */
public class OklabLookupFilter extends PointFilter {
    /**
     * Domain for the 'a' and 'b' channels of Oklab that comfortably covers the sRGB gamut.
     */
    private static final float AB_CHANNEL_MIN = -0.5f;
    private static final float AB_CHANNEL_MAX = 0.5f;
    private static final float AB_CHANNEL_RANGE = AB_CHANNEL_MAX - AB_CHANNEL_MIN;

    private final short[] lTable;
    private final short[] aTable;
    private final short[] bTable;

    public OklabLookupFilter(ChannelLookup l, ChannelLookup a, ChannelLookup b) {
        super("Oklab Lookup");
        Objects.requireNonNull(l, "l must not be null");
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(b, "b must not be null");
        this.lTable = l.getTable();
        this.aTable = a.getTable();
        this.bTable = b.getTable();
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        // note: processPixel receives raw (possibly premultiplied) values for TYPE_INT_ARGB_PRE images
        int alpha = rgb & 0xFF_00_00_00;
        if (alpha == 0) {
            return rgb; // fully transparent: nothing visible to adjust
        }

        // convert sRGB (int) to Oklab (float[])
        float[] oklab = ColorSpaces.srgbToOklab(rgb);
        float l = oklab[0];
        float a = oklab[1];
        float b = oklab[2];

        // apply lookup tables
        // the L channel is already in [0, 1]; scale it to a table index
        int lIndex = Math.clamp((int) (l * 255.0f + 0.5f), 0, 255);
        float newL = lTable[lIndex] / 255.0f;

        // a and b channels are normalized from their specific range
        float newA = applyAbLookup(a, aTable);
        float newB = applyAbLookup(b, bTable);

        // convert back to sRGB while preserving original alpha
        return alpha | (ColorSpaces.oklabToSrgb(newL, newA, newB) & 0x00_FF_FF_FF);
    }

    /**
     * Applies lookup table adjustments to an Oklab 'a' or 'b' channel value.
     */
    private static float applyAbLookup(float channelValue, short[] table) {
        // normalize the value from its domain to [0, 1] for LUT lookup
        float normalized = (channelValue - AB_CHANNEL_MIN) / AB_CHANNEL_RANGE;
        int index = Math.clamp((int) (normalized * 255.0f + 0.5f), 0, 255);

        // get the transformed value from the LUT and scale it back to [0, 1]
        float transformedNormalized = table[index] / 255.0f;

        // denormalize the value back to the original domain
        return transformedNormalized * AB_CHANNEL_RANGE + AB_CHANNEL_MIN;
    }
}
