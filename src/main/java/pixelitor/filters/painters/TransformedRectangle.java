/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.painters;

import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * A rectangle rotated/scaled/sheared around its center.
 */
public class TransformedRectangle implements Debuggable {
    private final double origTopLeftX;
    private final double origTopLeftY;
    private final double origTopRightX;
    private final double origTopRightY;
    private final double origBottomRightX;
    private final double origBottomRightY;
    private final double origBottomLeftX;
    private final double origBottomLeftY;
    private final double theta;

    private double topLeftX;
    private double topLeftY;
    private double topRightX;
    private double topRightY;
    private double bottomRightX;
    private double bottomRightY;
    private double bottomLeftX;
    private double bottomLeftY;

    private GeneralPath cachedShape;
    private Rectangle cachedBox;

    public TransformedRectangle(Rectangle r, double theta,
                                double sx, double sy, double shx, double shy) {
        this(r.getX(), r.getY(), r.getWidth(), r.getHeight(), theta, sx, sy, shx, shy);
    }

    public TransformedRectangle(double x, double y,
                                double width, double height,
                                double theta, double sx, double sy,
                                double shx, double shy) {
        origTopLeftX = x;
        origTopLeftY = y;
        this.theta = theta;

        origTopRightX = x + width;
        origTopRightY = y;

        origBottomRightX = origTopRightX;
        origBottomRightY = y + height;

        origBottomLeftX = x;
        origBottomLeftY = origBottomRightY;

        double cx = x + width / 2.0;
        double cy = y + height / 2.0;

        AffineTransform transform = AffineTransform.getTranslateInstance(cx, cy);
        if (theta != 0) {
            transform.rotate(theta);
        }
        if (sx != 1.0 || sy != 1.0) {
            transform.scale(sx, sy);
        }
        if (shx != 0 || shy != 0) {
            // the opposite direction seems to be more intuitive
            transform.shear(-shx, -shy); 
        }
        transform.translate(-cx, -cy);

        Point2D topLeft = new Point2D.Double(origTopLeftX, origTopLeftY);
        Point2D bottomLeft = new Point2D.Double(origBottomLeftX, origBottomLeftY);
        Point2D topRight = new Point2D.Double(origTopRightX, origTopRightY);
        Point2D bottomRight = new Point2D.Double(origBottomRightX, origBottomRightY);

        transform.transform(topLeft, topLeft);
        transform.transform(bottomLeft, bottomLeft);
        transform.transform(topRight, topRight);
        transform.transform(bottomRight, bottomRight);

        topLeftX = topLeft.getX();
        topLeftY = topLeft.getY();
        bottomLeftX = bottomLeft.getX();
        bottomLeftY = bottomLeft.getY();
        topRightX = topRight.getX();
        topRightY = topRight.getY();
        bottomRightX = bottomRight.getX();
        bottomRightY = bottomRight.getY();
    }

    public double getTopLeftX() {
        return topLeftX;
    }

    public double getTopLeftY() {
        return topLeftY;
    }

    public Shape asShape() {
        if (cachedShape != null) {
            return cachedShape;
        }

        cachedShape = new GeneralPath();
        cachedShape.moveTo(topLeftX, topLeftY);
        cachedShape.lineTo(topRightX, topRightY);
        cachedShape.lineTo(bottomRightX, bottomRightY);
        cachedShape.lineTo(bottomLeftX, bottomLeftY);
        cachedShape.closePath();

        return cachedShape;
    }

    public Rectangle getBoundingBox() {
        if (cachedBox != null) {
            return cachedBox;
        }
        double minX = min(min(topLeftX, topRightX), min(bottomRightX, bottomLeftX));
        double minY = min(min(topLeftY, topRightY), min(bottomRightY, bottomLeftY));
        double maxX = max(max(topLeftX, topRightX), max(bottomRightX, bottomLeftX));
        double maxY = max(max(topLeftY, topRightY), max(bottomRightY, bottomLeftY));

        int width = (int) Math.ceil(maxX - minX);
        int height = (int) Math.ceil(maxY - minY);
        cachedBox = new Rectangle((int) minX, (int) minY, width, height);
        return cachedBox;
    }

    public void align(Rectangle layout, Rectangle transformedBounds) {
        int dx = layout.x - transformedBounds.x;
        int dy = layout.y - transformedBounds.y;
        translate(dx, dy);
    }

    public void translate(double dx, double dy) {
        topLeftX += dx;
        topLeftY += dy;

        topRightX += dx;
        topRightY += dy;

        bottomRightX += dx;
        bottomRightY += dy;

        bottomLeftX += dx;
        bottomLeftY += dy;

        cachedShape = null;
        cachedBox = null;
    }

    // paints the original and transformed corners for debugging
    public void paintCorners(Graphics2D g) {
        g.setColor(Color.RED);
        g.fillOval((int) topLeftX - 5, (int) topLeftY - 5, 10, 10);
        g.fillOval((int) origTopLeftX - 5, (int) origTopLeftY - 5, 10, 10);

        g.setColor(Color.BLUE);
        g.fillOval((int) topRightX - 5, (int) topRightY - 5, 10, 10);
        g.fillOval((int) origTopRightX - 5, (int) origTopRightY - 5, 10, 10);

        g.setColor(Color.YELLOW);
        g.fillOval((int) bottomRightX - 5, (int) bottomRightY - 5, 10, 10);
        g.fillOval((int) origBottomRightX - 5, (int) origBottomRightY - 5, 10, 10);

        g.setColor(new Color(0, 100, 0));
        g.fillOval((int) bottomLeftX - 5, (int) bottomLeftY - 5, 10, 10);
        g.fillOval((int) origBottomLeftX - 5, (int) origBottomLeftY - 5, 10, 10);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addDouble("origTopLeftX", origTopLeftX);
        node.addDouble("origTopLeftY", origTopLeftY);
        node.addDouble("topLeftX", topLeftX);
        node.addDouble("topLeftY", topLeftY);

        return node;
    }
}

