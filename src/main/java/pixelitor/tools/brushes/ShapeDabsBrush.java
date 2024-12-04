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

import pixelitor.layers.Drawable;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Shapes;

import java.awt.Graphics2D;
import java.awt.Shape;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * A {@link DabsBrush} that draws filled shapes as dabs.
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
    public void putDab(PPoint currentPoint, double angle) {
        double x = currentPoint.getImX();
        double y = currentPoint.getImY();
        ShapeType shapeType = ((ShapeDabsBrushSettings) settings).getShapeType();

        // create the base shape at the given position
        Shape baseShape = shapeType.createShape(x - radius, y - radius, diameter);

        // rotate around the center if necessary
        Shape finalShape = (angle == 0)
            ? baseShape
            : Shapes.rotate(baseShape, angle, x, y);

        targetG.fill(finalShape);
        repaintComp(currentPoint);
    }

    @Override
    void initBrushStamp(PPoint p) {
        // no setup is necessary for shape brushes
    }
}
