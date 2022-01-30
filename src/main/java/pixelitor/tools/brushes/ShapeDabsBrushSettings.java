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

package pixelitor.tools.brushes;

import pixelitor.tools.shapes.ShapeType;

import javax.swing.*;

import static pixelitor.tools.brushes.AngleSettings.ANGLE_AWARE_NO_JITTER;

/**
 * The settings of a {@link ShapeDabsBrush}
 */
public class ShapeDabsBrushSettings extends DabsBrushSettings {
    private ShapeType shapeType;

    public ShapeDabsBrushSettings() {
        this(ANGLE_AWARE_NO_JITTER,
            new RadiusRatioSpacing(ShapeDabsBrushSettingsPanel.DEFAULT_SPACING_RATIO),
            ShapeDabsBrushSettingsPanel.DEFAULT_SHAPE);
    }

    private ShapeDabsBrushSettings(AngleSettings angleSettings,
                                   Spacing spacing, ShapeType shapeType) {
        super(angleSettings, spacing);
        this.shapeType = shapeType;
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public void setShapeType(ShapeType shapeType) {
        this.shapeType = shapeType;
    }

    @Override
    protected JPanel createConfigPanel() {
        return new ShapeDabsBrushSettingsPanel(this);
    }
}
