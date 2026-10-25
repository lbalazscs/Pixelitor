/*
 * @(#)CalligraphyPathWriter.java
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

package com.bric.awt;

import com.bric.geom.GeneralPathWriter;
import com.bric.geom.PathSegment;
import com.bric.geom.PathWriter;
import com.bric.geom.SimplifiedPathIterator;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

import static java.awt.geom.PathIterator.*;
import static net.jafama.FastMath.cos;
import static net.jafama.FastMath.sin;

/**
 * This creates a calligraphic or ribbon-like effect while
 * tracing a shape.
 * <p>There are two separate shape bodies being written.  If you
 * paint them separately: this creates an "over" and "under" effect.
 * If you paint them the same color: then this class is basically
 * a <code>java.awt.Stroke</code>.
 * <p>Callers must call {@link #flush()} once after the last write.
 */
public class CalligraphyPathWriter extends PathWriter {
    private static final float[] NO_TIMES = new float[0];

    private enum Track {
        UNDEFINED, TRACK1, TRACK2
    }

    private final PathWriter dest1, dest2;

    private Track track = Track.UNDEFINED;
    private PathSegment.Float tail;
    private float moveX, moveY;
    private final AffineTransform rotate;
    private final AffineTransform shift1;
    private final AffineTransform shift2;

    /**
     * This is only used internally when we require a PathWriter to
     * write fragments of paths.
     */
    private final PathWriter tailWriter = new PathWriter() {
        @Override
        public void moveTo(float x, float y) {
            throw new UnsupportedOperationException("moveTo(" + x + ", " + y + ")");
        }

        @Override
        public void lineTo(float x, float y) {
            tail = tail.lineTo(x, y);
        }

        @Override
        public void quadTo(float cx, float cy, float x, float y) {
            tail = tail.quadTo(cx, cy, x, y);
        }

        @Override
        public void curveTo(float cx1, float cy1, float cx2, float cy2,
                            float x, float y) {
            tail = tail.cubicTo(cx1, cy1, cx2, cy2, x, y);
        }

        @Override
        public void closePath() {
            throw new UnsupportedOperationException("closePath()");
        }

        @Override
        public void flush() {
        }
    };

    /**
     * Create a new <code>CalligraphyPathWriter</code>.
     *
     * @param angle       the angle of the nib in this path.
     * @param offset1     the offset of one side of the traced shape.
     *                    One side of shape you trace will be offset by:
     *                    <br>(+offset1*cos(angle), +offset1*sin(angle) )
     *                    <p>It is possible for this value to be zero, which means one
     *                    side of this path will exactly line up with the original shape
     *                    being traced.
     *                    <p>If both offsets are equal: then no new path segments will be
     *                    visible because they produce a zero-width ribbon.
     *                    <p>To achieve a simple stroke: one offset should be (+width/2),
     *                    and the other offset should be (-width/2).
     * @param offset2     the offset of the other side of the traced shape.
     *                    This side of shape will be offset by:
     *                    <br>(+offset2*cos(angle), +offset2*sin(angle) )
     * @param destination the destination for the data to write.
     */
    public CalligraphyPathWriter(float angle, float offset1, float offset2, GeneralPath destination) {
        this(angle, offset1, offset2, new GeneralPathWriter(destination), new GeneralPathWriter(destination));
    }

    /**
     * Create a new <code>CalligraphyPathWriter</code>.
     *
     * @param angle   the angle of the nib in this path.
     * @param offset1 the offset of one side of the traced shape.
     *                One side of shape you trace will be offset by:
     *                <br>(+offset1*cos(angle), +offset1*sin(angle) )
     *                <p>It is possible for this value to be zero, which means one
     *                side of this path will exactly line up with the original shape
     *                being traced.
     *                <p>If both offsets are equal: then no new path segments will be
     *                visible because they produce a zero-width ribbon.
     *                <p>To achieve a simple stroke: one offset should be (+width/2),
     *                and the other offset should be (-width/2).
     * @param offset2 the offset of the other side of the traced shape.
     *                This side of shape will be offset by:
     *                <br>(+offset2*cos(angle), +offset2*sin(angle) )
     * @param dest1   the destination for half of the calligraphic shape.
     * @param dest2   the destination for the other half.  This may be the
     *                same as <code>dest1</code> if you plan on painting this path
     *                at the same time with the same fill.
     */
    public CalligraphyPathWriter(float angle, float offset1, float offset2, PathWriter dest1, PathWriter dest2) {
        requireFinite(angle, "angle is invalid");
        requireFinite(offset1, "offset1 is invalid");
        requireFinite(offset2, "offset2 is invalid");
        if (dest1 == null && dest2 == null) {
            throw new IllegalArgumentException();
        }
        this.dest1 = dest1;
        this.dest2 = dest2;
        rotate = AffineTransform.getRotateInstance(-angle);
        double cos = cos(angle);
        double sin = sin(angle);
        shift1 = AffineTransform.getTranslateInstance(offset1 * cos, offset1 * sin);
        shift2 = AffineTransform.getTranslateInstance(offset2 * cos, offset2 * sin);
    }

