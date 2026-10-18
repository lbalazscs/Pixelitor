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

import java.awt.Shape;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * Clipping of arbitrary shapes against an axis-aligned rectangle,
 * without using java.awt.geom.Area.
 */
public final class RectClip {
    // maximum deviation (in image pixels) when curves have to be flattened
    private static final double FLATNESS = 0.01;

    private RectClip() {
    }

    /**
     * Returns the part of the shape that is inside the rectangle.
     * The winding rule of the shape is preserved.
     */
    public static Shape intersect(Shape shape, Rectangle2D rect) {
        Rectangle2D bounds = shape.getBounds2D();
        if (isInside(bounds, rect)) {
            return shape; // nothing to clip, keeps the type info (Ellipse2D etc.)
        }
        int rule = shape.getPathIterator(null).getWindingRule();
        Path2D.Double result = new Path2D.Double(rule);
        if (!isDisjoint(bounds, rect)) {
            clipInto(shape, rect, result, false);
        }
        return simplify(result);
    }

    /**
     * Returns the rectangle minus the shape.
     * Assumes that the winding number of the shape is -1, 0 or 1 everywhere
     * (always true for even-odd shapes, and for non-zero shapes
     * that don't have overlapping subpaths).
     */
    public static Shape subtract(Rectangle2D rect, Shape shape) {
        if (isDisjoint(shape.getBounds2D(), rect)) {
            return rect;
        }

        // With the even-odd rule, the rectangle contour and the clipped
        // contours of the shape XOR each other. A contour that coincides
        // with the rectangle (for example, the one added by a previous
        // subtract) toggles the rectangle off instead of being kept,
        // otherwise repeated inversions would pile up rectangle contours.
        Path2D.Double clipped = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        boolean shapeHadRect = clipInto(shape, rect, clipped, true);
        boolean hasOtherContours = clipped.getCurrentPoint() != null;

        if (shapeHadRect) {
            // rect XOR (rect + rest) = rest (which is empty if the shape was the rect)
            return simplify(clipped);
        }
        if (!hasOtherContours) {
            return rect; // the shape was empty after clipping
        }
        clipped.append(rect, false);
        return clipped;
    }

    private static boolean isInside(Rectangle2D b, Rectangle2D r) {
        return b.getMinX() >= r.getMinX() && b.getMaxX() <= r.getMaxX()
            && b.getMinY() >= r.getMinY() && b.getMaxY() <= r.getMaxY();
    }

    private static boolean isDisjoint(Rectangle2D b, Rectangle2D r) {
        return b.getMaxX() <= r.getMinX() || b.getMinX() >= r.getMaxX()
            || b.getMaxY() <= r.getMinY() || b.getMinY() >= r.getMaxY();
    }

