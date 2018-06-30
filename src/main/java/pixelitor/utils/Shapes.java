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

package pixelitor.utils;

import pixelitor.gui.ImageComponent;
import pixelitor.tools.pen.AnchorPoint;
import pixelitor.tools.pen.ControlPoint;
import pixelitor.tools.pen.Path;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.tools.pen.AnchorPointType.SMOOTH;

/**
 * Static shape-related utility methods
 */
public class Shapes {
    private static final Stroke BIG_STROKE = new BasicStroke(3);
    private static final Stroke SMALL_STROKE = new BasicStroke(1);

    private Shapes() {
        // do not instantiate
    }

    /**
     * Converts the given {@link Shape}, assumed to be
     * in image coordinates, to a {@link Path}
     */
    public static Path shapeToPath(Shape shape, ImageComponent ic) {
        Path path = new Path();
        PathIterator it = shape.getPathIterator(null);
        double[] coords = new double[6];
        while (!it.isDone()) {
            int type = it.currentSegment(coords);
            double x = coords[0];
            double y = coords[1];
            double xx = coords[2];
            double yy = coords[3];
            double xxx = coords[4];
            double yyy = coords[5];

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    startNewSubpath(path, x, y, ic);
                    break;
                case PathIterator.SEG_LINETO:
                    addLineToPath(path, x, y, ic);
                    break;
                case PathIterator.SEG_QUADTO:
                    addQuadCurveToPath(path, x, y, xx, yy, ic);
                    break;
                case PathIterator.SEG_CUBICTO:
                    addCubicCurveToPath(path, x, y, xx, yy, xxx, yyy, ic);
                    break;
                case PathIterator.SEG_CLOSE:
                    path.close();
                    break;
                default:
                    throw new IllegalArgumentException("type = " + type);
            }

            it.next();
        }
        assert path.checkWiring();
        return path;
    }

    private static void startNewSubpath(Path path, double x, double y, ImageComponent ic) {
        x = ic.imageXToComponentSpace(x);
        y = ic.imageYToComponentSpace(y);
        AnchorPoint first = new AnchorPoint(x, y, ic);
        first.setType(SMOOTH);
        path.startNewSubPath(first);
    }

    private static void addLineToPath(Path path, double newX, double newY, ImageComponent ic) {
        newX = ic.imageXToComponentSpace(newX);
        newY = ic.imageYToComponentSpace(newY);
        AnchorPoint ap = new AnchorPoint(newX, newY, ic);
        path.addPoint(ap);
    }

    private static void addQuadCurveToPath(Path path, double cx, double cy,
                                           double newX, double newY, ImageComponent ic) {
        cx = ic.imageXToComponentSpace(cx);
        cy = ic.imageYToComponentSpace(cy);
        newX = ic.imageXToComponentSpace(newX);
        newY = ic.imageYToComponentSpace(newY);
        AnchorPoint last = path.getLast();

        // convert the quadratic bezier (with one control point)
        // into a cubic one (with two control points), see
        // https://stackoverflow.com/questions/3162645/convert-a-quadratic-bezier-to-a-cubic
        double qp1x = cx;
        double qp1y = cy;
        double qp0x = last.x;
        double qp0y = last.y;
        double qp2x = newX;
        double qp2y = newY;

        double twoThirds = 2.0 / 3.0;
        double cp1x = qp0x + twoThirds * (qp1x - qp0x);
        double cp1y = qp0y + twoThirds * (qp1y - qp0y);
        double cp2x = qp2x + twoThirds * (qp1x - qp2x);
        double cp2y = qp2y + twoThirds * (qp1y - qp2y);

        ControlPoint lastOut = last.ctrlOut;
        lastOut.setLocationOnlyForThis(cp1x, cp1y);
        lastOut.afterMovingActionsForThis();

        AnchorPoint next = new AnchorPoint(newX, newY, ic);
        path.addPoint(next);
        next.setType(SMOOTH);

        ControlPoint nextIn = next.ctrlIn;
        nextIn.setLocationOnlyForThis(cp2x, cp2y);
        nextIn.afterMovingActionsForThis();
    }

    private static void addCubicCurveToPath(Path path, double c1x, double c1y,
                                            double c2x, double c2y,
                                            double newX, double newY, ImageComponent ic) {
        ControlPoint lastOut = path.getLast().ctrlOut;
        c1x = ic.imageXToComponentSpace(c1x);
        c1y = ic.imageYToComponentSpace(c1y);
        lastOut.setLocationOnlyForThis(c1x, c1y);
        lastOut.afterMovingActionsForThis();

        newX = ic.imageXToComponentSpace(newX);
        newY = ic.imageYToComponentSpace(newY);
        AnchorPoint next = new AnchorPoint(newX, newY, ic);
        path.addPoint(next);
        next.setType(SMOOTH);

        c2x = ic.imageXToComponentSpace(c2x);
        c2y = ic.imageYToComponentSpace(c2y);
        ControlPoint nextIn = next.ctrlIn;
        nextIn.setLocationOnlyForThis(c2x, c2y);
        nextIn.afterMovingActionsForThis();
    }

    public static void drawVisible(Graphics2D g, Shape shape) {
        assert shape != null;

        // black at the edges
        g.setStroke(BIG_STROKE);
        g.setColor(BLACK);
        g.draw(shape);

        // white in the middle
        g.setStroke(SMALL_STROKE);
        g.setColor(WHITE);
        g.draw(shape);
    }

    public static void fillVisible(Graphics2D g, Shape shape) {
        assert shape != null;

        fillVisible(g, shape, Color.WHITE);
    }

    public static void fillVisible(Graphics2D g, Shape shape, Color c) {
        assert shape != null;

        // black at the edges
        g.setStroke(BIG_STROKE);
        g.setColor(BLACK);
        g.draw(shape);

        // the given color in the middle
        g.setStroke(SMALL_STROKE);
        g.setColor(c);
        g.fill(shape);
    }

    public static void drawGradientArrow(Graphics2D g,
                                         double startX, double startY,
                                         double endX, double endY) {
        Line2D line = new Line2D.Double(startX, startY, endX, endY);
        Shapes.drawVisible(g, line);

        double angle = Math.atan2(endY - startY, endX - startX);

        double backAngle1 = 2.8797926 + angle;
        double backAngle2 = 3.4033926 + angle;
        int arrowRadius = 20;

        double arrowEnd1X = endX + (arrowRadius * Math.cos(backAngle1));
        double arrowEnd1Y = endY + (arrowRadius * Math.sin(backAngle1));
        double arrowEnd2X = endX + (arrowRadius * Math.cos(backAngle2));
        double arrowEnd2Y = endY + (arrowRadius * Math.sin(backAngle2));

        GeneralPath.Double arrowHead = new Path2D.Double();
        arrowHead.moveTo(endX, endY);
        arrowHead.lineTo(arrowEnd1X, arrowEnd1Y);
        arrowHead.lineTo(arrowEnd2X, arrowEnd2Y);
        arrowHead.closePath();
        Shapes.fillVisible(g, arrowHead);
    }

    // makes sure that the returned rectangle has positive width, height
    public static Rectangle toPositiveRect(int x1, int x2, int y1, int y2) {
        int topX, topY, width, height;

        if (x2 >= x1) {
            topX = x1;
            width = x2 - x1;
        } else {
            topX = x2;
            width = x1 - x2;
        }

        if (y2 >= y1) {
            topY = y1;
            height = y2 - y1;
        } else {
            topY = y2;
            height = y1 - y2;
        }

        return new Rectangle(topX, topY, width, height);
    }

    // makes sure that the returned rectangle has positive width, height
    public static Rectangle2D toPositiveRect(Rectangle2D input) {
        double inX = input.getX();
        double inY = input.getY();
        double inWidth = input.getWidth();
        double inHeight = input.getHeight();

        if (inWidth >= 0) {
            if (inHeight >= 0) {
                return input; // should be the most common case
            } else { // negative height
                double newY = inY + inHeight;
                return new Rectangle2D.Double(inX, newY, inWidth, -inHeight);
            }
        } else { // negative width
            if (inHeight >= 0) {
                double newX = inX + inWidth;
                return new Rectangle2D.Double(newX, inY, -inWidth, inHeight);
            } else { // negative height
                double newX = inX + inWidth;
                double newY = inY + inHeight;
                return new Rectangle2D.Double(newX, newY, -inWidth, -inHeight);
            }
        }
    }

    // makes sure that the returned rectangle has positive width, height
    public static Rectangle toPositiveRect(Rectangle rect) {
        int width = rect.width;
        int height = rect.height;

        if (width >= 0) {
            if (height >= 0) {
                return rect;
            } else {
                rect.y += height;
                rect.height = -height;
                return rect;
            }
        } else {
            if (height >= 0) {
                rect.x += width;
                rect.width = -width;
                return rect;
            } else {
                rect.x += width;
                rect.y += height;
                rect.width = -width;
                rect.height = -height;
                return rect;
            }
        }
    }

    public static Point2D calcCenter(Point2D p1, Point2D p2) {
        return new Point2D.Double(
                (p1.getX() + p2.getX()) / 2.0,
                (p1.getY() + p2.getY()) / 2.0
        );
    }
}
