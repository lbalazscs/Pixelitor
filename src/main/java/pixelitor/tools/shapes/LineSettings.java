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

import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GUIUtils;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.List;

/**
 * The settings for a line.
 */
class LineSettings extends ShapeTypeSettings {
    private final RangeParam width;
    private final EnumParam<BasicStrokeCap> cap;

    public LineSettings() {
        width = new RangeParam("Width (px)", 1, 10, 100);
        cap = BasicStrokeCap.asParam();
    }

    private LineSettings(RangeParam width, BasicStrokeCap defaultCap) {
        this.width = width;
        cap = BasicStrokeCap.asParam(defaultCap);
    }

    @Override
    protected JPanel createConfigPanel() {
        JPanel p = GUIUtils.arrVer(List.of(width, cap));
        return p;
    }

    public Stroke getStroke() {
        //noinspection MagicConstant
        return new BasicStroke(width.getValueAsFloat(),
            cap.getSelected().getValue(), BasicStroke.JOIN_ROUND);
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        width.setAdjustmentListener(listener);
        cap.setAdjustmentListener(listener);
    }

    @Override
    public LineSettings copy() {
        LineSettings copy = new LineSettings(width.copy(), cap.getSelected());
        return copy;
    }

    @Override
    public String toString() {
        return "Line Settings, stroke = " + getStroke();
    }
}
