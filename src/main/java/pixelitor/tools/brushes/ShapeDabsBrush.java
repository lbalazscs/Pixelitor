/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.util.PPoint;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * A {@link DabsBrush} where the dabs are filled shapes
 */
public class ShapeDabsBrush extends DabsBrush {
    public ShapeDabsBrush(double radius, ShapeDabsBrushSettings settings) {
        super(radius, settings, false);
    }

    @Override
    public void setTarget(Composition comp, Graphics2D g) {
        super.setTarget(comp, g);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    }

    @Override
    public void putDab(PPoint p, double theta) {
        double x = p.getImX();
        double y = p.getImY();
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
        updateComp(p);
    }

    @Override
    void setupBrushStamp(PPoint p) {
        // no setup is necessary for shape brushes
    }
}
