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

package pixelitor.tools;

import pixelitor.ImageDisplay;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Represents the mouse drag on the image made by the user while using a tool
 * The coordinates are scaled from mouse coordinates to image coordinates to compensate the zoom level,
 * and also translated because the centered image does not necessarily start at 0, 0
 */
public class UserDrag {
    private int startX;
    private int startY;
    private int endX;
    private int endY;

    private boolean startFromCenter;

    private int oldEndX;
    private int oldEndY;

    private boolean constrainPoints = false;

    // the normal constructor
    public UserDrag() {
    }

    public UserDrag(int startX, int startY, int endX, int endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public void setStartFromMouseEvent(MouseEvent e, ImageDisplay ic) {
        startX = ic.componentXToImageSpace(e.getX());
        startY = ic.componentYToImageSpace(e.getY());
    }

    public void setEndFromMouseEvent(MouseEvent e, ImageDisplay ic) {
        endX = ic.componentXToImageSpace(e.getX());
        endY = ic.componentYToImageSpace(e.getY());

        if (constrainPoints) {
            int dx = Math.abs(endX - startX);
            int dy = Math.abs(endY - startY);

            if (dx > dy) {
                endY = startY;
            } else {
                endX = startX;
            }
        }
    }

    public void setConstrainPoints(boolean constrainPoints) {
        this.constrainPoints = constrainPoints;
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getStartXFromCenter() {
        if (startFromCenter) {
            return startX - (endX - startX);
        } else {
            return startX;
        }
    }

    public int getStartYFromCenter() {
        if (startFromCenter) {
            return startY - (endY - startY);
        } else {
            return startY;
        }
    }

    public int getEndX() {
        return endX;
    }

    public int getEndY() {
        return endY;
    }

    public Point2D.Float getStartPoint() {
        return new Point2D.Float(startX, startY);
    }

    public Point2D.Float getEndPoint() {
        return new Point2D.Float(endX, endY);
    }

    public float getDistance() {
        int dx = startX - endX;
        int dy = startY - endY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public double getStartDistanceFrom(double x, double y) {
        double dx = startX - x;
        double dy = startY - y;
        return Math.sqrt(dx * dx + dy * dy);
    }


    public double getDrawAngle() {
        return Math.atan2(endX - startX, endY - startY); //  between -PI and PI
    }

    public double getAngleFromStartTo(double x, double y) {
        return Math.atan2(x - startX, y - startY);
    }

    public void drawLine(Graphics g) {
        g.drawLine(startX, startY, endX, endY);
    }

    public Line2D asLine() {
        return new Line2D.Float(startX, startY, endX, endY);
    }

    public void saveEndValues() {
        oldEndX = endX;
        oldEndY = endY;
    }

    public void adjustStartForSpaceDownMove() {
        int dx = endX - oldEndX;
        int dy = endY - oldEndY;

        startX += dx;
        startY += dy;
    }

    public int getDX() {
        return endX - startX;
    }

    public int getDY() {
        return endY - startY;
    }

    /**
     * Creates a Rectangle where the sign of with/height indicate the direction of drawing
     *
     * @return a Rectangle where the width and height can be < 0
     */
    public Rectangle createPossiblyEmptyRectangle() {
        int x;
        int y;
        int width;
        int height;

        if (startFromCenter) {
            int halfWidth = endX - startX; // can be negative
            int halfHeight = endY - startY; // can be negative

            x = startX - halfWidth;
            y = startY - halfHeight;

            width = 2 * halfWidth;
            height = 2 * halfHeight;
        } else {
            x = startX;
            y = startY;
            width = endX - startX;
            height = endY - startY;
        }

        return new Rectangle(x, y, width, height);
    }

    /**
     * Creates a Rectangle where the width/height are >=0 independently of the direction of the drawing
     *
     * @return a Rectangle where the width and height are >= 0
     */
    public Rectangle createPositiveRectangle() {
        int x;
        int y;
        int width;
        int height;

        if (startFromCenter) {
            int halfWidth;  // positive or zero
            if (endX > startX) {
                halfWidth = endX - startX;
                x = startX - halfWidth;
            } else {
                halfWidth = startX - endX;
                x = endX;
            }

            int halfHeight; // positive or zero
            if (endY > startY) {
                halfHeight = endY - startY;
                y = startY - halfHeight;
            } else {
                halfHeight = startY - endY;
                y = endY;
            }

            width = 2 * halfWidth;
            height = 2 * halfHeight;
        } else {
            int tmpEndX;
            if (endX > startX) {
                x = startX;
                tmpEndX = endX;
            } else {
                x = endX;
                tmpEndX = startX;
            }

            int tmpEndY;
            if (endY > startY) {
                y = startY;
                tmpEndY = endY;
            } else {
                y = endY;
                tmpEndY = startY;
            }

            width = tmpEndX - x;
            height = tmpEndY - y;
        }
        return new Rectangle(x, y, width, height);
    }

    public Point2D getCenterPoint() {
        float cx = (startX + endX) / 2.0f;
        float cy = (startY + endY) / 2.0f;

        return new Point2D.Float(cx, cy);
    }


    public void setStartFromCenter(boolean startFromCenter) {
        this.startFromCenter = startFromCenter;
    }

//    public Rectangle getAffectedStrokedRectangle(int thickness) {
//        Rectangle r = createPositiveRectangle();
//        if (thickness == 0) {
//            return r;
//        }
//
//        int halfThickness = thickness / 2 + 1;
//        int sizeEnlargement = thickness + 2;
//
//        r.setBounds(r.x - halfThickness, r.y - halfThickness, r.width + sizeEnlargement, r.height + sizeEnlargement);
//        return r;
//    }

    public boolean isClick() {
        return ((startX == endX) && (startY == endY));
    }

    public boolean isStartFromCenter() {
        return startFromCenter;
    }

    @Override
    public String toString() {
        return "UserDrag{" +
                "startX=" + startX +
                ", startY=" + startY +
                ", endX=" + endX +
                ", endY=" + endY +
                '}';
    }

    public int taxiCabMetric(int x, int y) {
        return Math.abs(x - startX) + Math.abs(y - startY);
    }
}
