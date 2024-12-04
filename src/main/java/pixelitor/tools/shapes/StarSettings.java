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

package pixelitor.tools.shapes;

import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.RangeParam;

import java.util.List;

/**
 * The settings for a star shape.
 */
public class StarSettings extends ShapeTypeSettings {
    public static final int DEFAULT_NUM_BRANCHES = 5;
    public static final double DEFAULT_RADIUS_RATIO = 0.38;

    private final RangeParam numBranches = new RangeParam("Number of Branches",
        3, DEFAULT_NUM_BRANCHES, 12);
    private final RangeParam radiusRatio = new RangeParam("Inner/Outer Radius Ratio (%)",
        1, DEFAULT_RADIUS_RATIO * 100, 100);
    private final List<FilterParam> params = List.of(numBranches, radiusRatio);

    public StarSettings() {
    }

    public StarSettings(int defNumBranches, int defRadiusRatio) {
        numBranches.setValueNoTrigger(defNumBranches);
        radiusRatio.setValueNoTrigger(defRadiusRatio);
    }

    @Override
    public List<FilterParam> getParams() {
        return params;
    }

    public int getNumBranches() {
        return numBranches.getValue();
    }

    public double getRadiusRatio() {
        return radiusRatio.getPercentage();
    }

    @Override
    public String toString() {
        return "Star Settings, #branches = " + getNumBranches();
    }
}
