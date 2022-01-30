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

import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GUIUtils;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.List;
import java.util.function.Consumer;

/**
 * The settings for a line.
 */
class LineSettings extends ShapeTypeSettings {
    private final RangeParam width;
    private final EnumParam<StrokeCap> cap;

    public LineSettings() {
        width = new RangeParam("Width (px)", 1, 10, 100);
        cap = StrokeCap.asParam();
    }

    private LineSettings(RangeParam width, EnumParam<StrokeCap> cap) {
        this.width = width;
        this.cap = cap;
    }

    @Override
    protected JPanel createConfigPanel() {
        return GUIUtils.arrangeVertically(List.of(width, cap));
    }

    public Stroke getStroke() {
        //noinspection MagicConstant
        return new BasicStroke(width.getValueAsFloat(),
            cap.getSelected().getValue(), BasicStroke.JOIN_ROUND);
    }

    @Override
    protected void forEachParam(Consumer<FilterParam> consumer) {
        consumer.accept(width);
        consumer.accept(cap);
    }

    @Override
    public LineSettings copy() {
        EnumParam<StrokeCap> capCopy = StrokeCap.asParam(); // same default
        capCopy.setSelectedItem(cap.getSelected(), false); // different value

        return new LineSettings(width.copy(), capCopy);
    }

    @Override
    public String toString() {
        return "Line Settings, stroke = " + getStroke();
    }
}
