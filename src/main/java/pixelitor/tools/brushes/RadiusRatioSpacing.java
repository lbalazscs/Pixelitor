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
 * A {@link Spacing} strategy where the distance between dabs
 * is proportional to the brush radius.
 */
public class RadiusRatioSpacing implements Spacing {
    private static final String RATIO_KEY = "Spacing Ratio";

    private double spacingRatio; // the spacing relative to the radius

    public RadiusRatioSpacing(double spacingRatio) {
        assert spacingRatio > 0;
        this.spacingRatio = spacingRatio;
    }

    @Override
    public double getSpacing(double radius) {
        assert radius > 0;
        return Math.max(radius * spacingRatio, MIN_SPACING);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        preset.putDouble(RATIO_KEY, spacingRatio);
    }

    @Override
    public void loadStateFrom(UserPreset preset) {
        spacingRatio = preset.getDouble(RATIO_KEY);
    }
}
