/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.util;

import pixelitor.filters.gui.EnumParam;

/**
 * Represents a supported, non-cylindrical color space.
 */
public enum ColorSpace {
    SRGB("sRGB (Faster)", Channel.RGB),
    OKLAB("Oklab (Better)", Channel.OK_L);

    private final String displayName;
    private final Channel mainChannel;

    ColorSpace(String displayName, Channel mainChannel) {
        this.displayName = displayName;
        this.mainChannel = mainChannel;
    }

    /**
     * Returns the primary channel for this color space.
     * Used as the default and randomized channel in the GUI.
     */
    public Channel getPrimaryChannel() {
        return mainChannel;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Creates an {@link EnumParam} for selecting an enum value.
     */
    public static EnumParam<ColorSpace> asParam() {
        return new EnumParam<>("Color Space", ColorSpace.class);
    }
}
