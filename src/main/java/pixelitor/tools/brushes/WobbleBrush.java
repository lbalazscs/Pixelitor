/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.tools.StrokeType;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;

/**
 *
 */
public class WobbleBrush extends StrokeBrush {
    private static final float SIZE_DIVIDING_FACTOR = 4.0f;

    public WobbleBrush(int radius) {
        super(radius, StrokeType.WOBBLE);
    }

    @Override
    public void drawShape(double x, double y) {
        float smallThickness = diameter / SIZE_DIVIDING_FACTOR;

        if(diameter != lastDiameter) {
            currentStroke = new WobbleStroke(0.5f, smallThickness, smallThickness);
        }
        targetG.setStroke(currentStroke);

        Shape circle = new Ellipse2D.Double(x, y, 0.1, 0.1);
        targetG.draw(circle);
    }

    @Override
    public void drawLine(double startX, double startY, double endX, double endY) {
        int savedRadius = radius;
        radius = (int) (radius / SIZE_DIVIDING_FACTOR);
        super.drawLine(startX, startY, endX, endY);
        radius = savedRadius;
    }
}
