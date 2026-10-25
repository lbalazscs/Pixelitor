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
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Arrays;

import static java.awt.geom.PathIterator.*;

/**
 * Walks along a {@link Shape}: it answers where the point at a given
 * distance from the start of the path is, and which direction the path
 * has there.
 *
 * <p>The shape is flattened only once, in the constructor, and the
 * length and the samples are all calculated from the same polyline.
 * The result is immutable.</p>
 *
 * <p>The distance is measured along the path, and the jump between two
 * subpaths (a {@code moveTo}) doesn't count: the subpaths are
 * concatenated. A closed subpath has its closing segment. Zero-length
 * segments and segments with non-finite coordinates are ignored.</p>
 */
public final class PathSampler {
    // The same flatness that was used when the text was laid out along
    // a path, so that the result is the same as before the extraction.
    private static final double FLATNESS = 1.0;

    /**
     * A point on the path and the direction of the path at that point.
     *
     * @param x     the x coordinate of the point
     * @param y     the y coordinate of the point
     * @param angle the angle of the tangent in radians, as in {@link Math#atan2}
     */
    public record Sample(double x, double y, double angle) {
    }

    // The flattened geometry: x1, y1, x2, y2 for each non-degenerate segment.
    // This is also the identity of the path: the closing segment of a closed
    // subpath is an explicit segment here, so closed and open paths with
    // the same corner points are not confused.
    private final double[] segments;

    // The distance from the start of the path to the end of each segment.
    // It is strictly increasing, apart from rounding.
    private final double[] endDists;

    public PathSampler(Shape path) {
        PathIterator it = new FlatteningPathIterator(path.getPathIterator(null), FLATNESS);
        double[] coords = new double[6];

        double[] segs = new double[4 * 32];
        double[] ends = new double[32];
        int numSegs = 0;
        double totalLength = 0;

        boolean started = false;
        double moveX = 0, moveY = 0; // start of the current subpath
        double curX = 0, curY = 0;   // the current point

        for (; !it.isDone(); it.next()) {
            double toX, toY;
            switch (it.currentSegment(coords)) {
                case SEG_MOVETO -> {
                    moveX = curX = coords[0];
                    moveY = curY = coords[1];
                    started = true;
                    continue;
                }
                case SEG_LINETO -> {
                    toX = coords[0];
                    toY = coords[1];
                }
                case SEG_CLOSE -> {
                    toX = moveX;
                    toY = moveY;
                }
                default -> throw new IllegalStateException("not flattened");
            }
            if (!started) {
                continue; // malformed: a segment before the first moveTo
            }

            double dx = toX - curX;
            double dy = toY - curY;
            double length = Math.hypot(dx, dy);
            if (!Double.isFinite(length)) {
                continue; // invalid coordinates: the segment doesn't exist
            }
            if (length > 0) { // a zero-length segment has no direction
                if (numSegs == ends.length) {
                    ends = Arrays.copyOf(ends, numSegs * 2);
                    segs = Arrays.copyOf(segs, numSegs * 8);
                }
                int i = 4 * numSegs;
                segs[i] = curX;
                segs[i + 1] = curY;
                segs[i + 2] = toX;
                segs[i + 3] = toY;

                totalLength += length;
                ends[numSegs++] = totalLength;
            }
            curX = toX;
            curY = toY;
        }

        segments = Arrays.copyOf(segs, 4 * numSegs);
        endDists = Arrays.copyOf(ends, numSegs);
    }

    /**
     * Returns the length of the path (without the jumps between subpaths).
     */
    public double length() {
        int n = endDists.length;
        return n == 0 ? 0 : endDists[n - 1];
    }

    /**
     * Returns true if the path has no length at all.
     */
    public boolean isEmpty() {
        return endDists.length == 0;
    }

    /**
     * Returns the point and the direction at the given distance from the
     * start of the path, or null if the distance is negative, or longer
     * than the path. If the distance is exactly at the junction of two
     * segments, then the direction is the one of the earlier segment.
     */
    public Sample pointAndAngleAt(double dist) {
        int n = endDists.length;
        // the negated comparison also rejects NaN
        if (n == 0 || !(dist >= 0) || dist > endDists[n - 1]) {
            return null;
        }

        // binary search for the first segment that ends at or after dist
        int lo = 0;
        int hi = n - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (endDists[mid] < dist) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }

        int i = 4 * lo;
        double x1 = segments[i];
        double y1 = segments[i + 1];
        double dx = segments[i + 2] - x1;
        double dy = segments[i + 3] - y1;

        double startDist = lo == 0 ? 0 : endDists[lo - 1];
        double segLength = endDists[lo] - startDist;
        double ratio = segLength > 0 ? (dist - startDist) / segLength : 0;

        return new Sample(x1 + ratio * dx, y1 + ratio * dy, Math.atan2(dy, dx));
    }

    /**
     * If this path is the other path moved by a constant offset (within
     * the given tolerance), returns that offset (which is zero for
     * identical paths). Otherwise returns null.
     *
     * <p>The paths must have the same structure: the same number of
     * segments, with the same lengths and directions. Because of this,
     * everything that depends only on distances and directions along the
     * path stays valid, and anything that depends on positions only has
     * to be moved by the offset. The offset is always measured from the
     * given path, so repeated small moves can't accumulate errors if the
     * caller keeps comparing against the same original path.</p>
     */
    public Point2D offsetFrom(PathSampler other, double tolerance) {
        double[] a = segments;
        double[] b = other.segments;
        if (a.length != b.length) {
            return null;
        }
        if (a.length == 0) {
            return new Point2D.Double();
        }

        double offsetX = a[0] - b[0];
        double offsetY = a[1] - b[1];
        for (int i = 0; i < a.length; i += 2) {
            // written so that NaN is rejected
            if (!(Math.abs(a[i] - b[i] - offsetX) <= tolerance)
                || !(Math.abs(a[i + 1] - b[i + 1] - offsetY) <= tolerance)) {
                return null;
            }
        }
        return new Point2D.Double(offsetX, offsetY);
    }
}
