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
 * A strategy for determining the spacing distance between brush dabs.
 * Used by the {@link DabsStrategy} implementations.
 */
public interface Spacing {
    double MIN_SPACING = 1.0;

    /**
     * Calculates the desired spacing in image pixels based on the current brush radius.
     */
    double getSpacing(double radius);

    void saveStateTo(UserPreset preset);

    void loadStateFrom(UserPreset preset);
}