    /**
     * Clips every subpath of the shape (implicitly closed, as when filling)
     * to the rectangle, and appends the results to the output.
     * If dropRectContours is true, then clipped contours that coincide
     * with the rectangle are not appended, and the return value tells
     * whether there was an odd number of them.
     */
    private static boolean clipInto(Shape shape, Rectangle2D rect, Path2D out,
                                    boolean dropRectContours) {
        PathIterator it = new FlatteningPathIterator(
            shape.getPathIterator(null), FLATNESS);
        double[] c = new double[6];
        Contour contour = new Contour(32);
        double startX = 0, startY = 0;
        boolean rectParity = false;

        while (!it.isDone()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO -> {
                    rectParity ^= clipContour(contour, rect, out, dropRectContours);
                    contour = new Contour(32);
                    startX = c[0];
                    startY = c[1];
                    contour.add(startX, startY);
                }
                case PathIterator.SEG_LINETO -> {
                    if (contour.n == 0) { // continuing after a SEG_CLOSE
                        contour.add(startX, startY);
                    }
                    contour.add(c[0], c[1]);
                }
                case PathIterator.SEG_CLOSE -> {
                    rectParity ^= clipContour(contour, rect, out, dropRectContours);
                    contour = new Contour(32);
                }
                default -> throw new IllegalStateException(
                    "unexpected segment type in a flattened path");
            }
            it.next();
        }
        rectParity ^= clipContour(contour, rect, out, dropRectContours);
        return rectParity;
    }

    /**
     * Returns true if the contour coincided with the rectangle
     * and was dropped because of dropRectContour.
     */
    private static boolean clipContour(Contour contour, Rectangle2D rect,
                                       Path2D out, boolean dropRectContour) {
        if (contour.n < 3) {
            return false; // no area
        }
        double minX = rect.getMinX(), maxX = rect.getMaxX();
        double minY = rect.getMinY(), maxY = rect.getMaxY();

        // cheap tests that avoid the clipping for most contours
        // of a complex shape (like a magic wand selection)
        double[] b = contour.bounds();
        if (b[2] <= minX || b[0] >= maxX || b[3] <= minY || b[1] >= maxY) {
            return false; // completely outside
        }

        Contour result;
        if (b[0] >= minX && b[2] <= maxX && b[1] >= minY && b[3] <= maxY) {
            result = contour; // completely inside
        } else {
            // Sutherland-Hodgman: clip against the four edges one after the other
            result = contour
                .clip(true, minX, 1)
                .clip(true, maxX, -1)
                .clip(false, minY, 1)
                .clip(false, maxY, -1);
            if (result.n < 3) {
                return false;
            }
        }

        if (dropRectContour) {
            Rectangle2D r = result.asRectangle();
            if (r != null && r.equals(rect)) {
                return true;
            }
        }
        result.appendTo(out);
        return false;
    }

    /**
     * Returns a Rectangle2D if the path consists of a single
     * axis-aligned rectangle, otherwise returns the path itself.
     */
    private static Shape simplify(Path2D path) {
        PathIterator it = path.getPathIterator(null);
        double[] c = new double[6];
        Contour contour = new Contour(8);
        while (!it.isDone()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO -> {
                    if (contour.n > 0) {
                        return path; // more than one subpath
                    }
                    contour.add(c[0], c[1]);
                }
                case PathIterator.SEG_LINETO -> {
                    if (contour.n >= 6) {
                        return path; // too many points for a rectangle
                    }
                    contour.add(c[0], c[1]);
                }
                case PathIterator.SEG_CLOSE -> {
                }
                default -> {
                    return path; // has curves
                }
            }
            it.next();
        }
        Rectangle2D r = contour.asRectangle();
        return r != null ? r : path;
    }

    /**
     * A closed polygon, stored as interleaved x, y coordinates.
     */
    private static final class Contour {
        double[] xy;
        int n; // number of points

        Contour(int capacity) {
            xy = new double[2 * capacity];
        }

        void add(double x, double y) {
            int i = 2 * n++;
            if (i + 2 > xy.length) {
                xy = Arrays.copyOf(xy, xy.length * 2);
            }
            xy[i] = x;
            xy[i + 1] = y;
        }

        /**
         * Returns {minX, minY, maxX, maxY}.
         */
        double[] bounds() {
            double minX = Double.POSITIVE_INFINITY, minY = minX;
            double maxX = Double.NEGATIVE_INFINITY, maxY = maxX;
            for (int i = 0; i < n; i++) {
                double x = xy[2 * i], y = xy[2 * i + 1];
                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
            return new double[]{minX, minY, maxX, maxY};
        }

        /**
         * Keeps the part of the polygon where side * (coordinate - bound) >= 0.
         * The coordinate is x if xAxis is true, otherwise y.
         */
        Contour clip(boolean xAxis, double bound, int side) {
            Contour out = new Contour(n + 8);
            if (n == 0) {
                return out;
            }
            double px = xy[2 * (n - 1)], py = xy[2 * (n - 1) + 1];
            double pd = side * ((xAxis ? px : py) - bound);
            for (int i = 0; i < n; i++) {
                double cx = xy[2 * i], cy = xy[2 * i + 1];
                double cd = side * ((xAxis ? cx : cy) - bound);
                if ((cd >= 0) != (pd >= 0)) { // the edge crosses the boundary
                    double t = pd / (pd - cd);
                    if (xAxis) {
                        out.add(bound, py + t * (cy - py));
                    } else {
                        out.add(px + t * (cx - px), bound);
                    }
                }
                if (cd >= 0) {
                    out.add(cx, cy);
                }
                px = cx;
                py = cy;
                pd = cd;
            }
            return out;
        }

        /**
         * If this polygon is an axis-aligned rectangle (ignoring
         * repeated points), returns it, otherwise returns null.
         */
        Rectangle2D asRectangle() {
            double[] p = new double[8];
            int m = 0; // number of points, without repeated ones
            double lastX = Double.NaN, lastY = Double.NaN;
            for (int i = 0; i < n; i++) {
                double x = xy[2 * i], y = xy[2 * i + 1];
                if (x == lastX && y == lastY) {
                    continue;
                }
                lastX = x;
                lastY = y;
                if (m == 4) {
                    if (x == p[0] && y == p[1]) {
                        continue; // closing point
                    }
                    return null;
                }
                p[2 * m] = x;
                p[2 * m + 1] = y;
                m++;
            }
            if (m != 4 || (p[6] == p[0] && p[7] == p[1])) {
                return null;
            }
            boolean verticalFirst = p[0] == p[2] && p[3] == p[5] && p[4] == p[6] && p[7] == p[1];
            boolean horizontalFirst = p[1] == p[3] && p[2] == p[4] && p[5] == p[7] && p[6] == p[0];
            if (!verticalFirst && !horizontalFirst) {
                return null;
            }
            double minX = Math.min(Math.min(p[0], p[2]), Math.min(p[4], p[6]));
            double maxX = Math.max(Math.max(p[0], p[2]), Math.max(p[4], p[6]));
            double minY = Math.min(Math.min(p[1], p[3]), Math.min(p[5], p[7]));
            double maxY = Math.max(Math.max(p[1], p[3]), Math.max(p[5], p[7]));
            if (minX == maxX || minY == maxY) {
                return null;
            }
            return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
        }

        void appendTo(Path2D path) {
            path.moveTo(xy[0], xy[1]);
            for (int i = 1; i < n; i++) {
                path.lineTo(xy[2 * i], xy[2 * i + 1]);
            }
            path.closePath();
        }
    }
}
