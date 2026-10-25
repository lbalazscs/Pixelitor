/*
 * @(#)MeasuredShape.java
 *
 * Copyright (c) 2011 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * https://javagraphics.java.net/
 * 
 * That site should also contain the most recent official version
 * of this software.  (See the SVN repository for more details.)
 */

package com.bric.geom;

import com.jhlabs.image.ImageMath;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static java.awt.geom.PathIterator.*;

/**
 * This represents a single closed path.
 * <P>This object can trace arbitrary amounts of itself using the
 * <code>writeShape()</code> methods.
 */
public class MeasuredShape {
    /**
     * This is the increment t goes through as each shape segment is
     * traversed. For quadratic and cubic curves, this affects
     * how the shape distance is measured. The default value is .05,
     * meaning quadratic and cubic curves are converted to linear segments
     * connecting at t = 0, t = .05, t = .1, ... t = .95, t = 1.
     */
    private static final float DEFAULT_SPACING = 0.05f;

    private final Segment[] segments;
    private final float moveX;
    private final float moveY;
    private final float closedDistance;
    private final float originalDistance;

    /**
     * Because a MeasuredShape must be exactly 1 subpath, this method
     * will safely break up a path into separate subpaths and create one
     * MeasuredShape for each.
     *
     * @param s a path, possibly containing multiple subpaths
     * @return a MeasuredShape object for each subpath in <code>s</code>
     */
    public static MeasuredShape[] getSubpaths(Shape s) {
        return getSubpaths(s.getPathIterator(null), DEFAULT_SPACING);
    }

    /**
     * Because a MeasuredShape must be exactly 1 subpath, this method
     * will safely break up a path into separate subpaths and create one
     * MeasuredShape for each.
     *
     * @param s       a path, possibly containing multiple subpaths
     * @param spacing the spacing to be used for each <code>MeasuredShape</code>
     * @return a MeasuredShape object for each subpath in <code>s</code>
     */
    public static MeasuredShape[] getSubpaths(Shape s, float spacing) {
        return getSubpaths(s.getPathIterator(null), spacing);
    }

    /**
     * Because a MeasuredShape must be exactly 1 subpath, this method
     * will safely break up a path into separate subpaths and create one
     * MeasuredShape for each.
     *
     * @param i a path, possibly containing multiple subpaths
     * @return a MeasuredShape object for each subpath in <code>i</code>
     */
    public static MeasuredShape[] getSubpaths(PathIterator i) {
        return getSubpaths(i, DEFAULT_SPACING);
    }

    /**
     * Because a MeasuredShape must be exactly 1 subpath, this method
     * will safely break up a path into separate subpaths and create one
     * MeasuredShape for each.
     *
     * @param i       a path, possibly containing multiple subpaths
     * @param spacing the spacing to be used for each <code>MeasuredShape</code>
     * @return a MeasuredShape object for each subpath in <code>i</code>
     */
    public static MeasuredShape[] getSubpaths(PathIterator i, float spacing) {
        List<MeasuredShape> subpaths = new ArrayList<>();
        GeneralPath path = null;
        boolean hasSegments = false;
        float[] coords = new float[6];
        while (!i.isDone()) {
            int type = i.currentSegment(coords);
            if (path == null && type != SEG_MOVETO) {
                throw new IllegalArgumentException("path does not begin with a moveTo");
            }
            switch (type) {
                case SEG_MOVETO -> {
                    if (path != null && hasSegments) {
                        subpaths.add(new MeasuredShape(path, spacing));
                    }
                    path = new GeneralPath();
                    path.moveTo(coords[0], coords[1]);
                    hasSegments = false;
                }
                case SEG_LINETO -> {
                    path.lineTo(coords[0], coords[1]);
                    hasSegments = true;
                }
                case SEG_QUADTO -> {
                    path.quadTo(coords[0], coords[1], coords[2], coords[3]);
                    hasSegments = true;
                }
                case SEG_CUBICTO -> {
                    path.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    hasSegments = true;
                }
                case SEG_CLOSE -> path.closePath();
                default -> throw new IllegalArgumentException("Unexpected segment type: " + type);
            }
            i.next();
        }
        if (path != null && hasSegments) {
            subpaths.add(new MeasuredShape(path, spacing));
        }
        return subpaths.toArray(new MeasuredShape[0]);
    }

