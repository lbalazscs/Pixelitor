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

import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.List;

/**
 * The settings for a line.
 */
class LineSettings extends ShapeTypeSettings {
    private final RangeParam width = new RangeParam("Width (px)", 1, 10, 100);
    private final EnumParam<StrokeCap> cap = StrokeCap.asParam();
    private final List<FilterParam> params = List.of(width, cap);

    public LineSettings() {
    }

    @Override
    public List<FilterParam> getParams() {
        return params;
    }

    public Stroke getStroke() {
        //noinspection MagicConstant
        return new BasicStroke(width.getValueAsFloat(),
            cap.getSelected().getValue(), BasicStroke.JOIN_ROUND);
    }

    @Override
    public String toString() {
        return "Line Settings, stroke = " + getStroke();
    }
}
