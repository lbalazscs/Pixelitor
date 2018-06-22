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

import static pixelitor.tools.pen.AnchorPoint.Type.SMOOTH;
import static pixelitor.tools.pen.AnchorPoint.Type.SYMMETRIC;

/**
 * A point on a {@link Path}
 */
public class AnchorPoint extends DraggablePoint {
    final ControlPoint ctrlIn;
    final ControlPoint ctrlOut;

    enum Type {
        SYMMETRIC(true) {
            @Override
            void setLocationOfOtherControl(int x, int y, AnchorPoint anchor, ControlPoint other) {
                int dx = x - anchor.x;
                int dy = y - anchor.y;

                other.setLocationOnlyForThis(anchor.x - dx, anchor.y - dy);
            }
        },
        /**
         * Collinear, but the handles don't necessarily have the same length
         */
        SMOOTH(true) {
            @Override
            void setLocationOfOtherControl(int x, int y, AnchorPoint anchor, ControlPoint other) {
                // keep the distance, but adjust the angle to the new angle
                double dist = other.getRememberedDistanceFromAnchor();
                double newAngle = Math.PI + Math.atan2(y - anchor.y, x - anchor.x);

                // The magic experimental constants 0.65 try to compensate for the
                // rounding errors but they are not very good, because they
                // should be distance dependent.
                // The real solution for the distance drifting is the
                // "remembered distance from anchor", but this also
                // should help a bit the precision
                int newX = anchor.x + (int) (0.65 + dist * Math.cos(newAngle));
                int newY = anchor.y + (int) (0.65 + dist * Math.sin(newAngle));
                other.setLocationOnlyForThis(newX, newY);
            }
        },
        /**
         * The two control handles are totally independent
         */
        CUSP(false) {
            @Override
            void setLocationOfOtherControl(int x, int y, AnchorPoint anchor, ControlPoint other) {
                // do nothing: the control points are independent
            }
        };

        private final boolean dependent;

        Type(boolean dependent) {
            this.dependent = dependent;
        }

        public boolean isDependent() {
            return dependent;
        }

        abstract void setLocationOfOtherControl(int x, int y, AnchorPoint anchor, ControlPoint other);
    }

    private Type type = SYMMETRIC;

    private static final Color CURVE_COLOR = Color.WHITE;
    private static final Color CURVE_ACTIVE_COLOR = Color.RED;
    private static final Color CTRL_IN_COLOR = Color.WHITE;
    private static final Color CTRL_IN_ACTIVE_COLOR = Color.RED;
    private static final Color CTRL_OUT_COLOR = Color.WHITE;
    private static final Color CTRL_OUT_ACTIVE_COLOR = Color.RED;

    public AnchorPoint(int x, int y, ImageComponent ic) {
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
    public void setLocation(int x, int y) {
        int oldX = this.x;
        int oldY = this.y;
        super.setLocation(x, y);

        int dx = x - oldX;
        int dy = y - oldY;

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

    public Type getType() {
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