    /**
     * Construct a <code>MeasuredShape</code> from a <code>Shape</code>,
     * using the default spacing.
     *
     * @param s the shape data
     * @throws IllegalArgumentException if the shape has more than 1 path.
     */
    public MeasuredShape(Shape s) {
        this(s.getPathIterator(null), DEFAULT_SPACING);
    }

    /**
     * Construct a <code>MeasuredShape</code> from a <code>Shape</code>.
     *
     * @param s       the shape data to create
     * @param spacing the value to increment t as each segment is traversed.
     *                The default value is .05.
     * @throws IllegalArgumentException if the shape has more than 1 path.
     */
    public MeasuredShape(Shape s, float spacing) {
        this(s.getPathIterator(null), spacing);
    }

    /**
     * Construct a <code>MeasuredShape</code> from a <code>PathIterator</code>
     * using the default spacing.
     *
     * @param i the shape data to create
     * @throws IllegalArgumentException if the shape has more than 1 path.
     */
    public MeasuredShape(PathIterator i) {
        this(i, DEFAULT_SPACING);
    }

    /**
     * Construct a <code>MeasuredShape</code> from a <code>PathIterator</code>.
     *
     * @param i       the shape data to create
     * @param spacing the value to increment t as each segment is traversed.
     *                The default value is .05.
     * @throws IllegalArgumentException if the shape has more than 1 path.
     */
    public MeasuredShape(PathIterator i, float spacing) {
        List<Segment> segmentsList = new ArrayList<>();
        float lastX = 0;
        float lastY = 0;
        float moveX = 0;
        float moveY = 0;
        int pathCount = 0;
        boolean closed = false;
        float closedDistance = 0;

        float[] coords = new float[6];
        while (!i.isDone()) {
            int type = i.currentSegment(coords);
            switch (type) {
                case SEG_CLOSE -> closed = true;
                case SEG_MOVETO -> {
                    if (pathCount == 1) {
                        throw new IllegalArgumentException("this object can only contain 1 subpath");
                    }
                    moveX = coords[0];
                    moveY = coords[1];
                    lastX = moveX;
                    lastY = moveY;
                    pathCount++;
                }
                case SEG_LINETO, SEG_QUADTO, SEG_CUBICTO -> {
                    if (pathCount != 1) {
                        throw new IllegalArgumentException("this shape data did not begin with a moveTo");
                    }
                    Segment s = (type == SEG_LINETO)
                        ? Segment.line(lastX, lastY, coords[0], coords[1])
                        : new Segment(type, lastX, lastY, coords, spacing);
                    lastX = s.endX();
                    lastY = s.endY();
                    segmentsList.add(s);
                    closedDistance += s.realDistance;
                }
                default -> throw new IllegalArgumentException("Unexpected segment type: " + type);
            }
            i.next();
        }
        float unclosedDistance = closedDistance;
        if (!segmentsList.isEmpty()) {
            Segment last = segmentsList.getLast();
            if (Math.abs(last.endX() - moveX) > 0.001 ||
                Math.abs(last.endY() - moveY) > 0.001) {
                Segment s = Segment.line(last.endX(), last.endY(), moveX, moveY);
                segmentsList.add(s);
                closedDistance += s.realDistance;
            }
        }
        originalDistance = closed ? closedDistance : unclosedDistance;
        this.closedDistance = closedDistance;

        this.moveX = moveX;
        this.moveY = moveY;

        segments = segmentsList.toArray(new Segment[0]);
        float curDist = 0;
        for (Segment segment : segments) {
            segment.startDistance = curDist;
            curDist += segment.realDistance;
        }
    }

