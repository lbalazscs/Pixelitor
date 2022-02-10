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

package pixelitor.tools.shapes;

import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.RangeParam;

import java.util.List;

/**
 * The settings for a line.
 */
public class StarSettings extends ShapeTypeSettings {
    private final RangeParam numBranches = new RangeParam("Number of Branches",
        3, 7, 12);
    private final RangeParam radiusRatio = new RangeParam("Inner/Outer Radius Ratio (%)",
        1, 50, 100);
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
        return radiusRatio.getPercentageValD();
    }

    @Override
    public String toString() {
        return "Star Settings, #branches = " + getNumBranches();
    }
}
