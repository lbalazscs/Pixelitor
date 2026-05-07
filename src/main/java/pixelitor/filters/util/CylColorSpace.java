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

package pixelitor.filters.util;

import pixelitor.filters.gui.EnumParam;
import pixelitor.gui.GUIText;

/**
 * A cylindrical color space.
 */
public enum CylColorSpace {
    HSV("HSV (Faster)"),
    OKLCH("Oklch (Better)");

    private final String displayName;

    CylColorSpace(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static EnumParam<CylColorSpace> asParam() {
        return new EnumParam<>(
            GUIText.COLOR_SPACE, ColorSpace.PRESET_KEY, CylColorSpace.class
        );
    }
}
