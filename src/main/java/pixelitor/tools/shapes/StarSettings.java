/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GUIUtils;

import javax.swing.*;
import java.util.List;

/**
 * The settings for a line.
 */
class StarSettings extends ShapeTypeSettings {
    private final RangeParam numBranches;
    private final RangeParam radiusRatio;

    public StarSettings() {
        numBranches = new RangeParam("Number of Branches", 3, 7, 12);
        radiusRatio = new RangeParam("Inner/Outer Radius Ratio (%)", 1, 50, 99);
    }

    private StarSettings(RangeParam numBranches, RangeParam radiusRatio) {
        this.numBranches = numBranches;
        this.radiusRatio = radiusRatio;
    }

    @Override
    protected JPanel createConfigPanel() {
        return GUIUtils.arrangeVertically(List.of(numBranches, radiusRatio));
    }

    public int getNumBranches() {
        return numBranches.getValue();
    }

    public double getRadiusRatio() {
        return radiusRatio.getPercentageValD();
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        numBranches.setAdjustmentListener(listener);
        radiusRatio.setAdjustmentListener(listener);
    }

    @Override
    public StarSettings copy() {
        return new StarSettings(numBranches.copy(), radiusRatio.copy());
    }

    @Override
    public String toString() {
        return "Star Settings, #branches = " + getNumBranches();
    }
}
