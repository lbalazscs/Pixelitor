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
import java.awt.geom.PathIterator;
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
    private final List<AnchorPoint> anchorPoints = new ArrayList<>();
    // The curve point which is currently moving while the path is being built
    private AnchorPoint moving;

    // The curve point which was added first
    // Relevant for closing
    private AnchorPoint first;

    // The curve point which was finalized last
    // Relevant because its handle is dragged while the path is being built
    private AnchorPoint last;

    private boolean closed = false;

    public void addFirstPoint(AnchorPoint p) {
        anchorPoints.add(p);
        first = p;
        last = p;
    }

    public void setMovingPoint(AnchorPoint p) {
        moving = p;
    }

    public void finalizeMovingPoint(int x, int y) {
        moving.setLocation(x, y);
        moving.calcImCoords();
        anchorPoints.add(moving);
        last = moving;
        moving = null;
    }

    public AnchorPoint getMoving() {
        return moving;
    }

    public AnchorPoint getFirst() {
        return first;
    }

    public AnchorPoint getLast() {
        return last;
    }

    public int getNumPoints() {
        return anchorPoints.size();
    }

    public Shape toComponentSpaceShape() {
        // TODO cache, but one must be careful to
        // re-create after any editing
        GeneralPath path = new GeneralPath();

        if (first == null) { // TODO maybe a Path should be created only when there is at least one node
            return path;
        }

        path.moveTo(first.x, first.y);
        AnchorPoint prevPoint = first;

        for (int i = 1; i < anchorPoints.size(); i++) {
            AnchorPoint point = anchorPoints.get(i);
            path.curveTo(prevPoint.ctrlOut.x,
                    prevPoint.ctrlOut.y,
                    point.ctrlIn.x,
                    point.ctrlIn.y,
                    point.x,
                    point.y);
            prevPoint = point;
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

    public Shape toImageSpaceShape() {
        GeneralPath path = new GeneralPath();

        if (first == null) { // TODO maybe a Path should be created only when there is at least one node
            return path;
        }

        path.moveTo(first.imX, first.imY);
        AnchorPoint prevPoint = first;

        for (int i = 1; i < anchorPoints.size(); i++) {
            AnchorPoint point = anchorPoints.get(i);
            path.curveTo(prevPoint.ctrlOut.imX,
                    prevPoint.ctrlOut.imY,
                    point.ctrlIn.imX,
                    point.ctrlIn.imY,
                    point.imX,
                    point.imY);
            prevPoint = point;
        }
        assert moving == null;
        return path;
    }

    public static Path fromShape(Shape shape, ImageComponent ic) {
        Path path = new Path();
        PathIterator it = shape.getPathIterator(null);
        float[] coords = new float[6];
        while (!it.isDone()) {
            int type = it.currentSegment(coords);
            float x = coords[0];
            float y = coords[1];
            float xx = coords[2];
            float yy = coords[3];
            float xxx = coords[4];
            float yyy = coords[5];

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    System.out.printf("Path::fromShape: SEG_MOVETO x = %.2f, y = %.2f%n", x, y);
                    path.addFirstPoint(new AnchorPoint(x, y, ic));
                    break;
                case PathIterator.SEG_LINETO:
                    System.out.printf("Path::fromShape: SEG_LINETO x = %.2f, y = %.2f%n", x, y);
                    break;
                case PathIterator.SEG_QUADTO:
                    System.out
                            .printf("Path::fromShape: SEG_QUADTO x = %.2f, y = %.2f, xx = %.2f, yy = %.2f%n", x, y, xx, yy);
                    break;
                case PathIterator.SEG_CUBICTO:
                    System.out
                            .printf("Path::fromShape: SEG_CUBICTO x = %.2f, y = %.2f, xx = %.2f, yy = %.2f, xxx = %.2f, yyy = %.2f%n", x, y, xx, yy, xxx, yyy);
                    break;
                case PathIterator.SEG_CLOSE:
                    System.out.printf("Path::fromShape: SEG_CLOSE%n");
                    break;
                default:
                    throw new IllegalArgumentException("type = " + type);
            }

            it.next();
        }
        return path;
    }

    public void paintForBuilding(Graphics2D g, PathBuilder.State state) {
        // paint the shape
        if (state != INITIAL) {
            Shapes.drawVisible(g, toComponentSpaceShape());
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
        Shapes.drawVisible(g, toComponentSpaceShape());

        // paint the handles
        int numPoints = anchorPoints.size();
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

            AnchorPoint point = anchorPoints.get(i);
            point.paintHandles(g, paintIn, paintOut);
        }
    }

    public DraggablePoint handleWasHit(int x, int y) {
        for (AnchorPoint point : anchorPoints) {
            DraggablePoint draggablePoint = point.handleOrCtrlHandleWasHit(x, y);
            if (draggablePoint != null) {
                return draggablePoint;
            }
        }
        return null;
    }

    public void resetToInitialState() {
        assert anchorPoints.size() == 1;
        anchorPoints.remove(0);
        moving = null;
        last = null;
    }

    public void close() {
        assert getNumPoints() > 2;
        anchorPoints.add(first);
        moving = null; // can be ignored in this case
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public void icResized(ImageComponent ic) {
        for (AnchorPoint point : anchorPoints) {
            point.restoreCoordsFromImSpace(ic);
            point.ctrlIn.restoreCoordsFromImSpace(ic);
            point.ctrlOut.restoreCoordsFromImSpace(ic);
        }
    }

    public void dump() {
        int numPoints = anchorPoints.size();
        if (numPoints == 0) {
            System.out.println("Empty path");
        }
        for (int i = 0; i < numPoints; i++) {
            AnchorPoint point = anchorPoints.get(i);
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

    public AnchorPoint getPoint(int index) {
        return anchorPoints.get(index);
    }

    public void changeTypeFromSymmetricToSmooth() {
        for (AnchorPoint point : anchorPoints) {
            point.changeTypeFromSymmetricToSmooth();
        }
    }

}
