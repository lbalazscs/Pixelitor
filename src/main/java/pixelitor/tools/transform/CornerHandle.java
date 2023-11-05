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

import pixelitor.gui.View;
import pixelitor.tools.util.DragDisplay;
import pixelitor.tools.util.PPoint;

import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.io.Serial;

/**
 * A corner handle of a {@link TransformBox}
 */
public class CornerHandle extends PositionHandle {
    @Serial
    private static final long serialVersionUID = 1L;

    // the two neighbors in the horizontal and vertical directions
    // and the edges that connect them to this corner handle
    private CornerHandle horNeighbor;
    private EdgeHandle horEdge;
    private CornerHandle verNeighbor;
    private EdgeHandle verEdge;

    // the original coordinates of the two neighbors before a drag
    private double verOrigX;
    private double verOrigY;
    private double horOrigX;
    private double horOrigY;

    // true for NW and NE
    private final boolean nextToRot;

    public CornerHandle(String name, TransformBox box, boolean nextToRot, PPoint pos,
                        View view, int cursorIndex, int cursorIndexIO) {
        super(name, box, pos, view, cursorIndex, cursorIndexIO);
        this.nextToRot = nextToRot;
    }

    public CornerHandle copy(TransformBox newBox) {
        // the position is based on image coordinates, because they are
        // always accurate, even in boxes in inactive shape layers
        PPoint pos;
        if (view != null) {
            pos = PPoint.fromIm(getImX(), getImY(), view);
        } else {
            pos = PPoint.lazyFromIm(getImX(), getImY(), view);
        }
        return new CornerHandle(name, newBox, nextToRot,
            pos, view, cursorIndex, cursorIndexIO);
    }

    public void setVerNeighbor(CornerHandle verNeighbor, EdgeHandle verEdge, boolean propagate) {
        this.verNeighbor = verNeighbor;
        this.verEdge = verEdge;
        if (propagate) {
            verNeighbor.setVerNeighbor(this, verEdge, false);
        }
    }

    public void setHorNeighbor(CornerHandle horNeighbor, EdgeHandle horEdge, boolean propagate) {
        this.horNeighbor = horNeighbor;
        this.horEdge = horEdge;
        if (propagate) {
            horNeighbor.setHorNeighbor(this, horEdge, false);
        }
    }

    @Override
    public void setLocation(double x, double y) {
        // This method doesn't move the related points because when
        // the point is transformed with a rotation transform,
        // AffineTransform.transform calls it, and expects the simple behavior.
        super.setLocation(x, y);
    }

    @Override
    public void mousePressed(double x, double y) {
        super.mousePressed(x, y);

        verOrigX = verNeighbor.getX();
        verOrigY = verNeighbor.getY();
        horOrigX = horNeighbor.getX();
        horOrigY = horNeighbor.getY();
    }

    @Override
    public void mouseDragged(double x, double y) {
        // The angle can change by 180 degrees
        // when the box is turned "inside out"
        box.recalcAngle();

        double dx = x - dragStartX;
        double dy = y - dragStartY;
        double newX = origX + dx;
        double newY = origY + dy;
        setLocation(newX, newY);

        // calculate the deltas in the original coordinate system
        double odx = dx * cos + dy * sin;
        double ody = -dx * sin + dy * cos;

        // the vertical neighbor is moved only by odx
        verNeighbor.setLocation(verOrigX + odx * cos, verOrigY + odx * sin);

        // the horizontal neighbor is moved only by ody
        horNeighbor.setLocation(horOrigX - ody * sin, horOrigY + ody * cos);

        box.cornerHandlesMoved();
    }

    private Point2D getHorHalfPoint() {
        // as this is used for placing the drag display, take
        // the rotation handle into account: for the NW-NE edge
        // return the rotation location instead of the edge center
        if (nextToRot && horNeighbor.nextToRot) {
            return box.getRot();
        }

        return horEdge;
    }

    private Point2D getVerHalfPoint() {
        return verEdge;
    }

    private Direction getHorEdgeDirection() {
        return horEdge.getDirection();
    }

    private Direction getVerEdgeDirection() {
        return verEdge.getDirection();
    }

    @Override
    protected void drawDragDisplays(DragDisplay dd, Dimension2D imSize) {
        drawWidthDisplay(dd, imSize);
        drawHeightDisplay(dd, imSize);
    }

    public void drawWidthDisplay(DragDisplay dd, Dimension2D imSize) {
        Direction horEdgeDirection = getHorEdgeDirection();
        String widthString = DragDisplay.getWidthDisplayString(imSize.getWidth());
        Point2D horHalf = getHorHalfPoint();
        float horX = (float) (horHalf.getX() + horEdgeDirection.dx);
        float horY = (float) (horHalf.getY() + horEdgeDirection.dy);
        dd.drawOneLine(widthString, horX, horY);
    }

    public void drawHeightDisplay(DragDisplay dd, Dimension2D imSize) {
        Direction verEdgeDirection = getVerEdgeDirection();
        String heightString = DragDisplay.getHeightDisplayString(imSize.getHeight());
        Point2D verHalf = getVerHalfPoint();
        float verX = (float) (verHalf.getX() + verEdgeDirection.dx);
        float verY = (float) (verHalf.getY() + verEdgeDirection.dy);
        dd.drawOneLine(heightString, verX, verY);
    }
}
