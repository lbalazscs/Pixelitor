/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.RangeParam;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Settings specific to the {@link ConnectBrush}.
 */
public class ConnectBrushSettings extends BrushSettings {
    private final EnumParam<Style> styleModel = Style.asParam();
    private final RangeParam densityModel = new RangeParam("Line Density (%)", 1, 50, 100);
    private final RangeParam widthModel = new RangeParam("Line Width (px)", 1, 1, 10);
    private final BooleanParam resetForEachStroke = new BooleanParam(
        "Reset History for Each Stroke");

    @Override
    protected void forEachParam(Consumer<FilterParam> consumer) {
        consumer.accept(styleModel);
        consumer.accept(densityModel);
        consumer.accept(widthModel);
        consumer.accept(resetForEachStroke);
    }

    public boolean shouldClearHistoryPerStroke() {
        return resetForEachStroke.isChecked();
    }

    public Style getStyle() {
        return styleModel.getSelected();
    }

    @Override
    protected JPanel createConfigPanel() {
        BrushSettingsPanel p = new BrushSettingsPanel();

        p.addParam(styleModel, "style");
        p.addSlider(densityModel, "density");
        p.addSlider(widthModel, "width");
        p.addParam(resetForEachStroke, "resetForEach");

        // a button for manual history reset
        p.addFullWidthButton("Reset History Now",
            e -> ConnectBrush.clearHistory(), "resetHistNow");

        return p;
    }

    public double getDensity() {
        return densityModel.getPercentage();
    }

    public float getLineWidth() {
        return widthModel.getValueAsFloat();
    }

    public enum Style {
        NORMAL("Normal", 0.0), // lines connect directly between points
        CHROME("Chrome", -0.2), // lines are offset slightly inwards
        FUR("Fur", 0.2); // lines are offset slightly outwards
//        LONG_FUR("Long Fur", 0.5);

        private final String displayName;

        // the offset of connecting lines along the vector between points
        private final double offsetFactor;

        Style(String displayName, double offsetFactor) {
            this.displayName = displayName;
            this.offsetFactor = offsetFactor;
        }

        public double getOffsetFactor() {
            return offsetFactor;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public static EnumParam<Style> asParam() {
            return new EnumParam<>("Style", Style.class);
        }
    }
}
