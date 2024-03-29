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
 * The settings for a rectangle shape.
 */
class RectangleSettings extends ShapeTypeSettings {
    private final RangeParam radius = new RangeParam("Rounding Radius (px)", 0, 0, 500);
    private final List<FilterParam> params = List.of(radius);

    public RectangleSettings() {
    }

    @Override
    public List<FilterParam> getParams() {
        return params;
    }

    public double getRadius() {
        return radius.getValueAsDouble();
    }
}
