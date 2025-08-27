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
import pixelitor.gui.GUIText;

import java.util.List;

/**
 * Represents a supported, non-cylindrical color space.
 */
public enum ColorSpace {
    SRGB("sRGB (Faster)"),
    OKLAB("Oklab (Better)");

    private final String displayName;

    ColorSpace(String displayName) {
        this.displayName = displayName;
    }

    public List<Channel> getChannels() {
        return switch (this) {
            case SRGB -> List.of(Channel.RGB, Channel.RED, Channel.GREEN, Channel.BLUE);
            case OKLAB -> List.of(Channel.OK_L, Channel.OK_A, Channel.OK_B);
        };
    }

    /**
     * Returns the primary channel for this color space.
     * Used as the default and randomized channel in the GUI.
     */
    public Channel getPrimaryChannel() {
        return switch (this) {
            case SRGB -> Channel.RGB;
            case OKLAB -> Channel.OK_L;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Creates an {@link EnumParam} for selecting an enum value.
     */
    public static EnumParam<ColorSpace> asParam() {
        return new EnumParam<>(GUIText.COLOR_SPACE, ColorSpace.PRESET_KEY, ColorSpace.class);
    }

    // the preset key for any color-space-related filter param
    public static final String PRESET_KEY = "color_space";
}
