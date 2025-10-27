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

package pixelitor.tools.brushes;

import pixelitor.filters.gui.UserPreset;

/**
 * Encapsulates the configuration for the orientation of brush dabs.
 */
public final class RotationSettings {
    // global shared immutable instances for the most common cases
    public static final RotationSettings NOT_DIRECTIONAL = new RotationSettings(false, 0);
    public static final RotationSettings DIRECTIONAL_NO_JITTER = new RotationSettings(true, 0);

    private static final String DIRECTIONAL_KEY = "Angled";
    private static final String MAX_JITTER_KEY = "Max Angle Jitter";

    // whether the dabs are rotated to follow the stroke direction
    private final boolean directional;

    private final double maxAngleJitter;

    public RotationSettings(boolean directional, double maxAngleJitter) {
        this.directional = directional;
        this.maxAngleJitter = maxAngleJitter;
    }

    public boolean isJitterEnabled() {
        return maxAngleJitter > 0;
    }

    /**
     * Applies random angular jitter to the given base angle.
     */
    public double jitterAngle(double baseAngle) {
        if (maxAngleJitter == 0) {
            return baseAngle;
        }
        double jitter = (2 * Math.random() - 1) * maxAngleJitter;
        return baseAngle + jitter;
    }

    public boolean isDirectional() {
        return directional;
    }

    public void saveStateTo(UserPreset preset) {
        preset.putBoolean(DIRECTIONAL_KEY, directional);
        preset.putDouble(MAX_JITTER_KEY, maxAngleJitter);
    }

    public static RotationSettings fromPreset(UserPreset preset) {
        return new RotationSettings(
            preset.getBoolean(DIRECTIONAL_KEY),
            preset.getDouble(MAX_JITTER_KEY));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
            "follows stroke dir=" + directional + ", " +
            "maxAngleJitter=" + maxAngleJitter + ']';
    }
}
