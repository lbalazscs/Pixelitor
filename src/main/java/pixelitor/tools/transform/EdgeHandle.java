/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.tools.util.DragDisplay;
import pixelitor.tools.util.PPoint;

import java.awt.geom.Dimension2D;
import java.io.Serial;

/**
 * An edge handle of a {@link TransformBox}.
 * It is always at the midpoint between two {@link CornerHandle}s
 */
public class EdgeHandle extends PositionHandle {
    @Serial
    private static final long serialVersionUID = 1L;

    private final CornerHandle ch1;
    private final CornerHandle ch2;

    private final boolean horizontal;

    // the original coordinates of the two corners before a drag
    private double ch1OrigX;
    private double ch1OrigY;
    private double ch2OrigX;
    private double ch2OrigY;

    public EdgeHandle(String name, TransformBox box,
                      CornerHandle ch1, CornerHandle ch2,
                      boolean horizontal, int cursorIndex, int cursorIndexIO) {
        super(name, box, PPoint.halfPointBetween(ch1, ch2), ch1.getView(),
            cursorIndex, cursorIndexIO);
        this.ch1 = ch1;
        this.ch2 = ch2;
        this.horizontal = horizontal;
    }

    public void updatePosition() {
        setLocation(
            (ch1.getX() + ch2.getX()) / 2.0,
            (ch1.getY() + ch2.getY()) / 2.0);
    }

    @Override
    public void mousePressed(double x, double y) {
        super.mousePressed(x, y);

        ch1OrigX = ch1.getX();
        ch1OrigY = ch1.getY();
        ch2OrigX = ch2.getX();
        ch2OrigY = ch2.getY();
    }

    @Override
    public void mouseDragged(double x, double y) {
        super.mouseDragged(x, y);

        // The angle can change by 180 degrees
        // when the box is turned "inside out"
        box.recalcAngle();

        double dx = x - dragStartX;
        double dy = y - dragStartY;

        // calculate the deltas in the original coordinate system
        double odx = dx * cos + dy * sin;
        double ody = -dx * sin + dy * cos;

        if (horizontal) {
            // the horizontal corners are moved only by ody
            ch1.setLocation(ch1OrigX - ody * sin, ch1OrigY + ody * cos);
            ch2.setLocation(ch2OrigX - ody * sin, ch2OrigY + ody * cos);
        } else {
            // the vertical corners are moved only by odx
            ch1.setLocation(ch1OrigX + odx * cos, ch1OrigY + odx * sin);
            ch2.setLocation(ch2OrigX + odx * cos, ch2OrigY + odx * sin);
        }

        box.cornerHandlesMoved();
    }

    @Override
    protected void drawDragDisplays(DragDisplay dd, Dimension2D imSize) {
        if (horizontal) {
            ch2.drawHeightDisplay(dd, imSize);
        } else {
            ch2.drawWidthDisplay(dd, imSize);
        }
    }
}
