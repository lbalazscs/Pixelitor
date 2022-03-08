/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

    protected double lastDiameter = -1;
    protected Stroke currentStroke;

    protected StrokeBrush(double radius, StrokeType strokeType) {
        this(radius, strokeType, CAP_ROUND, JOIN_ROUND);
    }

    protected StrokeBrush(double radius, StrokeType strokeType, int cap, int join) {
        super(radius);
        this.strokeType = strokeType;
        this.cap = cap;
        this.join = join;
    }

    @Override
    public void startAt(PPoint p) {
        super.startAt(p);
        drawStartShape(p);
        repaintComp(p);
    }

    @Override
    public void continueTo(PPoint p) {
        assert previous != null;

        drawLine(previous, p);
        repaintComp(p);
        rememberPrevious(p);
    }

    /**
     * The ability to draw something sensible immediately
     * when the user has just clicked but didn't drag the mouse yet.
     */
    abstract void drawStartShape(PPoint p);

    /**
     * Connects the two points with a line, using the stroke
     */
    protected void drawLine(PPoint start, PPoint end) {
        if (diameter != lastDiameter) {
            currentStroke = createStroke((float) diameter);
            lastDiameter = diameter;
        }

        targetG.setStroke(currentStroke);
        start.drawLineTo(end, targetG);
    }

    protected Stroke createStroke(float thickness) {
        return strokeType.createStroke(thickness, cap, join, null);
    }

    @Override
    public double getPreferredSpacing() {
        return 0;
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();

        node.addAsString("stroke type", strokeType);

        return node;
    }
}
