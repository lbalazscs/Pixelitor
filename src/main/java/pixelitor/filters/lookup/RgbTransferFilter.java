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

import com.jhlabs.image.TransferFilter;

import java.util.Objects;

/**
 * A {@link TransferFilter} backend that applies an {@link RGBLookup}.
 */
public class RgbTransferFilter extends TransferFilter {
    private final RGBLookup lookup;

    public RgbTransferFilter(RGBLookup lookup) {
        super("RGB Transfer");
        this.lookup = Objects.requireNonNull(lookup, "lookup must not be null");

        short[] redLut = lookup.getRedLut();
        short[] greenLut = lookup.getGreenLut();
        short[] blueLut = lookup.getBlueLut();

        rTable = new int[256];
        gTable = new int[256];
        bTable = new int[256];

        for (int i = 0; i < 256; i++) {
            rTable[i] = redLut[i] & 0xFFFF;
            gTable[i] = greenLut[i] & 0xFFFF;
            bTable[i] = blueLut[i] & 0xFFFF;
        }

        initialized = true;
    }

    @Override
    protected void initialize() {
        // the default TransferFilter.initialize() would overwrite the tables with zeros
        initialized = true;
    }

    public RGBLookup getLookup() {
        return lookup;
    }
}
