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

package pixelitor.filters.gui;

/**
 * The transparency behavior for a color selector.
 */
public enum TransparencyMode {
    /**
     * Transparent colors can't be selected.
     */
    OPAQUE_ONLY(false, false),

    /**
     * The user can select an alpha value, but randomizing will always create opaque colors.
     */
    MANUAL_ALPHA_ONLY(true, false),

    /**
     * The user can select an alpha value, and randomizing will also randomize the alpha.
     */
    ALPHA_ENABLED(true, true);

    private final boolean allowTransparency;
    private final boolean randomizeTransparency;

    TransparencyMode(boolean allowTransparency, boolean randomizeTransparency) {
        this.allowTransparency = allowTransparency;
        this.randomizeTransparency = randomizeTransparency;
    }

    /**
     * Whether the user is allowed to select a color with transparency.
     */
    public boolean allowTransparency() {
        return allowTransparency;
    }

    /**
     * Whether the alpha channel should also be randomized.
     */
    public boolean randomizeTransparency() {
        return randomizeTransparency;
    }
}
