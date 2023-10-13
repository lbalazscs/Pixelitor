/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import static pixelitor.utils.Geometry.*;

/**
 * A {@link Stroke} implementation that creates a tapered stroke effect.
 */
public class TaperingStroke implements Stroke {
    private final float thickness;
    private final boolean reverse; // switches the tapering direction

    public TaperingStroke(float thickness) {
        this(thickness, false);
    }

    public TaperingStroke(float thickness, boolean reverse) {
        this.thickness = thickness;
        this.reverse = reverse;
    }

    @Override
    public Shape createStrokedShape(Shape shape) {
        float[] coords = new float[6];

        GeneralPath taperedOutline = new GeneralPath();
        List<Point2D> points = new ArrayList<>();

        // processes the path segments of the input shape and
        // tapers the stroke based on the distance along the path.
        for (var it = new FlatteningPathIterator(shape.getPathIterator(null), 1); !it.isDone(); it.next()) {
            switch (it.currentSegment(coords)) {
                case SEG_MOVETO:
                    if (!points.isEmpty()) {
                        if (reverse) {
                            Collections.reverse(points);
                        }
                        createTaperedOutline(points, taperedOutline);
                        points.clear();
                    }
                    //noinspection fallthrough
                case SEG_LINETO:
                    points.add(new Point2D.Float(coords[0], coords[1]));
                    break;
            }
        }

        if (!points.isEmpty()) {
            if (reverse) {
                Collections.reverse(points);
            }
            createTaperedOutline(points, taperedOutline);
        }

        return taperedOutline;
    }

    private void createTaperedOutline(List<Point2D> points, GeneralPath taperedOutline) {
        float[] distances = new float[points.size() - 1];
        float totalDistance = 0;
        for (int i = 0; i < distances.length; i++) {
            Point2D a = points.get(i);
            Point2D b = points.get(i + 1);
            totalDistance += distances[i] = (float) FastMath.hypot(a.getX() - b.getX(), a.getY() - b.getY());
        }

        // first point
        Point2D P1 = points.get(0);
        // second point
        Point2D P2 = points.get(1);

        // first quad point
        Point2D Q1 = new Point2D.Float();
        // second quad point
        Point2D Q2 = new Point2D.Float();

        perpendiculars(P1, P2, thickness / 2, Q1, Q2);

        taperedOutline.moveTo(Q1.getX(), Q1.getY());

        float distanceTraversed = distances[0];

        Point2D[] returnPath = new Point2D[points.size() - 1];

        returnPath[0] = Q2;

        for (int i = 1, s = points.size() - 1; i < s; i++) {
            // For now onwards, P1 will represent (i-1)th and P2 will represent ith point and P3 is (i+1)th point
            // But they can still be called first and second (as of context)
            // THE third point of consideration. the ith point.
            P1 = points.get(i - 1);
            P2 = points.get(i);
            Point2D P3 = points.get(i + 1);

            // Our target?
            // To calculate a line passing through P2
            // such that it is equally aligned with P2P1 and P2P3
            // and has a length equal to thickness - thickness * distanceTraversed / totalDistance
            float requiredThickness = (thickness - thickness * distanceTraversed / totalDistance) / 2;
            distanceTraversed += distances[i - 1];

            // Our method?
            // We will first calculate the perpendiculars of P2P1 and P2P3 through P2
            // Later we can just take the mid-points to get the two points Q3 and Q4.

            var firstPerpendicularToP2P1 = new Point2D.Float();
            var secondPerpendicularToP2P1 = new Point2D.Float();

            perpendiculars(P2, P1, firstPerpendicularToP2P1, secondPerpendicularToP2P1);

            var firstPerpendicularToP2P3 = new Point2D.Float();
            var secondPerpendicularToP2P3 = new Point2D.Float();

            perpendiculars(P2, P3, firstPerpendicularToP2P3, secondPerpendicularToP2P3);

            // third quad point
            Point2D Q3 = new Point2D.Float((firstPerpendicularToP2P1.x + secondPerpendicularToP2P3.x) / 2, (firstPerpendicularToP2P1.y + secondPerpendicularToP2P3.y) / 2);
            // Q3 = Q3 - P2
            subtract(Q3, P2, Q3);

            // fourth quad point
            Point2D Q4 = new Point2D.Float((secondPerpendicularToP2P1.x + firstPerpendicularToP2P3.x) / 2, (secondPerpendicularToP2P1.y + firstPerpendicularToP2P3.y) / 2);
            // Q4 = Q4 - P2
            subtract(Q4, P2, Q4);

            normalize(Q3);
            scale(Q3, requiredThickness);
            add(Q3, P2, Q3);

            normalize(Q4);
            scale(Q4, requiredThickness);
            add(Q4, P2, Q4);

            returnPath[i] = Q3;
            taperedOutline.lineTo(Q4.getX(), Q4.getY());
        }

        Point2D Ql = points.getLast();
        taperedOutline.lineTo(Ql.getX(), Ql.getY());

        for (int i = returnPath.length - 1; i >= 0; i--) {
            Point2D P = returnPath[i];
            taperedOutline.lineTo(P.getX(), P.getY());
        }

        taperedOutline.closePath();
    }
}
