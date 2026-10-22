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

package pixelitor.filters.channels;

import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.lookup.ChannelLookup;
import pixelitor.filters.util.Channel;

/**
 * The per-channel sub-model interface for channel-based adjustments
 * (e.g., Levels and Curves).
 */
public interface ChannelAdjustment {
    /**
     * Returns the channel represented by this adjustment.
     */
    Channel getChannel();

    /**
     * Creates the lookup table for this channel's adjustments.
     */
    ChannelLookup toLookup();

    /**
     * Resets this adjustment to its identity state without
     * triggering the adjustment listener.
     */
    void reset();

    /**
     * Randomizes this channel's adjustment.
     * Precondition: the adjustment is in its reset state.
     */
    void randomize();

    /**
     * Saves this adjustment's settings to the given preset.
     */
    void saveTo(UserPreset preset);

    /**
     * Restores this adjustment's settings from the given preset.
     * Missing keys leave the current state untouched.
     */
    void loadFrom(UserPreset preset);
}
