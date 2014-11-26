/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools.brushes;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

public class AngledShapeBrush extends UniformDabsBrush {
    private BrushShapeProvider shapeProvider;

    public AngledShapeBrush(BrushShapeProvider shapeProvider, double spacingRatio) {
        super(spacingRatio, true);
        this.shapeProvider = shapeProvider;
    }

    @Override
    void setupBrushStamp(Graphics2D g, float diameter) {
        // no setup is necessary for shape brushes
    }

    // TODO called only for the first point!
    @Override
    public void drawPoint(Graphics2D g, int x, int y, int radius) {
        // call super to set prevX, prevY
        super.drawPoint(g, x, y, radius);

        if(angleAware) {
            return;
        }

        int size = 2 * radius;
        Shape shape = shapeProvider.getShape(x - radius, y - radius, size, size);
        g.fill(shape);
    }

    // TODO this works but it is ugly, and the drawPoint is never called from drawLine
    @Override
    public void drawPointWithAngle(Graphics2D g, int x, int y, int radius, double theta) {
        int size = 2 * radius;
        Shape shape = shapeProvider.getShape(x - radius, y - radius, size, size);
        AffineTransform t = AffineTransform.getRotateInstance(theta, x, y);
        Shape transformedShape = t.createTransformedShape(shape);
        g.fill(transformedShape);
    }
}
