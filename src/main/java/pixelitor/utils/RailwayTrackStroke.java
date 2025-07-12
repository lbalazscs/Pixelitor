/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Stroke} implementation that looks like a railway track along a path.
 * The track consists of two parallel rails with perpendicular crossties at regular intervals.
 */
public class RailwayTrackStroke implements Stroke {
    private final double railDistance;
    private final double railWidth;
    private final double crosstieLength;
    private final double crosstieWidth;
    private final double crosstieSpacing;

    public RailwayTrackStroke(double width) {
        this(width,
            width / 5.0,
            1.33 * width,
            width / 3.0,
            width * 0.8);
    }

    public RailwayTrackStroke(double railDistance, double railWidth,
                              double crosstieLength, double crosstieWidth,
                              double crosstieSpacing) {
        if (railDistance <= 0 || railWidth <= 0 ||
            crosstieLength < railDistance || crosstieWidth <= 0 ||
            crosstieSpacing <= 0) {
            throw new IllegalArgumentException(
                "railDistance = %.2f, railWidth = %.2f, crosstieLength = %.2f, crosstieWidth = %.2f, crosstieSpacing = %.2f".formatted(
                    railDistance, railWidth, crosstieLength, crosstieWidth, crosstieSpacing)
            );
        }
        this.railDistance = railDistance;
        this.railWidth = railWidth;
        this.crosstieLength = crosstieLength;
        this.crosstieWidth = crosstieWidth;
        this.crosstieSpacing = crosstieSpacing;
    }

    @Override
    public Shape createStrokedShape(Shape shape) {
        double[] coords = new double[6];

        GeneralPath outline = new GeneralPath();
        List<Point2D> points = new ArrayList<>();

        var it = new FlatteningPathIterator(shape.getPathIterator(null), 1);
        while (!it.isDone()) {
            switch (it.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    if (!points.isEmpty()) {
                        createSubpathOutline(points, outline);
                        points.clear();
                    }
                    // fallthrough to add point
                case PathIterator.SEG_LINETO:
                    points.add(new Point2D.Double(coords[0], coords[1]));
                    break;
                case PathIterator.SEG_CLOSE:
                    // check if it was closed before reaching the first
                    Point2D first = points.getFirst();
                    if (coords[0] != first.getX() || coords[1] != first.getY()) {
                        points.add(first);
                    }
                    break;
            }
            it.next();
        }

        if (!points.isEmpty()) {
            createSubpathOutline(points, outline);
        }

        return outline;
    }

    private void createSubpathOutline(List<Point2D> points, GeneralPath result) {
        if (points.size() < 2) {
            return;
        }

        BasicStroke railStroke = new BasicStroke((float) railWidth);
        BasicStroke crosstieStroke = new BasicStroke((float) crosstieWidth);

        GeneralPath leftRail = new GeneralPath();
        GeneralPath rightRail = new GeneralPath();

        double halfRailDist = railDistance / 2;

        // calculate all rail points
        List<Point2D> leftPoints = new ArrayList<>();
        List<Point2D> rightPoints = new ArrayList<>();

        // handle first point
        Point2D first = points.get(0);
        Point2D second = points.get(1);
        Point2D firstLeft = new Point2D.Double();
        Point2D firstRight = new Point2D.Double();
        Geometry.calcPerpendicularPoints(first, second, halfRailDist, firstLeft, firstRight);
        leftPoints.add(firstLeft);
        rightPoints.add(firstRight);

        // process intermediate points
        for (int i = 1, s = points.size() - 1; i < s; i++) {
            Point2D prev = points.get(i - 1);
            Point2D current = points.get(i);
            Point2D next = points.get(i + 1);

            // temporary points for storing perpendicular vectors
            var prevPerp1 = new Point2D.Double();
            var prevPerp2 = new Point2D.Double();
            var nextPerp1 = new Point2D.Double();
            var nextPerp2 = new Point2D.Double();

            // calculate perpendicular points for both segments meeting at current point
            Geometry.calcPerpendicularPoints(current, prev, halfRailDist, prevPerp1, prevPerp2);
            Geometry.calcPerpendicularPoints(current, next, halfRailDist, nextPerp1, nextPerp2);

            leftPoints.add(prevPerp2);
            rightPoints.add(prevPerp1);

            leftPoints.add(nextPerp1);
            rightPoints.add(nextPerp2);
        }

        // handle last point
        Point2D last = points.getLast();
        Point2D secondToLast = points.get(points.size() - 2);
        Point2D lastRight = new Point2D.Double();
        Point2D lastLeft = new Point2D.Double();
        Geometry.calcPerpendicularPoints(last, secondToLast, halfRailDist, lastRight, lastLeft);
        leftPoints.add(lastLeft);
        rightPoints.add(lastRight);

        // draw the rails
        for (int i = 0; i < leftPoints.size(); i++) {
            Point2D lp = leftPoints.get(i);
            Point2D rp = rightPoints.get(i);

            if (i == 0) {
                leftRail.moveTo(lp.getX(), lp.getY());
                rightRail.moveTo(rp.getX(), rp.getY());
            } else {
                leftRail.lineTo(lp.getX(), lp.getY());
                rightRail.lineTo(rp.getX(), rp.getY());
            }
        }

        // add the stroked rails
        result.append(railStroke.createStrokedShape(leftRail), false);
        result.append(railStroke.createStrokedShape(rightRail), false);

        // calculate total path length and add crossties
        double totalLength = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D curr = points.get(i);
            Point2D next = points.get(i + 1);
            totalLength += curr.distance(next);
        }

        double nextCrosstie = 0;

        while (nextCrosstie < totalLength) {
            double remainingDist = nextCrosstie;
            int segment = 0;
            Point2D p1 = points.get(0);
            Point2D p2 = points.get(1);
            double segmentLength = p1.distance(p2);

            while (remainingDist > segmentLength && segment < points.size() - 2) {
                remainingDist -= segmentLength;
                segment++;
                p1 = points.get(segment);
                p2 = points.get(segment + 1);
                segmentLength = p1.distance(p2);
            }

            double t = remainingDist / segmentLength;
            Point2D crosstieCenter = Geometry.interpolate(p1, p2, t);

            GeneralPath crosstie = new GeneralPath();
            Point2D leftEnd = new Point2D.Double();
            Point2D rightEnd = new Point2D.Double();

            Geometry.calcPerpendicularPoints(crosstieCenter, p2, crosstieLength / 2, leftEnd, rightEnd);

            crosstie.moveTo(leftEnd.getX(), leftEnd.getY());
            crosstie.lineTo(rightEnd.getX(), rightEnd.getY());

            result.append(crosstieStroke.createStrokedShape(crosstie), false);

            nextCrosstie += crosstieSpacing;
        }
    }
}