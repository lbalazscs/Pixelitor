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
import pixelitor.utils.debug.Ansi;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import static pixelitor.tools.pen.PathBuilder.State.DRAGGING_THE_CONTROL_OF_LAST;
import static pixelitor.tools.pen.PathBuilder.State.INITIAL;
import static pixelitor.tools.pen.PathBuilder.State.MOVING_TO_NEXT_CURVE_POINT;

/**
 * A path is a composite Bézier curve: a series of Bézier curves
 * joined end to end where the last point of one curve
 * coincides with the starting point of the next curve.
 * <p>
 * https://en.wikipedia.org/wiki/Composite_B%C3%A9zier_curve
 */
public class Path {
    private final List<CurvePoint> curvePoints = new ArrayList<>();
    // The curve point which is currently moving while the path is being built
    private CurvePoint moving;

    // The curve point which was added first
    // Relevant for closing
    private CurvePoint first;

    // The curve point which was finalized last
    // Relevant because its handle is dragged while the path is being built
    private CurvePoint last;

    private boolean closed = false;

    public void addFirstPoint(CurvePoint p) {
        curvePoints.add(p);
        first = p;
        last = p;
    }

    public void setMovingPoint(CurvePoint p) {
        moving = p;
    }

    public void finalizeMovingPoint(int x, int y) {
        moving.setLocation(x, y);
        moving.calcImCoords();
        curvePoints.add(moving);
        last = moving;
        moving = null;
    }

    public CurvePoint getMoving() {
        return moving;
    }

    public CurvePoint getFirst() {
        return first;
    }

    public CurvePoint getLast() {
        return last;
    }

    public int getNumPoints() {
        return curvePoints.size();
    }

    public Shape toShape() {
        // TODO cache, but one must be careful to
        // re-create after any editing
        GeneralPath path = new GeneralPath();

        for (int i = 0; i < curvePoints.size(); i++) {
            CurvePoint point = curvePoints.get(i);
            if (i == 0) {
                path.moveTo(point.x, point.y);
            } else {
                CurvePoint prevPoint = curvePoints.get(i - 1);
                path.curveTo(prevPoint.ctrlOut.x,
                        prevPoint.ctrlOut.y,
                        point.ctrlIn.x,
                        point.ctrlIn.y,
                        point.x,
                        point.y);
            }
        }
        if (moving != null) {
            path.curveTo(last.ctrlOut.x,
                    last.ctrlOut.y,
                    moving.ctrlIn.x,
                    moving.ctrlIn.y,
                    moving.x,
                    moving.y);
        }

        // for closed paths the last control point instance
        // is the same as the first one, so the closing is
        // included in the loop

        return path;
    }

    public void paintForBuilding(Graphics2D g, PathBuilder.State state) {
        // paint the shape
        if (state != INITIAL) {
            Shapes.drawVisible(g, toShape());
        }

        // paint the handles
        int numPoints = getNumPoints();
        if (state == DRAGGING_THE_CONTROL_OF_LAST) {
            if (numPoints > 1) {
                last.paintHandles(g, true, true);
            } else {
                // special case: only one point, no shape
                if (!last.samePositionAs(last.ctrlOut)) {
                    Line2D.Double line = new Line2D.Double(
                            last.x, last.y, last.ctrlOut.x, last.ctrlOut.y);
                    Shapes.drawVisible(g, line);
                }
            }
        } else if (state == MOVING_TO_NEXT_CURVE_POINT) {
            boolean paintIn = true;
            if (numPoints <= 2) {
                paintIn = false;
            }
            last.paintHandles(g, paintIn, true);
            if (first.isActive()) {
//                first.paintHandle(g);
                first.paintHandles(g, true, false);
            }
        }
    }

    public void paintForEditing(Graphics2D g) {
        // paint the shape
        Shapes.drawVisible(g, toShape());

        // paint the handles
        int numPoints = curvePoints.size();
        for (int i = 0; i < numPoints; i++) {
            boolean paintIn = true;
            boolean paintOut = true;
            if (!closed && (i == 0)) {
                // don't paint the in control handle for the first point
                paintIn = false;
            }
            if (!closed && (i == numPoints - 1)) {
                // don't paint the out control handle for the last point
                paintOut = false;
            }

            CurvePoint point = curvePoints.get(i);
            point.paintHandles(g, paintIn, paintOut);
        }
    }

    public DraggablePoint handleWasHit(int x, int y) {
        for (CurvePoint point : curvePoints) {
            DraggablePoint draggablePoint = point.handleOrCtrlHandleWasHit(x, y);
            if (draggablePoint != null) {
                return draggablePoint;
            }
        }
        return null;
    }

    public void resetToInitialState() {
        assert curvePoints.size() == 1;
        curvePoints.remove(0);
        moving = null;
        last = null;
    }

    public void close() {
        assert getNumPoints() > 2;
        curvePoints.add(first);
        moving = null; // can be ignored in this case
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public void icResized(ImageComponent ic) {
        for (CurvePoint point : curvePoints) {
            point.restoreCoordsFromImSpace(ic);
            point.ctrlIn.restoreCoordsFromImSpace(ic);
            point.ctrlOut.restoreCoordsFromImSpace(ic);
        }
    }

    public void dump() {
        int numPoints = curvePoints.size();
        if (numPoints == 0) {
            System.out.println("Empty path");
        }
        for (int i = 0; i < numPoints; i++) {
            CurvePoint point = curvePoints.get(i);
            System.out.print(Ansi.PURPLE + "Point " + i + ":" + Ansi.RESET);
            if (i != 0 && point == first) {
                System.out.println(" same as the first");
            } else {
                System.out.println();
                System.out.println("    " + point.toString());
                System.out.println("    " + point.ctrlIn.toString());
                System.out.println("    " + point.ctrlOut.toString());
            }
        }
    }

    public CurvePoint getPoint(int index) {
        return curvePoints.get(index);
    }
}
