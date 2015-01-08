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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

/**
 * The "Circles" brush
 */
public class OutlineCircleBrush extends StrokeBrush {
    public OutlineCircleBrush() {
        super(StrokeType.OUTLINE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    @Override
    public void drawPoint(Graphics2D g, int x, int y, int radius) {
        int diameter = 2 * radius;
        Ellipse2D.Float circle = new Ellipse2D.Float(x - radius, y - radius, diameter, diameter);
        Stroke saveStroke = g.getStroke();
        g.setStroke(StrokeType.OUTLINE.getInnerStroke());
        g.draw(circle);
        g.setStroke(saveStroke);
    }
}