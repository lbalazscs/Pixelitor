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

import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;

import java.awt.image.BufferedImageOp;
import java.util.Objects;
import java.util.function.Function;

/**
 * Factory for creating pixel operations from per-channel lookup tables.
 */
public final class LookupOps {
    /**
     * Controls which {@link BufferedImageOp} backend is used for sRGB adjustments.
     * true = {@link FastLookupOp} (optimized for packed-integer images, but does
     * not support single-band grayscale images directly);
     * false = {@link RgbTransferFilter} (supports grayscale images directly).
     */
    private static final boolean SRGB_FAST_LOOKUP = true;

    private LookupOps() {
    }

    /**
     * Creates a {@link BufferedImageOp} for the given color space and channel lookup provider,
     * using the default sRGB backend switch.
     */
    public static BufferedImageOp create(ColorSpace cs, Function<Channel, ChannelLookup> lookups) {
        return create(cs, lookups, SRGB_FAST_LOOKUP);
    }

    /**
     * Package-private overload with an explicit backend flag for testing and benchmarking.
     */
    static BufferedImageOp create(ColorSpace cs, Function<Channel, ChannelLookup> lookups, boolean srgbFastLookup) {
        Objects.requireNonNull(cs, "cs must not be null");
        Objects.requireNonNull(lookups, "lookups must not be null");

        return switch (cs) {
            case SRGB -> {
                RGBLookup rgbLookup = new RGBLookup(
                    lookups.apply(Channel.RGB),
                    lookups.apply(Channel.RED),
                    lookups.apply(Channel.GREEN),
                    lookups.apply(Channel.BLUE)
                );
                yield srgbFastLookup ? rgbLookup.asFastLookupOp() : new RgbTransferFilter(rgbLookup);
            }
            case OKLAB -> new OklabLookupFilter(
                lookups.apply(Channel.OK_L),
                lookups.apply(Channel.OK_A),
                lookups.apply(Channel.OK_B)
            );
        };
    }

    /**
     * Returns true if the active sRGB backend supports grayscale images directly.
     */
    public static boolean supportsGray() {
        return !SRGB_FAST_LOOKUP;
    }

    /**
     * Returns true if the default sRGB backend is {@link FastLookupOp}.
     */
    public static boolean isSrgbFastLookup() {
        return SRGB_FAST_LOOKUP;
    }
}
