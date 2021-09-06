/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gui;

import javax.swing.*;

/**
 * A range with a logarithmic scale
 */
public class LogZoomParam extends RangeParam {
    /**
     * The values are on the logarithmic scale (base 10),
     * and multiplied by 100 for continuous zooming.
     * Example: the 100-10000 zoom range corresponds to 2-4 on
     * the logarithmic scale, and must be given as 200-400
     */
    public LogZoomParam(String name, int min, int def, int max) {
        super(name, min, def, max);
    }

    @Override
    public JComponent createGUI() {
        var gui = new LogRangeGUI(this);
        paramGUI = gui;
        afterGUICreation();
        return gui;
    }

    public double getZoomValue() {
        return Math.pow(10.0, getValueAsDouble() / 100.0);
    }

    public double getZoomRatio() {
        return getZoomValue() / 100.0;
    }
}