    private static void requireFinite(float value, String text) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(text + " (" + value + ")");
        }
    }

    private PathSegment.Float startOfNextSegment() {
        if (tail == null) {
            throw new IllegalStateException("missing moveTo segment");
        }
        return new PathSegment.Float(tail.data[tail.data.length - 2], tail.data[tail.data.length - 1]);
    }

    @Override
    public void moveTo(float x, float y) {
        requireFinite(x, "x is invalid");
        requireFinite(y, "y is invalid");
        flush(x, y);
        moveX = x;
        moveY = y;
    }

    @Override
    public void lineTo(float x, float y) {
        requireFinite(x, "x is invalid");
        requireFinite(y, "y is invalid");

        PathSegment.Float moveTo = startOfNextSegment();
        PathSegment.Float line = moveTo.lineTo(x, y);

        defineTrack(0, 1, line);

        tail = tail.lineTo(x, y);
    }

    @Override
    public void quadTo(float cx, float cy, float x, float y) {
        requireFinite(cx, "cx is invalid");
        requireFinite(cy, "cy is invalid");
        requireFinite(x, "x is invalid");
        requireFinite(y, "y is invalid");

        PathSegment.Float moveTo = startOfNextSegment();
        PathSegment.Float quad = moveTo.quadTo(cx, cy, x, y);

        //first, check to see if this is really quad data:
        int simplifiedSegment = SimplifiedPathIterator.simplify(
            SEG_QUADTO,
            moveTo.data[0], moveTo.data[1], quad.data);

        if (simplifiedSegment == SEG_LINETO) {
            lineTo(quad.data[0], quad.data[1]);
            return;
        }

        float[] times = findQuadExtrema(quad);
        if (times.length > 0) {
            float[] x_ = quad.getXCoeffs();
            float[] y_ = quad.getYCoeffs();
            float from = 0;
            for (float to : times) {
                traceQuadPiece(quad, x_, y_, from, to);
                flush(quad.getX(to), quad.getY(to));
                from = to;
            }
            traceQuadPiece(quad, x_, y_, from, 1);
            return;
        }

        defineTrack(0, 1, quad);
        tail = tail.quadTo(cx, cy, x, y);
    }

    private float[] findQuadExtrema(PathSegment.Float quad) {
        float[] ry_ = quad.getYCoeffs(rotate);
        float t = -ry_[1] / (2 * ry_[0]);
        if (t >= 0 && t <= 1) {
            return new float[]{t};
        }
        return NO_TIMES;
    }

    private void traceQuadPiece(PathSegment.Float quad, float[] x_, float[] y_, float from, float to) {
        defineTrack(from, to, quad);
        PathWriter.quadTo(tailWriter, from, to,
            x_[0], x_[1], x_[2],
            y_[0], y_[1], y_[2]);
    }

    @Override
    public void curveTo(float cx1, float cy1,
                        float cx2, float cy2,
                        float x, float y) {
        requireFinite(cx1, "cx1 is invalid");
        requireFinite(cy1, "cy1 is invalid");
        requireFinite(cx2, "cx2 is invalid");
        requireFinite(cy2, "cy2 is invalid");
        requireFinite(x, "x is invalid");
        requireFinite(y, "y is invalid");

        PathSegment.Float moveTo = startOfNextSegment();
        PathSegment.Float cubic = moveTo.cubicTo(cx1, cy1, cx2, cy2, x, y);

        //first, check to see if this is really cubic data:
        int simplifiedSegment = SimplifiedPathIterator.simplify(
            SEG_CUBICTO,
            moveTo.data[0], moveTo.data[1], cubic.data);
        if (simplifiedSegment == SEG_LINETO) {
            lineTo(cubic.data[0], cubic.data[1]);
            return;
        } else if (simplifiedSegment == SEG_QUADTO) {
            quadTo(cubic.data[0], cubic.data[1], cubic.data[2], cubic.data[3]);
            return;
        }

        float[] times = findCubicExtrema(cubic);
        if (times.length > 0) {
            float[] x_ = cubic.getXCoeffs();
            float[] y_ = cubic.getYCoeffs();
            float from = 0;
            for (float to : times) {
                traceCubicPiece(cubic, x_, y_, from, to);
                flush(cubic.getX(to), cubic.getY(to));
                from = to;
            }
            traceCubicPiece(cubic, x_, y_, from, 1);
            return;
        }

        defineTrack(0, 1, cubic);
        tail = tail.cubicTo(cx1, cy1, cx2, cy2, x, y);
    }

    private float[] findCubicExtrema(PathSegment.Float cubic) {
        float[] ry_ = cubic.getYCoeffs(rotate);
        if (Math.abs(ry_[0]) < 0.00001) {
            //the leading coefficient is effectively 0, so
            //instead of ay*(t^3)+by*(t^2)+cy*t+dy we have
            //0+bx*(t^2)+cx*t+dx

            float t = -ry_[2] / (2 * ry_[1]);
            if (t >= 0 && t <= 1) {
                return new float[]{t};
            }
            return NO_TIMES;
        }

        float determinant = 4 * ry_[1] * ry_[1] - 12 * ry_[0] * ry_[2];
        if (determinant >= 0) {
            float t1 = -1;
            float t2 = -1;
            determinant = (float) Math.sqrt(determinant);
            if (determinant == 0) {
                t1 = (-2 * ry_[1]) / (6 * ry_[0]);
            } else {
                t1 = (-2 * ry_[1] + determinant) / (6 * ry_[0]);
                t2 = (-2 * ry_[1] - determinant) / (6 * ry_[0]);
            }

            if (!cubic.isThetaWellDefined(t1)) {
                t1 = -1;
            }
            if (!cubic.isThetaWellDefined(t2)) {
                t2 = -1;
            }

            //sort our list of 2 elements:
            if (t2 < t1) {
                float swap = t1;
                t1 = t2;
                t2 = swap;
            }

            if (t1 >= 0 && t2 <= 1) {
                return new float[]{t1, t2};
            } else if (t1 >= 0 && t1 <= 1) {
                return new float[]{t1};
            } else if (t2 >= 0 && t2 <= 1) {
                return new float[]{t2};
            }
        }
        return NO_TIMES;
    }

    private void traceCubicPiece(PathSegment.Float cubic, float[] x_, float[] y_, float from, float to) {
        defineTrack(from, to, cubic);
        PathWriter.cubicTo(tailWriter,
            from, to, x_[0], x_[1], x_[2], x_[3],
            y_[0], y_[1], y_[2], y_[3]);
    }

    private void defineTrack(float t0, float t1, PathSegment.Float segment) {
        float[] y_ = segment.getYCoeffs(rotate);

        float t = (t0 + t1) / 2;
        boolean onTrack1 = switch (y_.length) {
            case 2 -> y_[0] > 0;
            case 3 -> (2 * y_[0] * t + y_[1]) > 0;
            case 4 -> (3 * y_[0] * t * t + 2 * y_[1] * t + y_[2]) > 0;
            default -> throw new RuntimeException("unexpected condition");
        };
        Track nextTrack = onTrack1 ? Track.TRACK1 : Track.TRACK2;
        if (track != Track.UNDEFINED && track != nextTrack) {
            float x = segment.getX(t0);
            float y = segment.getY(t0);
            flush(x, y);
        }

        if (track == Track.UNDEFINED) {
            track = nextTrack;
        }
    }

    @Override
    public void closePath() {
        PathSegment.Float next = startOfNextSegment();
        float lastX = next.data[0];
        float lastY = next.data[1];
        if (Math.abs(lastX - moveX) > 0.00001 || Math.abs(lastY - moveY) > 0.00001) {
            lineTo(moveX, moveY);
        }

        emitPendingRibbon();
    }

    private void flush(float moveX, float moveY) {
        emitPendingRibbon();
        tail = new PathSegment.Float(moveX, moveY);
    }

    private void emitPendingRibbon() {
        try {
            // This occurs if there hasn't been a moveTo
            if (tail == null) {
                return;
            }

            // This occurs if a moveTo is followed by a close
            // with no segment data.
            if (track == Track.UNDEFINED) {
                return;
            }

            PathWriter dest = (track == Track.TRACK1) ? dest1 : dest2;

            // This can happen if only 1 dest path was provided to write to.
            if (dest == null) {
                return;
            }

            AffineTransform transform1, transform2;

            // Switching the order we apply the transforms fixes winding
            // problems when dest1 and dest2 are the same path.  The same
            // basic path data is being written either way.
            if (track == Track.TRACK1) {
                transform1 = shift1;
                transform2 = shift2;
            } else {
                transform1 = shift2;
                transform2 = shift1;
            }

            PathSegment.Float head = tail.getHead();
            PathSegment.Float t = head;
            while (t != null) {
                t.write(dest, 0, 1, transform1);
                t = t.next;
            }

            t = tail;
            dest.lineTo(
                (float) (t.data[t.data.length - 2] + transform2.getTranslateX()),
                (float) (t.data[t.data.length - 1] + transform2.getTranslateY()));

            while (t != null) {
                if (t.type == SEG_MOVETO) {
                    if (t.prev == null) {
                        dest.closePath();
                    } else {
                        throw new RuntimeException("Unexpected condition.");
                    }
                } else {
                    t.write(dest, 1, 0, transform2);
                }
                t = t.prev;
            }
        } finally {
            tail = null;
            track = Track.UNDEFINED;
        }
    }

    /**
     * Flushes any pending ribbon and flushes the destination writer(s).
     * <p>Callers must call this method once after all shape data has been written.
     */
    @Override
    public void flush() {
        emitPendingRibbon();
        if (dest1 != null) {
            dest1.flush();
        }
        if (dest2 != null && dest2 != dest1) {
            dest2.flush();
        }
    }
}
