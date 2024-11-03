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

package pixelitor.utils;

import net.jafama.FastMath;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static pixelitor.utils.Geometry.add;
import static pixelitor.utils.Geometry.calcPerpendicularPoints;
import static pixelitor.utils.Geometry.normalize;
import static pixelitor.utils.Geometry.scale;
import static pixelitor.utils.Geometry.subtract;

/**
 * A {@link Stroke} implementation that creates a stroke that gradually tapers along its path.
 * The stroke width decreases linearly from the starting point to the endpoint of the path.
 * The tapering direction can be reversed to make the stroke taper from thin to thick instead.
 */
public class TaperingStroke implements Stroke {
    private final float maxStrokeWidth;
    private final boolean reverse; // switches the tapering direction

    public TaperingStroke(float maxStrokeWidth) {
        this(maxStrokeWidth, false);
    }

    public TaperingStroke(float maxStrokeWidth, boolean reverse) {
        if (maxStrokeWidth <= 0) {
            throw new IllegalArgumentException("maxStrokeWidth = " + maxStrokeWidth);
        }
        this.maxStrokeWidth = maxStrokeWidth;
        this.reverse = reverse;
    }

    @Override
    public Shape createStrokedShape(Shape shape) {
        float[] coords = new float[6];

        // Will hold the final shape of the tapered stroke
        GeneralPath taperedOutline = new GeneralPath();

        // Collects points along the path before processing
        List<Point2D> points = new ArrayList<>();

        var it = new FlatteningPathIterator(shape.getPathIterator(null), 1);
        while (!it.isDone()) {
            switch (it.currentSegment(coords)) {
                case SEG_MOVETO: // Start of a new subpath
                    // Process any existing points for the last subpath
                    if (!points.isEmpty()) {
                        if (reverse) {
                            Collections.reverse(points);
                        }
                        createSubpathOutline(points, taperedOutline);
                        points.clear();
                    }
                    // fallthrough: add the MOVETO point just like LINETO
                    // noinspection fallthrough
                case SEG_LINETO:
                    // Store the point
                    points.add(new Point2D.Float(coords[0], coords[1]));
                    break;
            }
            it.next();
        }

        // Process any remaining points for the last subpath
        if (!points.isEmpty()) {
            if (reverse) {
                Collections.reverse(points);
            }
            createSubpathOutline(points, taperedOutline);
        }

        return taperedOutline;
    }

    // creates the tapered outline for a subpath,
    // always starting from the full width and going to zero
    private void createSubpathOutline(List<Point2D> points, GeneralPath result) {
        // Calculate segment lengths and total subpath length
        float[] segmentLengths = new float[points.size() - 1];
        float totalPathLength = 0;
        for (int i = 0; i < segmentLengths.length; i++) {
            Point2D a = points.get(i);
            Point2D b = points.get(i + 1);
            totalPathLength += segmentLengths[i] = (float) FastMath.hypot(a.getX() - b.getX(), a.getY() - b.getY());
        }

        // Handle the first segment of the subpath
        Point2D startPoint = points.get(0);
        Point2D secondPoint = points.get(1);
        Point2D leftPoint = new Point2D.Float();
        Point2D rightPoint = new Point2D.Float();
        float initialDist = maxStrokeWidth / 2;
        calcPerpendicularPoints(startPoint, secondPoint, initialDist, leftPoint, rightPoint);

        // Start the outline path
        result.moveTo(leftPoint.getX(), leftPoint.getY());
        float distanceCovered = segmentLengths[0];

        // Store points for the return path
        Point2D[] returnPath = new Point2D[points.size() - 1];
        returnPath[0] = rightPoint;

        // Process intermediate points
        for (int i = 1, s = points.size() - 1; i < s; i++) {
            Point2D prevPoint = points.get(i - 1);
            Point2D currentPoint = points.get(i);
            Point2D nextPoint = points.get(i + 1);

            // Temporary points for storing perpendicular vectors
            var prevPerp1 = new Point2D.Float();
            var prevPerp2 = new Point2D.Float();
            var nextPerp1 = new Point2D.Float();
            var nextPerp2 = new Point2D.Float();

            // stroke width at this point based on distance covered
            float currentWidth = (maxStrokeWidth - maxStrokeWidth * distanceCovered / totalPathLength) / 2;
            distanceCovered += segmentLengths[i - 1];

            // Calculate perpendicular points for both segments meeting at current point
            calcPerpendicularPoints(currentPoint, prevPoint, prevPerp1, prevPerp2);
            calcPerpendicularPoints(currentPoint, nextPoint, nextPerp1, nextPerp2);

            // Average the perpendicular vectors to create smooth transitions
            Point2D avgLeft = new Point2D.Float((prevPerp1.x + nextPerp2.x) / 2, (prevPerp1.y + nextPerp2.y) / 2);
            Point2D avgRight = new Point2D.Float((prevPerp2.x + nextPerp1.x) / 2, (prevPerp2.y + nextPerp1.y) / 2);

            // Normalize and scale the vectors to current stroke width
            normalizeAndScale(avgLeft, currentPoint, currentWidth);
            normalizeAndScale(avgRight, currentPoint, currentWidth);

            // Store for return path
            returnPath[i] = avgLeft;

            // Add to forward path
            result.lineTo(avgRight.getX(), avgRight.getY());
        }

        // Complete the outline by going to the endpoint
        Point2D last = points.getLast();
        result.lineTo(last.getX(), last.getY());

        // Draw the return path back to start
        for (int i = returnPath.length - 1; i >= 0; i--) {
            Point2D P = returnPath[i];
            result.lineTo(P.getX(), P.getY());
        }

        result.closePath();
    }

    private static void normalizeAndScale(Point2D avgLeft, Point2D currentPoint, float currentWidth) {
        // Convert to vector from current point
        subtract(avgLeft, currentPoint, avgLeft);

        normalize(avgLeft);
        scale(avgLeft, currentWidth);

        // Move back to absolute position
        add(avgLeft, currentPoint, avgLeft);
    }
}
