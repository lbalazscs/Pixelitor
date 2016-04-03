/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.utils.debug.DebugNode;

import java.awt.Stroke;

import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_ROUND;

/**
 * A Brush that uses a Stroke to draw
 */
public abstract class StrokeBrush extends AbstractBrush {
    private final StrokeType strokeType;
    private final int cap;
    private final int join;

    protected int lastDiameter = -1;
    protected Stroke currentStroke;

    protected StrokeBrush(int radius, StrokeType strokeType) {
        this(radius, strokeType, CAP_ROUND, JOIN_ROUND);
    }

    protected StrokeBrush(int radius, StrokeType strokeType, int cap, int join) {
        super(radius);
        this.strokeType = strokeType;
        this.cap = cap;
        this.join = join;
    }

    @Override
    public void onDragStart(double x, double y) {
        drawShape(x, y);
        updateComp(x, y);
        setPrevious(x, y);
    }

    @Override
    public void onNewMousePoint(double x, double y) {
        drawLine(previousX, previousY, x, y);
        updateComp(x, y);
        setPrevious(x, y);
    }

    abstract void drawShape(double x, double y);

    protected void drawLine(double startX, double startY, double endX, double endY) {
        int thickness = 2*radius;
        if(thickness != lastDiameter) {
            currentStroke = strokeType.getStroke(thickness, cap, join, null);
            lastDiameter = thickness;
        }

        targetG.setStroke(currentStroke);

        targetG.drawLine((int) startX, (int) startY, (int) endX, (int) endY);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addStringChild("Stroke Type", strokeType.toString());

        return node;
    }
}
