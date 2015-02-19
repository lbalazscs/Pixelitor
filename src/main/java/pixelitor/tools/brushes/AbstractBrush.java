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

import pixelitor.tools.AbstractBrushTool;

import java.awt.Graphics2D;

public abstract class AbstractBrush implements Brush {
    protected Graphics2D g;
    protected int radius = AbstractBrushTool.DEFAULT_BRUSH_RADIUS;
    protected int diameter;
    protected int previousX;
    protected int previousY;

    @Override
    public void setRadius(int radius) {
        this.radius = radius;
        this.diameter = 2 * radius;
    }

    @Override
    public void setTargetGraphics(Graphics2D g) {
        this.g = g;
    }

    protected void setPreviousCoordinates(int previousX, int previousY) {
        this.previousX = previousX;
        this.previousY = previousY;
    }
}
