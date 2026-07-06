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

package pixelitor.tools.gradient;

import java.awt.MultipleGradientPaint.CycleMethod;

/**
 * The cycling method of a gradient.
 * CycleMethods can't be put directly into a JComboBox,
 * because their toString() would be all uppercase, so this wrapper
 * enum pairs a CycleMethod with a display name.
 */
public enum GradientCycling {
    NO_CYCLE("No Cycle", CycleMethod.NO_CYCLE),
    REFLECT("Reflect", CycleMethod.REFLECT),
    REPEAT("Repeat", CycleMethod.REPEAT);

    public static final String PRESET_KEY = "Cycling";

    private final String displayName;
    private final CycleMethod cycleMethod;

    GradientCycling(String displayName, CycleMethod cycleMethod) {
        this.displayName = displayName;
        this.cycleMethod = cycleMethod;
    }

    public CycleMethod getCycleMethod() {
        return cycleMethod;
    }

    public static GradientCycling from(CycleMethod cycleMethod) {
        for (GradientCycling cycling : values()) {
            if (cycling.cycleMethod == cycleMethod) {
                return cycling;
            }
        }
        throw new IllegalArgumentException("Unknown cycle method: " + cycleMethod);
    }

    public static GradientCycling fromPresetString(String s) {
        if (s == null) {
            return NO_CYCLE;
        }
        for (GradientCycling cycling : values()) {
            // check both enum name (new presets) and display name (old presets)
            if (cycling.name().equals(s) || cycling.displayName.equals(s)) {
                return cycling;
            }
        }
        return NO_CYCLE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
