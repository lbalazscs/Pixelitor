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

package pixelitor.tools.util;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Similar to {@link UserDrag}, but for clarity
 * here everything is stored exclusively in image space.
 */
public class ImDrag {
    private final double startX;
    private final double startY;
    private final double endX;
    private final double endY;

    private boolean startFromCenter;

    public ImDrag(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public ImDrag(PPoint start, PPoint end) {
        startX = start.getImX();
        startY = start.getImY();
        endX = end.getImX();
        endY = end.getImY();
    }

    public ImDrag(Rectangle2D r) {
        startX = r.getX();
        startY = r.getY();
        endX = startX + r.getWidth();
        endY = startY + r.getHeight();
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public double getStartXFromCenter() {
        if (startFromCenter) {
            return startX - (endX - startX);
        } else {
            return startX;
        }
    }

    public double getStartYFromCenter() {
        if (startFromCenter) {
            return startY - (endY - startY);
        } else {
            return startY;
        }
    }

    public Point2D getCenterPoint() {
        double cx = (startX + endX) / 2.0;
        double cy = (startY + endY) / 2.0;

        return new Point2D.Double(cx, cy);
    }

    public ImDrag getCenterDrag() {
        Point2D center = getCenterPoint();
        ImDrag rv = new ImDrag(center.getX(), center.getY(), this.getEndX(), this.getEndY());
        return rv;
    }

    public double getEndX() {
        return endX;
    }

    public double getEndY() {
        return endY;
    }

    public Point2D.Double getStartPoint() {
        return new Point2D.Double(startX, startY);
    }

    public Point2D.Double getEndPoint() {
        return new Point2D.Double(endX, endY);
    }

    public boolean isClick() {
        return ((startX == endX) && (startY == endY));
    }

    public Line2D asLine() {
        return new Line2D.Double(startX, startY, endX, endY);
    }

    public void setStartFromCenter(boolean startFromCenter) {
        this.startFromCenter = startFromCenter;
    }

    public boolean isStartFromCenter() {
        return startFromCenter;
    }

    /**
     * Creates a Rectangle where the sign of with/height indicate the direction of drawing
     *
     * @return a Rectangle where the width and height can be < 0
     */
    public Rectangle2D createPossiblyEmptyRect() {
        double x;
        double y;
        double width;
        double height;

        if (startFromCenter) {
            double halfWidth = endX - startX; // can be negative
            double halfHeight = endY - startY; // can be negative

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

        return new Rectangle2D.Double(x, y, width, height);
    }

    /**
     * Creates a Rectangle where the width/height are >=0 independently of the direction of the drawing
     *
     * @return a Rectangle where the width and height are >= 0
     */
    public Rectangle2D createPositiveRect() {
        double x;
        double y;
        double width;
        double height;

        if (startFromCenter) {
            double halfWidth;  // positive or zero
            if (endX > startX) {
                halfWidth = endX - startX;
                x = startX - halfWidth;
            } else {
                halfWidth = startX - endX;
                x = endX;
            }

            double halfHeight; // positive or zero
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
            double tmpEndX;
            if (endX > startX) {
                x = startX;
                tmpEndX = endX;
            } else {
                x = endX;
                tmpEndX = startX;
            }

            double tmpEndY;
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
        return new Rectangle2D.Double(x, y, width, height);
    }

    public double getDistance() {
        double dx = startX - endX;
        double dy = startY - endY;
        return Math.sqrt(dx * dx + dy * dy);
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

    public double getDX() {
        return endX - startX;
    }

    public double getDY() {
        return endY - startY;
    }

    public double taxiCabMetric(int x, int y) {
        return Math.abs(x - startX) + Math.abs(y - startY);
    }
}
