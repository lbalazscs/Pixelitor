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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.ShapeType;

import javax.swing.*;

/**
 * The settings of a {@link SprayBrush}
 */
public class SprayBrushSettings extends BrushSettings {
    private static final ShapeType DEFAULT_SHAPE = ShapeType.RANDOM_STAR;

    private final RangeParam radiusModel = new RangeParam("Average Shape Radius (px)", 1, 4, 20);
    private final RangeParam radiusVariabilityModel = new RangeParam("Shape Radius Variability (%)", 0, 50, 100);
    private final RangeParam flowModel = new RangeParam("Flow", 1, 5, 10);
    private final BooleanParam randomOpacityModel = new BooleanParam("Random Opacity", true);
    private final RangeParam colorRandomnessModel = new RangeParam("Color Randomness (%)", 0, 40, 100);
    private EnumParam<ShapeType> typeModel;

    @Override
    protected JPanel createConfigPanel() {
        BrushSettingsPanel p = new BrushSettingsPanel();

        typeModel = ShapeType.asParam(DEFAULT_SHAPE);
        p.addParam(typeModel, "shape");

        p.addSlider(radiusModel, "avgRadius");
        p.addSlider(radiusVariabilityModel, "radiusVar");
        p.addSlider(flowModel, "flow");
        p.addParam(randomOpacityModel, "rndOpacity");

        if (tool != Tools.ERASER) {
            p.addSlider(colorRandomnessModel, "colorRand");
        }

        return p;
    }

    public ShapeType getShapeType() {
        if (typeModel != null) {
            return typeModel.getSelected();
        }
        return DEFAULT_SHAPE;
    }

    public double getShapeRadius() {
        return radiusModel.getValue();
    }

    public int getFlow() {
        return flowModel.getValue();
    }

    public float getRadiusVariability() {
        return radiusVariabilityModel.getPercentageValF();
    }

    public boolean randomOpacity() {
        return randomOpacityModel.isChecked();
    }

    public float getColorRandomness() {
        return colorRandomnessModel.getPercentageValF();
    }
}
