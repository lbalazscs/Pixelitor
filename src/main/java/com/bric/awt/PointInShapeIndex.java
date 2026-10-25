/*
 * @(#)PointInShapeIndex.java
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

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

/**
 * A read-only spatial index that answers "is this point inside the shape?"
 * in (nearly) constant time, instead of the O(segments) that
 * {@link java.awt.geom.Path2D#contains(double, double)} needs.
 * <p>
 * It is built once from a {@link Shape} and then queried many times.
 * It is thread-safe after construction.
 *
 * <h2>How it works</h2>
 * The shape's outline is split into <i>edges</i>, which are bucketed into
 * horizontal bands (a CSR layout: {@code bandStart} + {@code ids}).
 * A query casts a ray in the +x direction, but only looks at the edges
 * stored in the band of the query point, and sums the signed crossings,
 * exactly as {@code Path2D.contains} does:
 * <ul>
 *   <li>every subpath is implicitly closed;</li>
 *   <li>the same half-open conventions are used for the y-range
 *       ({@code y0 <= py < y1}) and the x-test, with the same
 *       floating-point operations for the x-intercept;</li>
 *   <li>the winding rule of the shape is applied to the sum
 *       (NON_ZERO: {@code != 0}, EVEN_ODD: odd).</li>
 * </ul>
 *
 * <h2>Curves are handled exactly, not approximately</h2>
 * Quadratic and cubic segments are subdivided into small <i>leaves</i>
 * until the control points of the leaf are within {@link #FLATNESS} of
 * its chord. For each leaf the index stores its control points and the
 * actual control-point distance {@code eps} from the chord. A Bézier leaf
 * lies in the convex hull of its control points, and the hull lies
 * inside the {@code eps}-neighborhood of the chord. Therefore, if the query
 * point is farther than {@code eps} from the chord, the leaf and its chord give
 * <i>identical</i> crossing counts, and the cheap chord test is used.
 * If the point is closer than that (rare), the crossing count of that single
 * leaf is evaluated exactly by local subdivision. So the answers match
 * {@code Shape.contains} even for points that are extremely close to a curve,
 * and not just for points farther away than some flattening tolerance.
 * <p>
 * This matters for the charcoal effect because it probes points only
 * 0.05 px away from the outline, and the result of each probe changes how
 * many random numbers are consumed, so even a rare flipped answer would
 * change the whole crack pattern that follows for the same seed.
 *
 * <h2>Which shapes are indexed</h2>
 * Only {@link Path2D} (including {@code GeneralPath}) and {@link Area} are indexed,
 * because their {@code contains} is O(segments) and their result is defined by
 * their outline. For every other shape ({@code Rectangle2D}, {@code Ellipse2D},
 * {@code RoundRectangle2D}, {@code Arc2D}, {@code Polygon}, custom shapes, ...) the
 * index delegates to {@code shape.contains}. That keeps the behavior exactly as
 * it was: for example {@code Ellipse2D.contains} is analytic and differs slightly from
 * its (Bézier-approximated) outline, and it is already O(1).
 * The same delegation is used if the outline has non-finite coordinates, or is
 * absurdly large.
 */
final class PointInShapeIndex {
    /**
     * Curves are subdivided until the control points of a leaf are this close
     * to the chord. This only affects speed (edge count vs. how often
     * the exact leaf evaluation is needed), never the results.
     */
    private static final double FLATNESS = 0.01;
    private static final double FLATNESS_SQ = FLATNESS * FLATNESS;
    /**
     * Safety limit for the curve subdivision (2^16 leaves per curve at most).
     */
    private static final int MAX_FLATTEN_DEPTH = 16;
    /**
     * Recursion limit of the exact leaf evaluation.
     */
    private static final int MAX_EXACT_DEPTH = 40;

    private static final double MIN_BAND_HEIGHT = 1.0;
    private static final int MAX_BANDS = 1 << 20;
    /**
     * Limits the number of band entries to about this times the number of edges.
     */
    private static final int MAX_ENTRIES_PER_EDGE = 16;
    /**
     * Above this, the index is not worth its memory and the shape is used directly.
     */
    private static final int MAX_EDGES = 1 << 24;

    private static final int HULL_STRIDE = 5; // minX, minY, maxX, maxY, epsSq

    private static final double[] NO_DOUBLES = new double[0];
    private static final int[] NO_INTS = new int[0];

