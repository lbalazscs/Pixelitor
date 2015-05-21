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

    int lastDiameter = -1;
    Stroke lastStroke;

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
    public void onDragStart(int x, int y) {
        drawShape(x, y);
        updateComp(x, y);
        setPrevious(x, y);
    }

    @Override
    public void onNewMousePoint(int x, int y) {
        drawLine(previousX, previousY, x, y);
        updateComp(x, y);
        setPrevious(x, y);
    }

    abstract void drawShape(int x, int y);

    protected void drawLine(int startX, int startY, int endX, int endY) {
        int thickness = 2*radius;
        if(thickness != lastDiameter) {
            lastStroke = strokeType.getStroke(thickness, cap, join, null);
            lastDiameter = thickness;
        }

        targetG.setStroke(lastStroke);

        targetG.drawLine(startX, startY, endX, endY);
    }
}
