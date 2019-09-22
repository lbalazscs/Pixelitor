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

import java.awt.geom.Ellipse2D;

/**
 * A brush that paints with vector-based circles
 */
public class HardBrush extends StrokeBrush {

    public HardBrush(double radius) {
        super(radius, StrokeType.BASIC);
    }

    @Override
    public void drawStartShape(PPoint p) {
        double x = p.getImX();
        double y = p.getImY();

        Ellipse2D.Double circle = new Ellipse2D.Double(x - radius, y - radius, diameter, diameter);
        targetG.fill(circle);
    }
}
