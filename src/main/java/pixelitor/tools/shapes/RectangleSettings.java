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
 * The settings for a rectangle shape.
 */
class RectangleSettings extends ShapeTypeSettings {
    private final RangeParam radius;

    public RectangleSettings() {
        radius = new RangeParam("Rounding Radius (px)", 0, 0, 500);
    }

    private RectangleSettings(RangeParam radius) {
        this.radius = radius;
    }

    @Override
    protected JPanel createConfigPanel() {
        JPanel p = new JPanel();
        p.add(radius.createGUI());
        return p;
    }

    public double getRadius() {
        return radius.getValueAsDouble();
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        radius.setAdjustmentListener(listener);
    }

    @Override
    public RectangleSettings copy() {
        RectangleSettings copy = new RectangleSettings(radius.copy());
        return copy;
    }
}
