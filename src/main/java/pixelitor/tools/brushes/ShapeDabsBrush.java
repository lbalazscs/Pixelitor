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

import pixelitor.layers.Drawable;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Shapes;

import java.awt.Graphics2D;
import java.awt.Shape;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * A {@link DabsBrush} where the dabs are filled shapes
 */
public class ShapeDabsBrush extends DabsBrush {
    public ShapeDabsBrush(double radius, ShapeDabsBrushSettings settings) {
        super(radius, settings);
    }

    @Override
    public void setTarget(Drawable dr, Graphics2D g) {
        super.setTarget(dr, g);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    }

    @Override
    public void putDab(PPoint p, double theta) {
        double x = p.getImX();
        double y = p.getImY();
        ShapeType shapeType = ((ShapeDabsBrushSettings) settings).getShapeType();
        if (theta != 0) {
            Shape shape = shapeType.createShape(x - radius, y - radius, diameter);
            Shape rotatedShape = Shapes.rotate(shape, theta, x, y);
            targetG.fill(rotatedShape);
        } else {
            Shape shape = shapeType.createShape(x - radius, y - radius, diameter);
            targetG.fill(shape);
        }
        repaintComp(p);
    }

    @Override
    void setupBrushStamp(PPoint p) {
        // no setup is necessary for shape brushes
    }
}
