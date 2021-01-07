/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.tools.shapes.StrokeType;
import pixelitor.tools.util.PPoint;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;

/**
 * A brush based on the JHLabs WobbleStroke
 */
public class WobbleBrush extends StrokeBrush {
    public WobbleBrush(double radius) {
        super(radius, StrokeType.WOBBLE);
    }

    @Override
    public double getMaxEffectiveRadius() {
        return 5.0 + super.getMaxEffectiveRadius() * 1.5; // can be bigger because of the randomness
    }

    @Override
    public void drawStartShape(PPoint p) {
        if (diameter != lastDiameter) {
            currentStroke = createStroke((float) diameter);
            lastDiameter = diameter;
        }

        targetG.setStroke(currentStroke);
        Shape circle = new Ellipse2D.Double(p.getImX(), p.getImY(), 0.1, 0.1);
        targetG.draw(circle);
    }

    @Override
    protected void repaintComp(PPoint p) {
        // make sure that it is repainted even if the radius is small
        double thickness = diameter;
        if (radius < 3) {
            thickness += 2;
        }
        comp.repaintRegion(previous, p, thickness);
    }
}
