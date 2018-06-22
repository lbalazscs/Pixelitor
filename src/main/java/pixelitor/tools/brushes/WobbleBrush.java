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

import com.jhlabs.awt.WobbleStroke;
import pixelitor.tools.PPoint;
import pixelitor.tools.StrokeType;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;

/**
 * A brush based on the JHLabs WobbleStroke
 */
public class WobbleBrush extends StrokeBrush {
    private static final float SIZE_DIVIDING_FACTOR = 4.0f;

    public WobbleBrush(int radius) {
        super(radius, StrokeType.WOBBLE);
    }

    @Override
    public void drawStartShape(PPoint p) {
        double x = p.getImX();
        double y = p.getImY();
        float smallThickness = diameter / SIZE_DIVIDING_FACTOR;

        if(diameter != lastDiameter) {
            currentStroke = new WobbleStroke(0.5f, smallThickness, smallThickness);
        }
        targetG.setStroke(currentStroke);

        Shape circle = new Ellipse2D.Double(x, y, 0.1, 0.1);
        targetG.draw(circle);
    }

    @Override
    public void drawLine(PPoint start, PPoint end) {
        int savedRadius = radius;
        radius = (int) (radius / SIZE_DIVIDING_FACTOR);

        super.drawLine(start, end);

        radius = savedRadius;
    }
}
