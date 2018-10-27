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

package pixelitor.tools.transform;

import pixelitor.gui.View;
import pixelitor.tools.util.DragDisplay;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.utils.Shapes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;

import static pixelitor.tools.util.DragDisplay.BG_WIDTH_PIXEL;

/**
 * A corner handle of a {@link TransformBox}
 */
public class TransformHandle extends DraggablePoint {
    private final TransformBox box;

    // the two neighbors in the horizontal and vertical directions
    private TransformHandle horNeighbor;
    private TransformHandle verNeighbor;

    // the original coordinates of the two neighbors before a drag
    private double verOrigX;
    private double verOrigY;
    private double horOrigX;
    private double horOrigY;

    // The sine and cosine of the current rotation angle
    private double sin;
    private double cos;

    private final int cursorIndex;
    private final int cursorIndexIO;

    private Direction direction;

    // true for NW and NE
    private final boolean nextToRot;

    public TransformHandle(String name, TransformBox box, boolean nextToRot, Point2D pos,
                           View view, Color c, int cursorIndex, int cursorIndexIO) {
        super(name, pos.getX(), pos.getY(), view, c, Color.RED);
        this.box = box;
        this.nextToRot = nextToRot;
        this.cursorIndex = cursorIndex;
        this.cursorIndexIO = cursorIndexIO;
    }

    public void setVerNeighbor(TransformHandle verNeighbor, boolean propagate) {
        this.verNeighbor = verNeighbor;
        if (propagate) {
            verNeighbor.setVerNeighbor(this, false);
        }
    }

    public void setHorNeighbor(TransformHandle horNeighbor, boolean propagate) {
        this.horNeighbor = horNeighbor;
        if (propagate) {
            horNeighbor.setHorNeighbor(this, false);
        }
    }

    @Override
    public void setLocation(double x, double y) {
        // this method does not move the related points because when
        // the point is transformed with a rotation transform,
        // AffineTransform.transform calls it, and expects the simple behavior
        super.setLocation(x, y);

        // the image space coordinates need to be updated continuously
        // because the transform calculations are based on them
        calcImCoords();
    }

    @Override
    public void mousePressed(double x, double y) {
        super.mousePressed(x, y); // sets dragStartX, dragStartY
        sin = box.getSin();
        cos = box.getCos();

        verOrigX = verNeighbor.getX();
        verOrigY = verNeighbor.getY();
        horOrigX = horNeighbor.getX();
        horOrigY = horNeighbor.getY();
    }

    @Override
    public void mouseDragged(double x, double y) {
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

        box.handlePositionsChanged();
    }

    @Override
    public void mouseReleased(double x, double y) {
        super.mouseReleased(x, y);

        // the angle can change by 180 degrees
        // when the box is turned "inside out"
        box.recalcAngle();
    }

    /**
     * Determines the direction as the box is rotating
     */
    public void recalcDirection() {
        int offset;
        if (box.areCornersInDefaultOrder()) {
            offset = cursorIndex + box.getCursorOffset();
        } else {
            offset = cursorIndexIO + box.getCursorOffset();
        }
        direction = Direction.atOffset(offset);
        cursor = direction.getCursor();
    }

    private Point2D getHorHalfPoint() {
        // as this is used for placing the drag display, take
        // the rotation handle into account: for the NW-NE edge
        // return the rotation location instead of the edge center
        if (nextToRot && horNeighbor.nextToRot) {
            return box.getRot();
        }

        return Shapes.calcCenter(this, horNeighbor);
    }

    private Point2D getVerHalfPoint() {
        return Shapes.calcCenter(this, verNeighbor);
    }

    private Direction getHorEdgeDirection() {
        return direction.getDirectionBetween(horNeighbor.direction);
    }

    private Direction getVerEdgeDirection() {
        return direction.getDirectionBetween(verNeighbor.direction);
    }

    @Override
    public void paintHandle(Graphics2D g) {
        super.paintHandle(g);

        if (isActive()) {
            drawDragDisplays(g);
        }
    }

    private void drawDragDisplays(Graphics2D g) {
        Dimension2D size = box.getRotatedImSize();
        DragDisplay dd = new DragDisplay(g, BG_WIDTH_PIXEL);

        Direction horEdgeDirection = getHorEdgeDirection();
        String widthString = DragDisplay.getWidthDisplayString(size.getWidth());
        Point2D horHalf = getHorHalfPoint();
        float horX = (float) (horHalf.getX() + horEdgeDirection.dx);
        float horY = (float) (horHalf.getY() + horEdgeDirection.dy);
        dd.drawOneLine(widthString, horX, horY);

        Direction verEdgeDirection = getVerEdgeDirection();
        String heightString = DragDisplay.getHeightDisplayString(size.getHeight());
        Point2D verHalf = getVerHalfPoint();
        float verX = (float) (verHalf.getX() + verEdgeDirection.dx);
        float verY = (float) (verHalf.getY() + verEdgeDirection.dy);
        dd.drawOneLine(heightString, verX, verY);

        dd.finish();
    }
}
