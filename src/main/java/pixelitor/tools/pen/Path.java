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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import static pixelitor.tools.pen.PathBuilder.State.DRAGGING_THE_CONTROLS;
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
    private CurvePoint last;
    private CurvePoint lastButOne;

    public void addPoint(CurvePoint p) {
        curvePoints.add(p);
        lastButOne = last;
        last = p;
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
                        point.y
                );
            }
        }

        return path;
    }

    public void paintForBuilding(Graphics2D g, PathBuilder.State state) {
        if (state != INITIAL) {
            Shapes.drawVisible(g, toShape());
        }

        int numPoints = getNumPoints();

        if (state == DRAGGING_THE_CONTROLS) {
            if (numPoints > 1) {
                last.drawHandles(g, true, true);
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
            lastButOne.drawHandles(g, paintIn, true);
        }
    }

    public void paintForEditing(Graphics2D g) {
        Shapes.drawVisible(g, toShape());

        int numPoints = curvePoints.size();
        for (int i = 0; i < numPoints; i++) {
            boolean paintIn = true;
            boolean paintOut = true;
            if (i == 0) {
                // don't paint the in control handle for the first point
                paintIn = false;
            }
            if (i == numPoints - 1) {
                // don't paint the out control handle for the last point
                paintOut = false;
            }

            CurvePoint point = curvePoints.get(i);
            point.drawHandles(g, paintIn, paintOut);
        }
    }

    public DraggablePoint handleWasHit(int x, int y) {
        for (CurvePoint point : curvePoints) {
            DraggablePoint draggablePoint = point.handleWasHit(x, y);
            if (draggablePoint != null) {
                return draggablePoint;
            }
        }
        return null;
    }

    public void resetToInitialState() {
        assert curvePoints.size() == 1;
        curvePoints.remove(0);
        last = null;
        lastButOne = null;
    }

    public void icResized(ImageComponent ic) {
        for (CurvePoint point : curvePoints) {
            point.restoreCoordsFromImSpace(ic);
            point.ctrlIn.restoreCoordsFromImSpace(ic);
            point.ctrlOut.restoreCoordsFromImSpace(ic);
        }
    }

    public void dump() {
        for (int i = 0; i < curvePoints.size(); i++) {
            CurvePoint point = curvePoints.get(i);
            System.out.println("Point " + i + ":");
            System.out.println("    " + point.toString());
            System.out.println("    " + point.ctrlIn.toString());
            System.out.println("    " + point.ctrlOut.toString());
        }
    }

    public CurvePoint getPoint(int index) {
        return curvePoints.get(index);
    }
}
