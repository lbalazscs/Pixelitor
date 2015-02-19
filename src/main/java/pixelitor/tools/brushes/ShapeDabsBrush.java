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

import java.awt.Shape;
import java.awt.geom.AffineTransform;

public class ShapeDabsBrush extends DabsBrush {
    private final BrushShapeProvider shapeProvider;

    public ShapeDabsBrush(BrushShapeProvider shapeProvider, double spacingRatio) {
        super(spacingRatio, true);
        this.shapeProvider = shapeProvider;
    }

    @Override
    public void putDab(double x, double y, double theta) {
        if(angleAware) {
            Shape shape = shapeProvider.getShape(x - radius, y - radius, diameter, diameter);
            AffineTransform t = AffineTransform.getRotateInstance(theta, x, y);
            Shape transformedShape = t.createTransformedShape(shape);
            g.fill(transformedShape);
        } else {
            Shape shape = shapeProvider.getShape(x - radius, y - radius, diameter, diameter);
            g.fill(shape);
        }
    }

    @Override
    void setupBrushStamp() {
        // no setup is necessary for shape brushes
    }
}
