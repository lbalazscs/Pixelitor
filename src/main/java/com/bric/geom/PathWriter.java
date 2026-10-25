/*
 * @(#)PathWriter.java
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

import java.awt.Shape;
import java.awt.geom.PathIterator;

import static java.awt.geom.PathIterator.*;

/**
 * This object writes path data.
 * <P>With this abstract class you could filter shape data as it is
 * being written.  The simplest extension of this is the
 * {@link GeneralPathWriter}.
 *
 * <P>This is designed to match the <code>GeneralPath</code> method
 * signatures.
 */
public abstract class PathWriter {
    /**
     * Adds a point to the path by moving to the specified coordinates.
     *
     * @param x the x-coordinate to move to
     * @param y the y-coordinate to move to
     */
    public abstract void moveTo(float x, float y);

    /**
     * Adds a point to the path by drawing a straight line from the current coordinates to the new specified coordinates.
     *
     * @param x the x-coordinate of the end point.
     * @param y the y-coordinate of the end point.
     */
    public abstract void lineTo(float x, float y);

    /**
     * Adds a curved segment, defined by two new points, to the path by drawing a Quadratic curve that intersects both the current coordinates and the coordinates (x2, y2), using the specified point (x1, y1) as a quadratic parametric control point.
     *
     * @param cx the x-coordinate of the control point.
     * @param cy the y-coordinate of the control point.
     * @param x  the x-coordinate of the end point.
     * @param y  the y-coordinate of the end point.
     */
    public abstract void quadTo(float cx, float cy, float x, float y);

    /**
     * Adds a curved segment, defined by three new points, to the path by drawing a Bézier curve that intersects both the current coordinates and the coordinates (x3, y3), using the specified points (x1, y1) and (x2, y2) as Bezier control points.
     *
     * @param cx1 the x-coordinate of the first control point.
     * @param cy1 the y-coordinate of the first control point.
     * @param cx2 the x-coordinate of the second control point.
     * @param cy2 the y-coordinate of the second control point.
     * @param x   the x-coordinate of the end point.
     * @param y   the y-coordinate of the end point.
     */
    public abstract void curveTo(float cx1, float cy1, float cx2, float cy2, float x, float y);

    /**
     * Closes the current subpath by drawing a straight line back to the coordinates of the last moveTo.
     */
    public abstract void closePath();

    /**
     * This guarantees that this writer has flushed all currently written
     * information.
     * <P>For example, some writers may need to perform complex calculations
     * and re-organize shapes.  Or some writers may simply be buffering
     * path instructions to better manage memory.  Calling this method
     * guarantees that all path instructions be immediately processed.
     * <P>Because some writers may be manipulating path data you should
     * <i>not call this method</i> until all shape data has been written.
     */
    public abstract void flush();

    /**
     * Writes a shape.
     * <P>This iterates through the shape and makes the appropriate
     * calls to <code>moveTo()</code>, <code>curveTo()</code>, etc.
     *
     * @param s the shape to write.
     */
    public void write(Shape s) {
        write(s.getPathIterator(null));
    }

    /**
     * Writes a path.
     * <P>This iterates through the path and makes the appropriate
     * calls to <code>moveTo()</code>, <code>curveTo()</code>, etc.
     *
     * @param i the path to write.
     */
    public void write(PathIterator i) {
        float[] coords = new float[6];
        int k;
        while (!i.isDone()) {
            k = i.currentSegment(coords);
            switch (k) {
                case SEG_MOVETO -> moveTo(coords[0], coords[1]);
                case SEG_LINETO -> lineTo(coords[0], coords[1]);
                case SEG_QUADTO -> quadTo(coords[0], coords[1], coords[2], coords[3]);
                case SEG_CUBICTO -> curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                case SEG_CLOSE -> closePath();
                default -> throw new RuntimeException("Unexpected segment: " + k);
            }
            i.next();
        }
    }

