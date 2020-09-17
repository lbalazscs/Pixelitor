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

package pixelitor.tools.brushes;

import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;

import javax.swing.*;

public class ConnectBrushSettings extends BrushSettings {
    private static final boolean RESET_DEFAULT = false;

    private JCheckBox resetForEachStroke;

    private final EnumParam<Style> styleModel = Style.asParam();

    private final RangeParam densityModel = new RangeParam("Line Density (%)", 1, 50, 100);
    private final RangeParam widthModel = new RangeParam("Line Width (px)", 1, 1, 10);

    public boolean deleteHistoryForEachStroke() {
        if (resetForEachStroke == null) { // not configured
            return RESET_DEFAULT;
        }
        return resetForEachStroke.isSelected();
    }

    public Style getStyle() {
        return styleModel.getSelected();
    }

    @Override
    protected JPanel createConfigPanel() {
        BrushSettingsPanel p = new BrushSettingsPanel();

        p.addParam(styleModel, "length");
        p.addSlider(densityModel, "density");
        p.addSlider(widthModel, "width");

        resetForEachStroke = new JCheckBox("", RESET_DEFAULT);
        resetForEachStroke.setName("resetForEach");
        p.addLabelWithControl("Reset History for Each Stroke: ", resetForEachStroke);

        p.addOnlyButton("Reset History Now",
            e -> ConnectBrush.deleteHistory(), "resetHistNow");

        return p;
    }

    public double getDensity() {
        return densityModel.getPercentageValF();
    }

    public float getLineWidth() {
        return widthModel.getValueAsFloat();
    }

    public enum Style {
        NORMAL("Normal", 0.0),
        CHROME("Chrome", -0.2),
        FUR("Fur", 0.2);
//        LONG_FUR("Long Fur", 0.5);

        private final String guiName;
        private final double offset;

        Style(String guiName, double offset) {
            this.guiName = guiName;
            this.offset = offset;
        }

        public double getOffset() {
            return offset;
        }

        @Override
        public String toString() {
            return guiName;
        }

        public static EnumParam<Style> asParam() {
            EnumParam<Style> param = new EnumParam<>("Length", Style.class);
            return param;
        }
    }
}
