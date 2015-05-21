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

import pixelitor.Composition;
import pixelitor.tools.AbstractBrushTool;

import java.awt.Graphics2D;

public abstract class AbstractBrush implements Brush {
    protected Graphics2D targetG;
    private Composition comp;

    protected int radius = AbstractBrushTool.DEFAULT_BRUSH_RADIUS;
    protected int diameter;
    protected int previousX;
    protected int previousY;

    protected AbstractBrush(int radius) {
        setRadius(radius);
    }

    @Override
    public void setRadius(int radius) {
//        System.out.println("AbstractBrush::setRadius: radius = " + radius
//                + ", class = " + this.getClass().getName());
        this.radius = radius;
        this.diameter = 2 * radius;
    }

    @Override
    public void setTarget(Composition comp, Graphics2D g) {
        this.comp = comp;
        this.targetG = g;
    }

    // call before setPrevious
    protected void updateComp(int x, int y) {
        comp.updateRegion(previousX, previousY, x, y, diameter);
    }

    // call after updateComp
    protected void setPrevious(int x, int y) {
        this.previousX = x;
        this.previousY = y;
    }

    public int getRadius() {
        return radius;
    }
}