    private final Shape shape;
    private final boolean evenOdd;
    private final boolean delegateToShape;

    // bounds of everything that can produce a crossing
    private final double minY;
    private final double maxY;
    private final double maxX;

    private final double invBandHeight;
    private final int bandCount;

    /**
     * Straight, non-horizontal edges: x0, y0, x1, y1.
     */
    private final double[] lines;
    private final int[] lineBandStart;
    private final int[] lineIds;

    /**
     * Curve leaves: x0, y0, c1x, c1y, c2x, c2y, x3, y3 (quads are elevated to cubics).
     */
    private final double[] curves;
    /**
     * Per leaf: control point bounding box and epsilon squared.
     */
    private final double[] curveHulls;
    private final int[] curveBandStart;
    private final int[] curveIds;

    PointInShapeIndex(Shape shape) {
        this.shape = shape;

        PathIterator it = shape.getPathIterator(null);
        this.evenOdd = it.getWindingRule() == PathIterator.WIND_EVEN_ODD;

        boolean indexable = hasOutlineDefinedContains(shape);
        Collector c = new Collector();
        if (indexable) {
            c.collect(it);
        }

        int lineCount = c.lineCount;
        int curveCount = c.curveCount;
        int edgeCount = lineCount + curveCount;

        if (!indexable || c.nonFinite || edgeCount > MAX_EDGES) {
            delegateToShape = true;
            minY = 0;
            maxY = 0;
            maxX = 0;
            invBandHeight = 0;
            bandCount = 0;
            lines = NO_DOUBLES;
            lineBandStart = NO_INTS;
            lineIds = NO_INTS;
            curves = NO_DOUBLES;
            curveHulls = NO_DOUBLES;
            curveBandStart = NO_INTS;
            curveIds = NO_INTS;
            return;
        }
        delegateToShape = false;

        if (edgeCount == 0) {
            // contains() is always false: y < +inf for every finite y
            minY = Double.POSITIVE_INFINITY;
            maxY = Double.NEGATIVE_INFINITY;
            maxX = Double.NEGATIVE_INFINITY;
            invBandHeight = 0;
            bandCount = 0;
            lines = NO_DOUBLES;
            lineBandStart = NO_INTS;
            lineIds = NO_INTS;
            curves = NO_DOUBLES;
            curveHulls = NO_DOUBLES;
            curveBandStart = NO_INTS;
            curveIds = NO_INTS;
            return;
        }

        minY = c.minY;
        maxY = c.maxY;
        maxX = c.maxX;
        lines = c.lines;
        curves = c.curves;
        curveHulls = c.hulls;

        // ~1px bands, enlarged if that would need too many bands or band entries
        double height = maxY - minY;
        int maxBands = (int) Math.min(MAX_BANDS, 4L * edgeCount);
        double bandHeight = Math.max(MIN_BAND_HEIGHT,
            Math.max(c.extentSum / ((double) MAX_ENTRIES_PER_EDGE * edgeCount), height / maxBands));
        bandCount = (int) Math.min(maxBands, (long) Math.floor(height / bandHeight) + 1);
        invBandHeight = 1.0 / bandHeight;

        // lines: band range = y-range of the edge
        int[] lineLo = new int[lineCount];
        int[] lineHi = new int[lineCount];
        for (int i = 0; i < lineCount; i++) {
            double ya = lines[4 * i + 1];
            double yb = lines[4 * i + 3];
            lineLo[i] = bandOf(Math.min(ya, yb), minY, invBandHeight, bandCount);
            lineHi[i] = bandOf(Math.max(ya, yb), minY, invBandHeight, bandCount);
        }
        lineBandStart = new int[bandCount + 1];
        lineIds = fillCsr(lineBandStart, lineLo, lineHi, lineCount);

        // curve leaves: band range = y-range of the control point hull
        int[] curveLo = new int[curveCount];
        int[] curveHi = new int[curveCount];
        for (int i = 0; i < curveCount; i++) {
            curveLo[i] = bandOf(curveHulls[HULL_STRIDE * i + 1], minY, invBandHeight, bandCount);
            curveHi[i] = bandOf(curveHulls[HULL_STRIDE * i + 3], minY, invBandHeight, bandCount);
        }
        curveBandStart = new int[bandCount + 1];
        curveIds = fillCsr(curveBandStart, curveLo, curveHi, curveCount);
    }

