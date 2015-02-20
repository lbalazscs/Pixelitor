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
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

/**
 * The "Squares" brush
 */
public class OutlineSquareBrush extends StrokeBrush {
    public OutlineSquareBrush() {
        super(StrokeType.OUTLINE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
    }

    @Override
    public void drawShape(int x, int y) {
        Rectangle2D.Float rectangle = new Rectangle2D.Float(x - radius, y - radius, diameter, diameter);
        Stroke saveStroke = targetG.getStroke();
        targetG.setStroke(StrokeType.OUTLINE.getInnerStroke());
        targetG.draw(rectangle);
        targetG.setStroke(saveStroke);
    }
}