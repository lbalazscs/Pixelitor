/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.tools.ShapeType;

import java.awt.Shape;
import java.awt.geom.AffineTransform;

public class ShapeDabsBrush extends DabsBrush {
    public ShapeDabsBrush(ShapeType shapeType, SpacingStrategy spacingStrategy,
                          AngleSettings angleSettings) {
        super(new ShapeDabsBrushSettings(
                angleSettings,
                spacingStrategy,
                shapeType
        ), false);
    }

    public ShapeDabsBrush(ShapeDabsBrushSettings settings) {
        super(settings, false);
    }

    @Override
    public void putDab(double x, double y, double theta) {
        ShapeType shapeType = ((ShapeDabsBrushSettings)settings).getShapeType();
        if (settings.isAngleAware()) {
            Shape shape = shapeType.getShape(x - radius, y - radius, diameter);
            AffineTransform t = AffineTransform.getRotateInstance(theta, x, y);
            Shape transformedShape = t.createTransformedShape(shape);
            targetG.fill(transformedShape);
        } else {
            assert theta == 0;
            Shape shape = shapeType.getShape(x - radius, y - radius, diameter);
            targetG.fill(shape);
        }
        updateComp((int) x, (int) y);
    }

    @Override
    void setupBrushStamp(double x, double y) {
        // no setup is necessary for shape brushes
    }
}