    /**
     * This appends a cubic curve to the PathWriter provided that ranges from t = t0 to t = t1.
     * This assumes that the path already ends at the point (ax*t0*t0*t0+bx*t0*t0+cx*t0+dx, ay*t0*t0*t0+by*t0*t0+cy*t0+dy).
     *
     * @param path the path to append data to.
     * @param t0   the t-value for the beginning of this curve
     * @param t1   the t-value for the end of this curve
     * @param ax   the coefficient for the (t^3) term of the x parametric equation
     * @param bx   the coefficient for the (t^2) term of the x parametric equation
     * @param cx   the coefficient for the t term of the x parametric equation
     * @param dx   the constant for the x parametric equation.
     * @param ay   the coefficient for the (t^3) term of the y parametric equation
     * @param by   the coefficient for the (t^2) term of the y parametric equation
     * @param cy   the coefficient for the t term of the y parametric equation
     * @param dy   the constant for the y parametric equation.
     */
    public static void cubicTo(PathWriter path, double t0, double t1, double ax, double bx, double cx, double dx, double ay, double by, double cy, double dy) {
        float[] pts = computeCubic(t0, t1, ax, bx, cx, dx, ay, by, cy, dy);
        path.curveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);
    }

    private static float[] computeCubic(double t0, double t1, double ax, double bx, double cx, double dx, double ay, double by, double cy, double dy) {
        double h = t1 - t0;
        double hThird = h / 3.0;

        double f0x = ((ax * t0 + bx) * t0 + cx) * t0 + dx;
        double f1x = ((ax * t1 + bx) * t1 + cx) * t1 + dx;
        double df0x = (3 * ax * t0 + 2 * bx) * t0 + cx;
        double df1x = (3 * ax * t1 + 2 * bx) * t1 + cx;

        double f0y = ((ay * t0 + by) * t0 + cy) * t0 + dy;
        double f1y = ((ay * t1 + by) * t1 + cy) * t1 + dy;
        double df0y = (3 * ay * t0 + 2 * by) * t0 + cy;
        double df1y = (3 * ay * t1 + 2 * by) * t1 + cy;

        return new float[]{
            (float) (f0x + hThird * df0x),
            (float) (f0y + hThird * df0y),
            (float) (f1x - hThird * df1x),
            (float) (f1y - hThird * df1y),
            (float) f1x,
            (float) f1y
        };
    }

    /**
     * This appends a quadratic curve to the PathWriter provided that ranges from t = t0 to t = t1.
     * This assumes that the path already ends at the point (ax*t0*t0+bx*t0+cx, ay*t0*t0+by*t0+cy).
     *
     * @param path the path to append data to.
     * @param t0   the t-value for the beginning of this curve
     * @param t1   the t-value for the end of this curve
     * @param ax   the coefficient for the (t^2) term of the x parametric equation
     * @param bx   the coefficient for the t term of the x parametric equation
     * @param cx   the constant for the x parametric equation.
     * @param ay   the coefficient for the (t^2) term of the y parametric equation
     * @param by   the coefficient for the t term of the y parametric equation
     * @param cy   the constant for the y parametric equation.
     */
    public static void quadTo(PathWriter path, double t0, double t1, double ax, double bx, double cx, double ay, double by, double cy) {
        float[] pts = computeQuad(t0, t1, ax, bx, cx, ay, by, cy);
        path.quadTo(pts[0], pts[1], pts[2], pts[3]);
    }

    private static float[] computeQuad(double t0, double t1, double ax, double bx, double cx, double ay, double by, double cy) {
        double h = t1 - t0;
        double hHalf = h / 2.0;

        double f0x = (ax * t0 + bx) * t0 + cx;
        double df0x = 2 * ax * t0 + bx;
        double f1x = (ax * t1 + bx) * t1 + cx;

        double f0y = (ay * t0 + by) * t0 + cy;
        double df0y = 2 * ay * t0 + by;
        double f1y = (ay * t1 + by) * t1 + cy;

        return new float[]{
            (float) (f0x + hHalf * df0x),
            (float) (f0y + hHalf * df0y),
            (float) f1x,
            (float) f1y
        };
    }
}