    /**
     * True for the shapes whose {@code contains} is the O(segments) crossing test
     * over their outline.
     */
    private static boolean hasOutlineDefinedContains(Shape shape) {
        return shape instanceof Path2D || shape instanceof Area;
    }

    /**
     * The monotonic y-to-band mapping. Because it is monotonic (subtraction, multiplication,
     * the cast and the clamp all preserve order), an edge with {@code ymin <= py < ymax}
     * is always stored in the band of {@code py}.
     */
    private static int bandOf(double y, double minY, double invBandHeight, int bandCount) {
        int b = (int) ((y - minY) * invBandHeight);
        return b < 0 ? 0 : Math.min(b, bandCount - 1);
    }

    /**
     * Builds the CSR arrays: fills {@code bandStart} and returns the id array.
     */
    private static int[] fillCsr(int[] bandStart, int[] lo, int[] hi, int count) {
        int bands = bandStart.length - 1;
        for (int i = 0; i < count; i++) {
            for (int b = lo[i]; b <= hi[i]; b++) {
                bandStart[b + 1]++;
            }
        }
        for (int b = 0; b < bands; b++) {
            bandStart[b + 1] += bandStart[b];
        }
        int[] ids = new int[bandStart[bands]];
        int[] next = new int[bands];
        System.arraycopy(bandStart, 0, next, 0, bands);
        for (int i = 0; i < count; i++) {
            for (int b = lo[i]; b <= hi[i]; b++) {
                ids[next[b]++] = i;
            }
        }
        return ids;
    }

    /**
     * Returns the same answer as {@code shape.contains(x, y)}.
     */
    boolean contains(double x, double y) {
        if (delegateToShape) {
            return shape.contains(x, y);
        }
        // like Path2D.contains: NaN and infinite coordinates are never inside
        if (!(x * 0.0 + y * 0.0 == 0.0)) {
            return false;
        }
        if (y < minY || y >= maxY || x >= maxX) {
            return false;
        }

        int band = bandOf(y, minY, invBandHeight, bandCount);
        int winding = 0;

        int[] ids = lineIds;
        double[] ln = lines;
        for (int k = lineBandStart[band], end = lineBandStart[band + 1]; k < end; k++) {
            int o = ids[k] << 2;
            winding += lineCrossings(x, y, ln[o], ln[o + 1], ln[o + 2], ln[o + 3]);
        }

        ids = curveIds;
        double[] cv = curves;
        double[] hulls = curveHulls;
        for (int k = curveBandStart[band], end = curveBandStart[band + 1]; k < end; k++) {
            int id = ids[k];
            int h = id * HULL_STRIDE;
            // the hull encloses both the curve and its chord, so these
            // early-outs are valid for both
            if (y < hulls[h + 1] || y >= hulls[h + 3] || x >= hulls[h + 2]) {
                continue;
            }
            int o = id << 3;
            double x0 = cv[o];
            double y0 = cv[o + 1];
            double x3 = cv[o + 6];
            double y3 = cv[o + 7];
            if (x < hulls[h]) {
                // the whole leaf is to the right of the point: only the net y-direction counts
                winding += (y3 > y ? 1 : 0) - (y0 > y ? 1 : 0);
            } else if (distSqToSegment(x, y, x0, y0, x3, y3) > hulls[h + 4]) {
                // outside the eps-neighborhood of the chord: the chord gives the same count as the curve
                winding += lineCrossings(x, y, x0, y0, x3, y3);
            } else {
                winding += cubicCrossings(x, y, x0, y0, cv[o + 2], cv[o + 3], cv[o + 4], cv[o + 5], x3, y3, 0);
            }
        }

        return evenOdd ? (winding & 1) != 0 : winding != 0;
    }

    /**
     * Signed crossing of the +x ray from (px, py) with the line (x0,y0)-(x1,y1),
     * using the same half-open conventions and operations as {@code Path2D.contains}.
     */
    private static int lineCrossings(double px, double py, double x0, double y0, double x1, double y1) {
        if (py < y0 && py < y1) {
            return 0;
        }
        if (py >= y0 && py >= y1) {
            return 0;
        }
        if (px >= x0 && px >= x1) {
            return 0;
        }
        if (px < x0 && px < x1) {
            return y0 < y1 ? 1 : -1;
        }
        double xIntercept = x0 + (py - y0) * (x1 - x0) / (y1 - y0);
        if (px >= xIntercept) {
            return 0;
        }
        return y0 < y1 ? 1 : -1;
    }

