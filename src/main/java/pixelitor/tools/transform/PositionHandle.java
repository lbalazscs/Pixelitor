/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.transform;

import pixelitor.gui.View;
import pixelitor.tools.util.DragDisplay;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;

import java.awt.Graphics2D;
import java.awt.geom.Dimension2D;
import java.io.Serial;

import static pixelitor.tools.util.DragDisplay.BG_WIDTH_PIXELS;

/**
 * The common functionality of corner and edge handles in a {@link TransformBox}.
 */
public abstract class PositionHandle extends DraggablePoint {
    @Serial
    private static final long serialVersionUID = 1L;

    protected final TransformBox box;

    // The sine and cosine of the current rotation angle
    protected double sin;
    protected double cos;

    // the angle-independent (at rotation = 0) parts of the cursor
    // offset for the normal transform box and the "inside out" one
    protected final int cursorIndex;
    protected final int cursorIndexIO;

    private Direction direction;

    protected PositionHandle(String name, TransformBox box,
                             PPoint p, View view,
                             int cursorIndex, int cursorIndexIO) {
        super(name, p, view);
        this.box = box;
        this.cursorIndex = cursorIndex;
        this.cursorIndexIO = cursorIndexIO;
    }

    @Override
    public void mousePressed(double x, double y) {
        super.mousePressed(x, y); // sets dragStartX, dragStartY

        sin = box.getSin();
        cos = box.getCos();
    }

    /**
     * Determines the direction as the box is rotating
     */
    public void recalcDirection(boolean isInsideOut, int cursorOffset) {
        int offset;
        if (isInsideOut) {
            offset = cursorIndexIO + cursorOffset;
        } else {
            // the corners are in default order
            offset = cursorIndex + cursorOffset;
        }
        direction = Direction.atOffset(offset);
        cursor = direction.getCursor();
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public void paintHandle(Graphics2D g) {
        super.paintHandle(g);

        if (isActive()) {
            Dimension2D size = box.getRotatedImSize();
            DragDisplay dd = new DragDisplay(g, BG_WIDTH_PIXELS);

            drawDragDisplays(dd, size);

            dd.cleanup();
        }
    }

    protected abstract void drawDragDisplays(DragDisplay dd, Dimension2D imSize);
}