    /**
     * Writes the entire shape.
     *
     * @param w the destination to write to
     */
    public void writeShape(PathWriter w) {
        writeWhole(w, true);
    }

    /**
     * Writes the entire shape backwards.
     *
     * @param w the destination to write to
     */
    public void writeShapeBackwards(PathWriter w) {
        writeWholeBackwards(w, true);
    }

    private void writeWhole(PathWriter w, boolean includeMoveTo) {
        if (segments.length == 0) {
            return;
        }
        if (includeMoveTo) {
            w.moveTo(moveX, moveY);
        }
        for (Segment segment : segments) {
            segment.write(w, 0, 1);
        }
        w.closePath();
    }

    private void writeWholeBackwards(PathWriter w, boolean includeMoveTo) {
        if (segments.length == 0) {
            return;
        }
        if (includeMoveTo) {
            w.moveTo(segments[segments.length - 1].getX(1),
                segments[segments.length - 1].getY(1));
        }
        for (int a = segments.length - 1; a >= 0; a--) {
            segments[a].write(w, 1, 0);
        }
        w.closePath();
    }

    /**
     * Trace the shape.
     * <p>
     * Note that {@code position} and {@code position + length} are clamped to [0, 1] and never wrap past the start point.
     *
     * @param position a fraction from zero to one indicating where to start tracing
     * @param length   a fraction from negative one to one indicating how much to trace.
     *                 If this value is negative then the shape will be traced backwards.
     * @param w        the destination to write to
     */
    public void writeShape(float position, float length, PathWriter w) {
        writeShape(position, length, w, true);
    }

    /**
     * Trace the shape.
     * <p>
     * Note that {@code position} and {@code position + length} are clamped to [0, 1] and never wrap past the start point.
     * For near-full lengths ({@code |length| >= 0.999999f}), tracing starts at the shape's beginning.
     *
     * @param position      a fraction from zero to one indicating where to start tracing
     * @param length        a fraction from negative one to one indicating how much to trace.
     *                      If this value is negative then the shape will be traced backwards.
     * @param w             the destination to write to
     * @param includeMoveTo this controls whether a moveTo is the first thing
     *                      written to the path.
     *                      Note setting this to <code>false</code> means it's the caller's responsibility
     *                      to make sure the path is in the correct position.
     */
    public void writeShape(float position, float length, PathWriter w, boolean includeMoveTo) {
        if (segments.length == 0) {
            return;
        }
        if (length >= 0.999999f) {
            writeWhole(w, includeMoveTo);
            return;
        } else if (length <= -0.999999f) {
            writeWholeBackwards(w, includeMoveTo);
            return;
        } else if (length < 0.000001 && length > -0.000001) {
            return;
        }

        Position i1 = getIndexOfPosition(position);
        Position i2 = getIndexOfPosition(position + length);
        int seg1 = i1.segmentIndex();
        float t1 = i1.t();
        int seg2 = i2.segmentIndex();
        float t2 = i2.t();

        if (includeMoveTo) {
            w.moveTo(segments[seg1].getX(t1), segments[seg1].getY(t1));
        }
        if (seg1 == seg2 && ((length > 0 && t2 > t1) || (length < 0 && t2 < t1))) {
            segments[seg1].write(w, t1, t2);
        } else {
            if (length > 0) {
                segments[seg1].write(w, t1, 1);
                int i = seg1 + 1;
                if (i >= segments.length) {
                    i = 0;
                }
                while (i != seg2) {
                    segments[i].write(w, 0, 1);
                    i++;
                    if (i >= segments.length) {
                        i = 0;
                    }
                }
                segments[seg2].write(w, 0, t2);
            } else {
                segments[seg1].write(w, t1, 0);
                int i = seg1 - 1;
                if (i < 0) {
                    i = segments.length - 1;
                }
                while (i != seg2) {
                    segments[i].write(w, 1, 0);
                    i--;
                    if (i < 0) {
                        i = segments.length - 1;
                    }
                }
                segments[seg2].write(w, 1, t2);
            }
        }
    }