    /**
     * Exact signed crossing count of the +x ray from (px, py) with a cubic Bézier curve,
     * by recursive subdivision with bounding box culling. Only used for the rare
     * query points that are within epsilon of a leaf.
     */
    private static int cubicCrossings(double px, double py,
                                      double x0, double y0, double x1, double y1,
                                      double x2, double y2, double x3, double y3, int depth) {
        double minY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
        double maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));
        if (py < minY || py >= maxY) {
            return 0;
        }
        double maxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));
        if (px >= maxX) {
            return 0;
        }
        double minX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
        if (px < minX) {
            return (y3 > py ? 1 : 0) - (y0 > py ? 1 : 0);
        }
        if (depth >= MAX_EXACT_DEPTH) {
            return lineCrossings(px, py, x0, y0, x3, y3);
        }
        double x01 = (x0 + x1) * 0.5, y01 = (y0 + y1) * 0.5;
        double x12 = (x1 + x2) * 0.5, y12 = (y1 + y2) * 0.5;
        double x23 = (x2 + x3) * 0.5, y23 = (y2 + y3) * 0.5;
        double x012 = (x01 + x12) * 0.5, y012 = (y01 + y12) * 0.5;
        double x123 = (x12 + x23) * 0.5, y123 = (y12 + y23) * 0.5;
        double xm = (x012 + x123) * 0.5, ym = (y012 + y123) * 0.5;
        return cubicCrossings(px, py, x0, y0, x01, y01, x012, y012, xm, ym, depth + 1)
            + cubicCrossings(px, py, xm, ym, x123, y123, x23, y23, x3, y3, depth + 1);
    }

    /**
     * Squared distance from (px, py) to the segment (x0,y0)-(x1,y1).
     */
    private static double distSqToSegment(double px, double py, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double ex = px - x0;
        double ey = py - y0;
        double len2 = dx * dx + dy * dy;
        if (len2 > 0) {
            double t = (ex * dx + ey * dy) / len2;
            if (t < 0) {
                t = 0;
            } else if (t > 1) {
                t = 1;
            }
            ex -= t * dx;
            ey -= t * dy;
        }
        return ex * ex + ey * ey;
    }

    /**
     * Walks the path once and collects straight edges and curve leaves.
     * Implicit closing follows Path2D.pointCrossings: each subpath is closed
     * by a line back to its start when it is not yet there.
     */
    private static final class Collector {
        double[] lines = new double[64];
        int lineCount;
        double[] curves = new double[64];
        double[] hulls = new double[40];
        int curveCount;

        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        /**
         * Sum of the y-extents of all edges, to estimate the number of band entries.
         */
        double extentSum;
        boolean nonFinite;

        void collect(PathIterator it) {
            double[] c = new double[6];
            double curX = 0, curY = 0, movX = 0, movY = 0;
            boolean started = false;
            while (!it.isDone() && !nonFinite) {
                int type = it.currentSegment(c);
                switch (type) {
                    case PathIterator.SEG_MOVETO -> {
                        if (started) {
                            addLine(curX, curY, movX, movY);
                        }
                        checkFinite(c, 2);
                        movX = curX = c[0];
                        movY = curY = c[1];
                        started = true;
                    }
                    case PathIterator.SEG_LINETO -> {
                        checkFinite(c, 2);
                        addLine(curX, curY, c[0], c[1]);
                        curX = c[0];
                        curY = c[1];
                    }
                    case PathIterator.SEG_QUADTO -> {
                        checkFinite(c, 4);
                        // exact degree elevation to a cubic
                        double c1x = curX + (2.0 / 3.0) * (c[0] - curX);
                        double c1y = curY + (2.0 / 3.0) * (c[1] - curY);
                        double c2x = c[2] + (2.0 / 3.0) * (c[0] - c[2]);
                        double c2y = c[3] + (2.0 / 3.0) * (c[1] - c[3]);
                        addCubic(curX, curY, c1x, c1y, c2x, c2y, c[2], c[3], 0);
                        curX = c[2];
                        curY = c[3];
                    }
                    case PathIterator.SEG_CUBICTO -> {
                        checkFinite(c, 6);
                        addCubic(curX, curY, c[0], c[1], c[2], c[3], c[4], c[5], 0);
                        curX = c[4];
                        curY = c[5];
                    }
                    case PathIterator.SEG_CLOSE -> {
                        addLine(curX, curY, movX, movY);
                        curX = movX;
                        curY = movY;
                    }
                    default -> nonFinite = true; // unknown segment type: don't try to be smart
                }
                it.next();
            }
            if (started && !nonFinite) {
                addLine(curX, curY, movX, movY);
            }
        }

        private void checkFinite(double[] c, int n) {
            for (int i = 0; i < n; i++) {
                if (!Double.isFinite(c[i])) {
                    nonFinite = true;
                    return;
                }
            }
        }

        void addLine(double x0, double y0, double x1, double y1) {
            if (nonFinite || y0 == y1) {
                return; // horizontal edges never produce a crossing
            }
            if (4 * lineCount + 4 > lines.length) {
                lines = java.util.Arrays.copyOf(lines, lines.length * 2);
            }
            int o = 4 * lineCount++;
            lines[o] = x0;
            lines[o + 1] = y0;
            lines[o + 2] = x1;
            lines[o + 3] = y1;
            minY = Math.min(minY, Math.min(y0, y1));
            maxY = Math.max(maxY, Math.max(y0, y1));
            maxX = Math.max(maxX, Math.max(x0, x1));
            extentSum += Math.abs(y1 - y0);
        }

        void addCubic(double x0, double y0, double x1, double y1,
                      double x2, double y2, double x3, double y3, int depth) {
            if (nonFinite) {
                return;
            }
            double distSq = Math.max(distSqToSegment(x1, y1, x0, y0, x3, y3),
                distSqToSegment(x2, y2, x0, y0, x3, y3));
            if (distSq <= FLATNESS_SQ || depth >= MAX_FLATTEN_DEPTH) {
                addLeaf(x0, y0, x1, y1, x2, y2, x3, y3, distSq);
                return;
            }
            double x01 = (x0 + x1) * 0.5, y01 = (y0 + y1) * 0.5;
            double x12 = (x1 + x2) * 0.5, y12 = (y1 + y2) * 0.5;
            double x23 = (x2 + x3) * 0.5, y23 = (y2 + y3) * 0.5;
            double x012 = (x01 + x12) * 0.5, y012 = (y01 + y12) * 0.5;
            double x123 = (x12 + x23) * 0.5, y123 = (y12 + y23) * 0.5;
            double xm = (x012 + x123) * 0.5, ym = (y012 + y123) * 0.5;
            addCubic(x0, y0, x01, y01, x012, y012, xm, ym, depth + 1);
            addCubic(xm, ym, x123, y123, x23, y23, x3, y3, depth + 1);
        }

        private void addLeaf(double x0, double y0, double x1, double y1,
                             double x2, double y2, double x3, double y3, double distSq) {
            if (distSq == 0) {
                // all control points lie on the chord, so it is just a line
                addLine(x0, y0, x3, y3);
                return;
            }
            double hMinY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
            double hMaxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));
            if (hMinY == hMaxY) {
                return; // no y-range, so it can never produce a crossing
            }
            double hMinX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
            double hMaxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));

            // a small safety margin on epsilon, so that rounding errors can't matter
            double eps = Math.sqrt(distSq) * 1.000001 + 1e-9;

            if (8 * curveCount + 8 > curves.length) {
                curves = java.util.Arrays.copyOf(curves, curves.length * 2);
            }
            if (HULL_STRIDE * curveCount + HULL_STRIDE > hulls.length) {
                hulls = java.util.Arrays.copyOf(hulls, hulls.length * 2);
            }
            int o = 8 * curveCount;
            curves[o] = x0;
            curves[o + 1] = y0;
            curves[o + 2] = x1;
            curves[o + 3] = y1;
            curves[o + 4] = x2;
            curves[o + 5] = y2;
            curves[o + 6] = x3;
            curves[o + 7] = y3;
            int h = HULL_STRIDE * curveCount;
            hulls[h] = hMinX;
            hulls[h + 1] = hMinY;
            hulls[h + 2] = hMaxX;
            hulls[h + 3] = hMaxY;
            hulls[h + 4] = eps * eps;
            curveCount++;

            minY = Math.min(minY, hMinY);
            maxY = Math.max(maxY, hMaxY);
            maxX = Math.max(maxX, hMaxX);
            extentSum += hMaxY - hMinY;
        }
    }
}
