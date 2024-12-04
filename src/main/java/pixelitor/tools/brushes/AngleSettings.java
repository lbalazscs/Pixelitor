/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
 * Angle-related settings for the brushes.
 */
public final class AngleSettings {
    // global shared immutable instances for the most common cases
    public static final AngleSettings NOT_ANGLED = new AngleSettings(false, 0);
    public static final AngleSettings ANGLED_NO_JITTER = new AngleSettings(true, 0);

    private static final String ANGLED_KEY = "Angled";
    private static final String MAX_JITTER_KEY = "Max Angle Jitter";

    private boolean angled;
    private double maxAngleJitter;

    public AngleSettings(boolean angled, double maxAngleJitter) {
        this.angled = angled;
        this.maxAngleJitter = maxAngleJitter;
    }

    public boolean isJitterEnabled() {
        return maxAngleJitter > 0;
    }

    public double calcJitteredAngle(double baseAngle) {
        double jitter = (2 * Math.random() - 1) * maxAngleJitter;
        return baseAngle + jitter;
    }

    public boolean isAngled() {
        return angled;
    }

    public void saveStateTo(UserPreset preset) {
        preset.putBoolean(ANGLED_KEY, angled);
        preset.putDouble(MAX_JITTER_KEY, maxAngleJitter);
    }

    public void loadStateFrom(UserPreset preset) {
        angled = preset.getBoolean(ANGLED_KEY);
        maxAngleJitter = preset.getDouble(MAX_JITTER_KEY);
    }

    @Override
    public String toString() {
        return "AngleSettings[" +
            "angled=" + angled + ", " +
            "maxAngleJitter=" + maxAngleJitter + ']';
    }
}