    /**
     * The distance of this shape, assuming that the path is closed.
     * This will be greater than or equal to <code>getOriginalDistance()</code>.
     *
     * @see #getOriginalDistance()
     */
    public float getClosedDistance() {
        return closedDistance;
    }

    /**
     * The distance of the shape used to construct this
     * <code>MeasuredShape</code>.
     * <p>This will be less than or equal to <code>getClosedDistance()</code>.
     *
     * @return The distance this path covered when the shape was constructed.
     * @see #getClosedDistance()
     */
    public float getOriginalDistance() {
        return originalDistance;
    }

    /**
     * Returns the x-value of where this path begins.
     * <P>Because a <code>MeasuredShape</code> can only be one
     * path, there is only possible <code>moveTo()</code>.
     *
     * @return the x-value of where this path begins.
     */
    public float getMoveToX() {
        return moveX;
    }

    /**
     * Returns the y-value of where this path begins.
     * <P>Because a <code>MeasuredShape</code> can only be one
     * path, there is only possible <code>moveTo()</code>.
     *
     * @return the y-value of where this path begins.
     */
    public float getMoveToY() {
        return moveY;
    }

    private void checkDistance(float distance) {
        if (distance < 0) {
            throw new IllegalArgumentException("distance (" + distance + ") must not be negative");
        }
        if (distance > closedDistance) {
            throw new IllegalArgumentException("distance (" + distance + ") must not be greater than the total distance of this shape (" + closedDistance + ")");
        }
    }

