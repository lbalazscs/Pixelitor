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

import javax.swing.*;

/**
 * The settings for a line.
 */
class StarSettings extends ShapeTypeSettings {
    private final RangeParam numBranches;

    public StarSettings() {
        numBranches = new RangeParam("Number of Branches", 3, 7, 12);
    }

    private StarSettings(RangeParam numBranches) {
        this.numBranches = numBranches;
    }

    @Override
    protected JPanel createConfigPanel() {
//        JPanel p = GUIUtils.arrangeVertically(List.of(width, cap));
        JPanel p = new JPanel();
        p.add(numBranches.createGUI("numBranches"));
        return p;
    }

    public int getNumBranches() {
        return numBranches.getValue();
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        numBranches.setAdjustmentListener(listener);
    }

    @Override
    public StarSettings copy() {
        StarSettings copy = new StarSettings(numBranches.copy());
        return copy;
    }

    @Override
    public String toString() {
        return "Star Settings, #branches = " + getNumBranches();
    }
}
