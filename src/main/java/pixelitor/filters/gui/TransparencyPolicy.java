/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
 * Whether transparent colors are allowed in a color selector.
 */
public enum TransparencyPolicy {
    /**
     * Transparent colors can't be selected.
     */
    NO_TRANSPARENCY(false, false),

    /**
     * The user can select an alpha value, but randomizing will
     * always create opaque colors.
     */
    USER_ONLY_TRANSPARENCY(true, false),

    /**
     * The user can select an alpha value, and randomizing will
     * with randomize the alpha
     */
    FREE_TRANSPARENCY(true, true);

    private final boolean allowTransparency;
    private final boolean randomizeTransparency;

    TransparencyPolicy(boolean allowTransparency, boolean randomizeTransparency) {
        this.allowTransparency = allowTransparency;
        this.randomizeTransparency = randomizeTransparency;
    }

    public boolean allowTransparency() {
        return allowTransparency;
    }

    public boolean randomizeTransparency() {
        return randomizeTransparency;
    }
}
