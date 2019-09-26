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

import pixelitor.tools.shapes.StrokeType;
import pixelitor.tools.util.PPoint;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

import static java.awt.BasicStroke.CAP_SQUARE;
import static java.awt.BasicStroke.JOIN_BEVEL;

/**
 * The "Squares" brush
 */
public class OutlineSquareBrush extends OutlineBrush {
    public OutlineSquareBrush(double radius, OutlineBrushSettings settings) {
        super(settings, radius, CAP_SQUARE, JOIN_BEVEL);
    }

    @Override
    public void drawStartShape(PPoint p) {
        double x = p.getImX();
        double y = p.getImY();
        Stroke savedStroke = targetG.getStroke();

        Shape rect = new Rectangle2D.Double(x - radius, y - radius, diameter, diameter);
        targetG.setStroke(StrokeType.OUTLINE.getInnerStroke());
        targetG.draw(rect);

        targetG.setStroke(savedStroke);
    }

    @Override
    public double getEffectiveRadius() {
        // multiply with sqrt 2
        return super.getEffectiveRadius() * 1.42;
    }
}