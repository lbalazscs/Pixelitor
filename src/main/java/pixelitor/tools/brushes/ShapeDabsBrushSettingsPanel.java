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
import pixelitor.tools.shapes.ShapeType;

import javax.swing.*;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

public class ShapeDabsBrushSettingsPanel extends BrushSettingsPanel {
    public static final ShapeType DEFAULT_SHAPE = ShapeType.ARROW;
    public static final double DEFAULT_SPACING_RATIO = 2.3;
    private final ShapeDabsBrushSettings settings;
    private BooleanParam angleAware;
    private RangeParam angleJitter;

    public ShapeDabsBrushSettingsPanel(ShapeDabsBrushSettings settings) {
        this.settings = settings;

        addShapeTypeSelector(settings);
        addSpacingSelector(settings);
        addAngleSettingsSelector();
    }

    private void addAngleSettingsSelector() {
        angleJitter = new RangeParam("Angle Jitter (degrees)", 0, 0, 180, true, NONE);
        addSlider(angleJitter, "angleJitter");

        angleAware = new BooleanParam("Angle Follows Movement", true);
        addParam(angleAware, "angleAware");

        angleJitter.setAdjustmentListener(this::changeAngleSettings);
        angleAware.setAdjustmentListener(this::changeAngleSettings);
    }

    private void addSpacingSelector(DabsBrushSettings settings) {
        RangeParam spacingSelector = new RangeParam("Spacing (radius %)", 1,
                (int) Math.round(DEFAULT_SPACING_RATIO * 100),
                1000, true, NONE);
        addSlider(spacingSelector, "spacing");
        spacingSelector.setAdjustmentListener(
            () -> settings.changeSpacing(new RadiusRatioSpacing(spacingSelector.getPercentageValF())));
    }

    private void addShapeTypeSelector(ShapeDabsBrushSettings settings) {
        EnumParam<ShapeType> typeModel = ShapeType.asParam(DEFAULT_SHAPE);
        JComponent typeSelector = typeModel.createGUI("shape");
        typeModel.setAdjustmentListener(() ->
                settings.setShapeType(typeModel.getSelected()));

        addLabelWithControlNoStretch("Shape:", typeSelector);
    }

    private void changeAngleSettings() {
        boolean angleAwareChecked = angleAware.isChecked();
        float angleJitterRadians = angleJitter.getValueInRadians();
        AngleSettings angleSettings = new AngleSettings(angleAwareChecked, angleJitterRadians);
        settings.changeAngleSettings(angleSettings);
    }
}