    private int findSegmentIndex(float distance, int[] cursor) {
        int n = segments.length;
        if (n == 0) {
            return 0;
        }
        int idx;
        if (cursor != null && cursor.length > 0 && cursor[0] >= 0 && cursor[0] < n && segments[cursor[0]].startDistance <= distance) {
            idx = cursor[0];
            while (idx < n - 1 && segments[idx + 1].startDistance <= distance) {
                idx++;
            }
        } else {
            int low = 0, high = n - 1;
            idx = 0;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                if (segments[mid].startDistance <= distance) {
                    idx = mid;
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
        }
        if (cursor != null && cursor.length > 0) {
            cursor[0] = idx;
        }
        return idx;
    }

    private float localT(Segment s, float distance) {
        return (s.realDistance > 0) ? Math.clamp((distance - s.startDistance) / s.realDistance, 0f, 1f) : 0f;
    }

    /**
     * Computes the point and normal vector at a certain distance from the beginning of this shape.
     * <p>
     * If <code>segCursor</code> is non-null and has length &gt; 0, it tracks the current
     * segment index across sequential calls to achieve linear total time for monotonic queries.
     *
     * @param distance   the distance from the beginning of this shape to measure
     * @param segCursor  an optional 1-element array storing the current segment index, or null
     * @param pointDest  the destination to store the point coordinates, or null
     * @param normalDest the destination to store the normal vector coordinates, or null
     */
    public void getPointAndNormal(float distance, int[] segCursor, Point2D pointDest, Point2D normalDest) {
        checkDistance(distance);
        if (segments.length == 0) {
            if (pointDest != null) {
                pointDest.setLocation(moveX, moveY);
            }
            if (normalDest != null) {
                normalDest.setLocation(0, 1);
            }
            return;
        }
        if (distance >= closedDistance) {
            if (segCursor != null && segCursor.length > 0) {
                segCursor[0] = segments.length - 1;
            }
            segments[0].eval(0, pointDest, normalDest);
            return;
        }

        int index = findSegmentIndex(distance, segCursor);
        Segment s = segments[index];
        s.eval(localT(s, distance), pointDest, normalDest);
    }

    /**
     * Returns the point at a certain distance from the beginning of this shape.
     *
     * @param distance the distance from the beginning of this shape to measure
     * @param dest     the destination to store the result in. (If this is null a new
     *                 Point2D will be constructed.)
     * @return the point at a certain distance from the beginning of this shape.
     * Note this will be <code>dest</code> if <code>dest</code> is non-null.
     */
    public Point2D getPoint(float distance, Point2D dest) {
        return getPoint(distance, null, dest);
    }

    /**
     * Returns the point at a certain distance from the beginning of this shape using
     * a segment cursor for accelerated lookup.
     *
     * @param distance  the distance from the beginning of this shape to measure
     * @param segCursor an optional 1-element array tracking the segment index, or null
     * @param dest      the destination to store the result in
     * @return the point at a certain distance from the beginning of this shape.
     */
    public Point2D getPoint(float distance, int[] segCursor, Point2D dest) {
        if (dest == null) {
            dest = new Point2D.Float();
        }
        getPointAndNormal(distance, segCursor, dest, null);
        return dest;
    }

    private Position getIndexOfPosition(float p) {
        if (segments.length == 0) {
            return new Position(0, 0f);
        }
        p = Math.clamp(p, 0f, 1f);
        if (p >= 0.99999f) {
            return new Position(0, 0f);
        }

        float targetDistance = p * closedDistance;
        int idx = findSegmentIndex(targetDistance, null);
        Segment s = segments[idx];
        return new Position(idx, localT(s, targetDistance));
    }

    private static class Segment {
        final int type;
        final float[] data;
        final float ax, bx, cx, dx;
        final float ay, by, cy, dy;
        final float realDistance;
        float startDistance;

        public static Segment line(float x0, float y0, float x1, float y1) {
            return new Segment(x0, y0, x1, y1);
        }

        private Segment(float x0, float y0, float x1, float y1) {
            this.type = SEG_LINETO;
            this.ax = 0;
            this.bx = 0;
            this.cx = x1 - x0;
            this.dx = x0;
            this.ay = 0;
            this.by = 0;
            this.cy = y1 - y0;
            this.dy = y0;
            this.data = new float[]{x0, y0, x1, y1};
            this.realDistance = ImageMath.hypot(cx, cy);
        }

        public Segment(int type, float lastX, float lastY, float[] coords, float spacing) {
            this.type = type;
            switch (type) {
                case SEG_LINETO -> {
                    ax = 0;
                    bx = 0;
                    cx = coords[0] - lastX;
                    dx = lastX;
                    ay = 0;
                    by = 0;
                    cy = coords[1] - lastY;
                    dy = lastY;
                    data = new float[]{lastX, lastY, coords[0], coords[1]};
                    realDistance = ImageMath.hypot(coords[0] - lastX, coords[1] - lastY);
                }
                case SEG_QUADTO -> {
                    ax = 0;
                    bx = lastX - 2 * coords[0] + coords[2];
                    cx = -2 * lastX + 2 * coords[0];
                    dx = lastX;
                    ay = 0;
                    by = lastY - 2 * coords[1] + coords[3];
                    cy = -2 * lastY + 2 * coords[1];
                    dy = lastY;
                    data = new float[]{lastX, lastY, coords[0], coords[1], coords[2], coords[3]};
                    realDistance = calculateDistance(ax, bx, cx, dx, ay, by, cy, dy, spacing);
                }
                case SEG_CUBICTO -> {
                    ax = -lastX + 3 * coords[0] - 3 * coords[2] + coords[4];
                    bx = 3 * lastX - 6 * coords[0] + 3 * coords[2];
                    cx = -3 * lastX + 3 * coords[0];
                    dx = lastX;
                    ay = -lastY + 3 * coords[1] - 3 * coords[3] + coords[5];
                    by = 3 * lastY - 6 * coords[1] + 3 * coords[3];
                    cy = -3 * lastY + 3 * coords[1];
                    dy = lastY;
                    data = new float[]{lastX, lastY, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]};
                    realDistance = calculateDistance(ax, bx, cx, dx, ay, by, cy, dy, spacing);
                }
                default -> throw new IllegalStateException("Unrecognized segment type: " + type);
            }
        }

        private static float calculateDistance(double ax, double bx, double cx, double dx,
                                               double ay, double by, double cy, double dy, float spacing) {
            int steps = Math.max(1, (int) Math.ceil(1.0 / spacing - 1e-6));
            double prevX = dx, prevY = dy, sum = 0;
            for (int i = 1; i <= steps; i++) {
                double t = (double) i / steps;
                double x = ((ax * t + bx) * t + cx) * t + dx;
                double y = ((ay * t + by) * t + cy) * t + dy;
                sum += ImageMath.hypot(x - prevX, y - prevY);
                prevX = x;
                prevY = y;
            }
            return (float) sum;
        }

        public float startX() {
            return data[0];
        }

        public float startY() {
            return data[1];
        }

        public float endX() {
            return data[data.length - 2];
        }

        public float endY() {
            return data[data.length - 1];
        }

        public void write(PathWriter path, float t0, float t1) {
            if (t0 == 0 && t1 == 1) {
                switch (type) {
                    case SEG_LINETO -> path.lineTo(data[2], data[3]);
                    case SEG_QUADTO -> path.quadTo(data[2], data[3], data[4], data[5]);
                    case SEG_CUBICTO -> path.curveTo(data[2], data[3], data[4], data[5], data[6], data[7]);
                    default -> throw new IllegalStateException("Unexpected segment type: " + type);
                }
                return;
            } else if (t0 == 1 && t1 == 0) {
                switch (type) {
                    case SEG_LINETO -> path.lineTo(data[0], data[1]);
                    case SEG_QUADTO -> path.quadTo(data[2], data[3], data[0], data[1]);
                    case SEG_CUBICTO -> path.curveTo(data[4], data[5], data[2], data[3], data[0], data[1]);
                    default -> throw new IllegalStateException("Unexpected segment type: " + type);
                }
                return;
            }
            switch (type) {
                case SEG_LINETO -> path.lineTo(getX(t1), getY(t1));
                case SEG_QUADTO -> PathWriter.quadTo(path, t0, t1, bx, cx, dx, by, cy, dy);
                case SEG_CUBICTO -> PathWriter.cubicTo(path, t0, t1, ax, bx, cx, dx, ay, by, cy, dy);
                default -> throw new IllegalStateException("Unexpected segment type: " + type);
            }
        }

        public float getTangentSlope(float t) {
            float derivX = (3 * ax * t + 2 * bx) * t + cx;
            float derivY = (3 * ay * t + 2 * by) * t + cy;
            return (float) Math.atan2(derivY, derivX);
        }

        public void eval(float t, Point2D pointDest, Point2D normalDest) {
            if (pointDest != null) {
                pointDest.setLocation(getX(t), getY(t));
            }
            if (normalDest != null) {
                float derivX = (3 * ax * t + 2 * bx) * t + cx;
                float derivY = (3 * ay * t + 2 * by) * t + cy;
                float len = (float) Math.sqrt((double) derivX * derivX + (double) derivY * derivY);
                if (len > 0f) {
                    float invLen = 1.0f / len;
                    normalDest.setLocation(-derivY * invLen, derivX * invLen);
                } else {
                    normalDest.setLocation(0f, 1f);
                }
            }
        }

        public float getX(float t) {
            return ((ax * t + bx) * t + cx) * t + dx;
        }

        public float getY(float t) {
            return ((ay * t + by) * t + cy) * t + dy;
        }
    }

    private record Position(int segmentIndex, float t) {
    }
}
