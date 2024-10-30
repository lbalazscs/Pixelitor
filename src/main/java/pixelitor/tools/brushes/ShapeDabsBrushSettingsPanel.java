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

package pixelitor.tools.brushes;

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.tools.shapes.ShapeType;

import javax.swing.*;

import static pixelitor.gui.utils.SliderSpinner.LabelPosition.NONE;

public class ShapeDabsBrushSettingsPanel extends BrushSettingsPanel {
    public static final ShapeType DEFAULT_SHAPE = ShapeType.ARROW;
    public static final double DEFAULT_SPACING_RATIO = 2.3;
    private static final int DEFAULT_SPACING = (int) Math.round(DEFAULT_SPACING_RATIO * 100);
    private final ShapeDabsBrushSettings settings;
    private BooleanParam angled;
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

        angled = new BooleanParam("Angle Follows Movement", true);
        addParam(angled, "angled");

        angleJitter.setAdjustmentListener(this::sngleSettingsChanged);
        angled.setAdjustmentListener(this::sngleSettingsChanged);
    }

    private void addSpacingSelector(DabsBrushSettings settings) {
        RangeParam spacing = new RangeParam("Spacing (radius %)", 1,
            DEFAULT_SPACING, 1000, true, NONE);
        addSlider(spacing, "spacing");
        spacing.setAdjustmentListener(
            () -> settings.setSpacing(new RadiusRatioSpacing(spacing.getPercentage())));
    }

    private void addShapeTypeSelector(ShapeDabsBrushSettings settings) {
        EnumParam<ShapeType> typeModel = ShapeType.asParam(DEFAULT_SHAPE);
        JComponent typeCombo = typeModel.createGUI("shape");
        typeModel.setAdjustmentListener(() ->
            settings.setShapeType(typeModel.getSelected()));

        gbh.addLabelAndControlNoStretch("Shape:", typeCombo);
    }

    private void sngleSettingsChanged() {
        settings.setAngleSettings(new AngleSettings(
            angled.isChecked(), angleJitter.getValueInRadians()));
    }
}
