/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.brushes;

import pixelitor.filters.gui.UserPreset;

/**
 * A {@link Spacing} implementation where the spacing between the dabs
 * is fixed, and does not depend on the brush radius
 */
public class FixedDistanceSpacing implements Spacing {
    private static final String DISTANCE_KEY = "Distance";

    private double distance;

    public FixedDistanceSpacing(double distance) {
        this.distance = distance;
    }

    @Override
    public double getSpacing(double radius) {
        return distance;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        preset.putDouble(DISTANCE_KEY, distance);
    }

    @Override
    public void loadStateFrom(UserPreset preset) {
        distance = preset.getDouble(DISTANCE_KEY);
    }
}
