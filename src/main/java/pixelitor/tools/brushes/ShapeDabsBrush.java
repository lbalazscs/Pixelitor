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

import pixelitor.Composition;
import pixelitor.tools.ShapeType;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

public class ShapeDabsBrush extends DabsBrush {
    public ShapeDabsBrush(int radius, ShapeType shapeType, SpacingStrategy spacingStrategy,
                          AngleSettings angleSettings) {
        super(radius, new ShapeDabsBrushSettings(
                angleSettings,
                spacingStrategy,
                shapeType
        ), false);
    }

    public ShapeDabsBrush(int radius, ShapeDabsBrushSettings settings) {
        super(radius, settings, false);
    }

    @Override
    public void setTarget(Composition comp, Graphics2D g) {
        super.setTarget(comp, g);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    }

    @Override
    public void putDab(double x, double y, double theta) {
        ShapeType shapeType = ((ShapeDabsBrushSettings)settings).getShapeType();
        if (theta != 0) {
            Shape shape = shapeType.getShape(x - radius, y - radius, diameter);
            AffineTransform t = AffineTransform.getRotateInstance(theta, x, y);
            Shape transformedShape = t.createTransformedShape(shape);
            targetG.fill(transformedShape);
        } else {
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
