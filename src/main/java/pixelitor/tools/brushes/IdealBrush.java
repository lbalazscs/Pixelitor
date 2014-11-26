/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools.brushes;

import pixelitor.tools.StrokeType;

import java.awt.Graphics2D;

/**
 *
 */
public class IdealBrush extends StrokeBrush {

    public IdealBrush() {
        super(StrokeType.BASIC);
    }

    @Override
    public void drawPoint(Graphics2D g, int x, int y, int radius) {
        int diameter = 2 * radius;
        g.fillOval(x - radius, y - radius, (int) diameter, (int) diameter);
    }
}
