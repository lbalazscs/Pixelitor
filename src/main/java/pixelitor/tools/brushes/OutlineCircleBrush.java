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

import pixelitor.tools.StrokeType;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_ROUND;

/**
 * The "Circles" brush
 */
public class OutlineCircleBrush extends StrokeBrush {
    public OutlineCircleBrush(int radius) {
        super(radius, StrokeType.OUTLINE, CAP_ROUND, JOIN_ROUND);
    }

    @Override
    public void drawShape(double x, double y) {
        Shape circle = new Ellipse2D.Double(x - radius, y - radius, diameter, diameter);
        Stroke saveStroke = targetG.getStroke();
        targetG.setStroke(StrokeType.OUTLINE.getInnerStroke());
        targetG.draw(circle);
        targetG.setStroke(saveStroke);
    }
}