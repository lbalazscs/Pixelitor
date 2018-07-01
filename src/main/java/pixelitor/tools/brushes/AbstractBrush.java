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

import pixelitor.Composition;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;

public abstract class AbstractBrush implements Brush {
    protected Graphics2D targetG;
    private Composition comp;

    protected int radius = AbstractBrushTool.DEFAULT_BRUSH_RADIUS;
    protected int diameter;
    protected PPoint previous;

    protected AbstractBrush(int radius) {
        setRadius(radius);
    }

    @Override
    public void setRadius(int radius) {
        this.radius = radius;
        this.diameter = 2 * radius;
    }

    @Override
    public void setTarget(Composition comp, Graphics2D g) {
        this.comp = comp;
        this.targetG = g;
    }

    // always call it before rememberPrevious!
    protected void updateComp(PPoint p) {
        comp.updateRegion(previous, p, diameter);
    }

    // always call it after updateComp!
    protected void rememberPrevious(PPoint p) {
        this.previous = p;
    }

    @Override
    public void onStrokeStart(PPoint p) {
        // when starting a new stroke, the previous
        // variables should not be set to 0, 0
        // because it causes unnecessary repainting
        rememberPrevious(p);
    }

    public int getRadius() {
        return radius;
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = new DebugNode("Brush", this);
        node.addClass();
        node.addInt("Radius", radius);
        node.addDouble("PreviousX", previous.getImX());
        node.addDouble("PreviousY", previous.getImY());

        return node;
    }
}
