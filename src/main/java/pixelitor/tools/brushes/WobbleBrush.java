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

import com.jhlabs.awt.WobbleStroke;
import pixelitor.tools.StrokeType;

import java.awt.geom.Ellipse2D;

/**
 *
 */
public class WobbleBrush extends StrokeBrush {
    private static final float SIZE_DIVIDING_FACTOR = 4.0f;

    public WobbleBrush() {
        super(StrokeType.WOBBLE);
    }

    @Override
    public void drawShape(int x, int y) {
        float smallThickness = diameter / SIZE_DIVIDING_FACTOR;

        if(diameter != lastDiameter) {
            lastStroke = new WobbleStroke(0.5f, smallThickness, smallThickness);
        }
        targetG.setStroke(lastStroke);

        Ellipse2D.Float circle = new Ellipse2D.Float(x + radius, y + radius, 0.1f, 0.1f);
        targetG.draw(circle);
    }

    @Override
    public void drawLine(int startX, int startY, int endX, int endY) {
        int savedRadius = radius;
        radius = (int) (radius / SIZE_DIVIDING_FACTOR);
        super.drawLine(startX, startY, endX, endY);
        radius = savedRadius;
    }
}
