/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.ShapeType;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * The settings of a {@link SprayBrush}
 */
public class SprayBrushSettings extends BrushSettings {
    private static final ShapeType DEFAULT_SHAPE = ShapeType.RANDOM_STAR;

    // the tool that owns these settings; the eraser has no
    // color that could be randomized, so its panel is different
    private final Tool tool;

    private final RangeParam radiusParam = new RangeParam("Average Shape Radius (px)", 1, 4, 20);
    private final RangeParam radiusVariabilityParam = new RangeParam("Shape Radius Variability (%)", 0, 50, 100);
    private final RangeParam flowParam = new RangeParam("Flow", 1, 5, 10);
    private final BooleanParam randomOpacityParam = new BooleanParam("Random Opacity", true);
    private final RangeParam colorRandomnessParam = new RangeParam("Color Randomness (%)", 0, 40, 100);
    private final EnumParam<ShapeType> typeParam = ShapeType.asParam(DEFAULT_SHAPE);

    public SprayBrushSettings(Tool tool) {
        this.tool = tool;
    }

    @Override
    protected void forEachParam(Consumer<FilterParam> consumer) {
        consumer.accept(radiusParam);
        consumer.accept(radiusVariabilityParam);
        consumer.accept(flowParam);
        consumer.accept(randomOpacityParam);
        consumer.accept(colorRandomnessParam);
        consumer.accept(typeParam);
    }

    @Override
    protected JPanel createConfigPanel() {
        BrushSettingsPanel p = new BrushSettingsPanel();

        p.addParam(typeParam, "shape");

        p.addSlider(radiusParam, "avgRadius");
        p.addSlider(radiusVariabilityParam, "radiusVar");
        p.addSlider(flowParam, "flow");
        p.addParam(randomOpacityParam, "rndOpacity");

        if (tool != Tools.ERASER) {
            p.addSlider(colorRandomnessParam, "colorRand");
        }

        return p;
    }

    public ShapeType getShapeType() {
        if (typeParam != null) {
            return typeParam.getSelected();
        }
        return DEFAULT_SHAPE;
    }

    public double getShapeRadius() {
        return radiusParam.getValue();
    }

    public int getFlow() {
        return flowParam.getValue();
    }

    public double getRadiusVariability() {
        return radiusVariabilityParam.getPercentage();
    }

    public boolean randomOpacity() {
        return randomOpacityParam.isChecked();
    }

    public double getColorRandomness() {
        return colorRandomnessParam.getPercentage();
    }
}
