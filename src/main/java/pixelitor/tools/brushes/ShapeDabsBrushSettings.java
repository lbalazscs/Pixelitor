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

import pixelitor.tools.shapes.ShapeType;

import javax.swing.*;

import static pixelitor.tools.brushes.RotationSettings.DIRECTIONAL_NO_JITTER;
import static pixelitor.tools.brushes.ShapeDabsBrushSettingsPanel.DEFAULT_SHAPE;
import static pixelitor.tools.brushes.ShapeDabsBrushSettingsPanel.DEFAULT_SPACING_RATIO;

/**
 * The settings of a {@link ShapeDabsBrush}.
 */
public class ShapeDabsBrushSettings extends DabsBrushSettings {
    private static final Spacing SPACING = new RadiusRatioSpacing(DEFAULT_SPACING_RATIO);
    private ShapeType shapeType;

    public ShapeDabsBrushSettings() {
        super(DIRECTIONAL_NO_JITTER, SPACING);
        this.shapeType = DEFAULT_SHAPE;
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
