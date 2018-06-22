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

package pixelitor.tools.pen;

import pixelitor.gui.ImageComponent;
import pixelitor.tools.DraggablePoint;
import pixelitor.utils.Shapes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;

import static pixelitor.tools.pen.AnchorPointType.SMOOTH;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;

/**
 * A point on a {@link Path}
 */
public class AnchorPoint extends DraggablePoint {
    final ControlPoint ctrlIn;
    final ControlPoint ctrlOut;

    private AnchorPointType type = SYMMETRIC;

    private static final Color CURVE_COLOR = Color.WHITE;
    private static final Color CURVE_ACTIVE_COLOR = Color.RED;
    private static final Color CTRL_IN_COLOR = Color.WHITE;
    private static final Color CTRL_IN_ACTIVE_COLOR = Color.RED;
    private static final Color CTRL_OUT_COLOR = Color.WHITE;
    private static final Color CTRL_OUT_ACTIVE_COLOR = Color.RED;

    public AnchorPoint(double x, double y, ImageComponent ic) {
        super("curve", x, y, ic, CURVE_COLOR, CURVE_ACTIVE_COLOR);

        ctrlIn = new ControlPoint("ctrlIn", x, y, ic, this, CTRL_IN_COLOR, CTRL_IN_ACTIVE_COLOR);
        ctrlOut = new ControlPoint("ctrlOut", x, y, ic, this, CTRL_OUT_COLOR, CTRL_OUT_ACTIVE_COLOR);
        ctrlIn.setSibling(ctrlOut);
        ctrlOut.setSibling(ctrlIn);
    }

    public void paintHandles(Graphics2D g, boolean paintIn, boolean paintOut) {
        if (paintIn && !this.samePositionAs(ctrlIn)) {
            Line2D.Double lineIn = new Line2D.Double(x, y, ctrlIn.x, ctrlIn.y);
            Shapes.drawVisible(g, lineIn);
            ctrlIn.paintHandle(g);
        }
        if (paintOut && !this.samePositionAs(ctrlOut)) {
            Line2D.Double lineOut = new Line2D.Double(x, y, ctrlOut.x, ctrlOut.y);
            Shapes.drawVisible(g, lineOut);
            ctrlOut.paintHandle(g);
        }

        paintHandle(g);
    }

    @Override
    public void setLocation(double x, double y) {
        double oldX = this.x;
        double oldY = this.y;
        super.setLocation(x, y);

        double dx = x - oldX;
        double dy = y - oldY;

        ctrlOut.translateOnlyThis(dx, dy);
        ctrlIn.translateOnlyThis(dx, dy);
    }

    public DraggablePoint handleOrCtrlHandleWasHit(int x, int y) {
        if (handleContains(x, y)) {
            return this;
        }
        if (ctrlIn.handleContains(x, y)) {
            return ctrlIn;
        }
        if (ctrlOut.handleContains(x, y)) {
            return ctrlOut;
        }
        return null;
    }

    public AnchorPointType getType() {
        return type;
    }

    @Override
    protected void afterMouseReleasedActions() {
        calcImCoords();
        ctrlIn.calcImCoords();
        ctrlOut.calcImCoords();
    }

    public void changeTypeFromSymmetricToSmooth() {
        if (type == SYMMETRIC) {
            type = SMOOTH;
        }
    }
}
