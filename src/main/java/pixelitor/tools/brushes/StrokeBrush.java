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

/**
 * A Brush that uses a Stroke to draw
 */
public abstract class StrokeBrush extends AbstractBrush {
    private final StrokeType strokeType;
    private final int cap;
    private final int join;

    int lastDiameter = -1;
    Stroke lastStroke;

    protected StrokeBrush(StrokeType strokeType) {
        this(strokeType, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    protected StrokeBrush(StrokeType strokeType, int cap, int join) {
        this.strokeType = strokeType;
        this.cap = cap;
        this.join = join;
    }

    @Override
    public void onDragStart(int x, int y) {
        drawShape(x, y);
        setPreviousCoordinates(x, y);
    }

    @Override
    public void onNewMousePoint(int x, int y) {
        drawLine(previousX, previousY, x, y);
        setPreviousCoordinates(x, y);
    }

    abstract void drawShape(int x, int y);

    public void drawLine(int startX, int startY, int endX, int endY) {
        int thickness = 2*radius;
        if(thickness != lastDiameter) {
            lastStroke = strokeType.getStroke(thickness, cap, join, null);
            lastDiameter = thickness;
        }

        g.setStroke(lastStroke);

        g.drawLine(startX, startY, endX, endY);
    }

    @Override
    public void reset() {

    }
}
