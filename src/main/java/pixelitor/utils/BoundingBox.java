/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.StringJoiner;

/**
 * A class that calculates a 2D bounding box from a set of points.
 */
public class BoundingBox {
    private double minX = Double.POSITIVE_INFINITY;
    private double minY = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;
    private boolean initialized = false;

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    /**
     * Expands the bounding box to include the given point.
     */
    public void add(double x, double y) {
        if (x > maxX) {
            maxX = x;
        }
        if (y > maxY) {
            maxY = y;
        }
        if (x < minX) {
            minX = x;
        }
        if (y < minY) {
            minY = y;
        }
        initialized = true;
    }

    public void add(Point2D point) {
        add(point.getX(), point.getY());
    }

    /**
     * Resets the bounding box to its initial state,
     * effectively removing all added points.
     */
    public void reset() {
        minX = Double.POSITIVE_INFINITY;
        minY = Double.POSITIVE_INFINITY;
        maxX = Double.NEGATIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;

        initialized = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Rectangle2D toRectangle2D() {
        return new Rectangle2D.Double(
            minX,
            minY,
            maxX - minX,
            maxY - minY);
    }

    public Rectangle2D toRectangle2D(double margin) {
        return new Rectangle2D.Double(
            minX - margin,
            minY - margin,
            maxX - minX + 2 * margin,
            maxY - minY + 2 * margin);
    }

    public Rectangle toRectangle(double margin) {
        return new Rectangle(
            (int) (minX - margin),
            (int) (minY - margin),
            (int) (maxX - minX + 2 * margin),
            (int) (maxY - minY + 2 * margin));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BoundingBox.class.getSimpleName() + "[", "]")
            .add("minX=" + minX)
            .add("minY=" + minY)
            .add("maxX=" + maxX)
            .add("maxY=" + maxY)
            .toString();
    }
}
