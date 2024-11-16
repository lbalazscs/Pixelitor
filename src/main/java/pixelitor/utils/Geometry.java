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

import com.jhlabs.image.ImageMath;
import net.jafama.FastMath;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Static utility methods for 2D geometry calculations.
 */
public class Geometry {
    public static final double GOLDEN_RATIO = 1.618033988749895;
    private static final double EPSILON = 0.0001;

    private Geometry() {
        // utility class
    }

    /**
     * Calculates two points that form a perpendicular line through the center,
     * maintaining a specified distance from the center.
     *
     * @param center    The point through which the perpendicular line passes
     * @param reference The reference point used to determine the perpendicular direction
     * @param distance  The distance from center to each output point
     * @param leftOut   Output parameter for the counter-clockwise perpendicular point
     * @param rightOut  Output parameter for the clockwise perpendicular point
     */
    public static void calcPerpendicularPoints(Point2D center,
                                               Point2D reference,
                                               double distance,
                                               Point2D leftOut,
                                               Point2D rightOut) {
        // Calculate direction vector from center to reference
        Point2D direction = new Point2D.Double();
        subtract(reference, center, direction);
        normalize(direction);
        scale(direction, distance);

        // Calculate perpendicular points
        // leftOut, rightOut = direction rotated 90 degrees either way
        calculatePerpendicularVectors(direction, leftOut, rightOut);

        // Translate points to center position
        add(leftOut, center, leftOut);
        add(rightOut, center, rightOut);
    }

    /**
     * The same as the previous one, but here the distance is 1.
     */
    public static void calcPerpendicularPoints(Point2D center,
                                               Point2D reference,
                                               Point2D leftOut,
                                               Point2D rightOut) {
        // Calculate direction vector from center to reference
        Point2D direction = new Point2D.Double();
        subtract(reference, center, direction);
        normalize(direction);

        // Calculate perpendicular points
        // leftOut, rightOut = direction rotated 90 degrees either way
        calculatePerpendicularVectors(direction, leftOut, rightOut);

        // Translate points to center position
        add(leftOut, center, leftOut);
        add(rightOut, center, rightOut);
    }

    /**
     * Calculates two unit vectors perpendicular to the input vector.
     *
     * @param input The input vector
     * @param leftOut Output parameter for counter-clockwise perpendicular vector
     * @param rightOut Output parameter for clockwise perpendicular vector
     */
    public static void calculatePerpendicularVectors(Point2D input,
                                                     Point2D leftOut,
                                                     Point2D rightOut) {
        leftOut.setLocation(-input.getY(), input.getX());
        rightOut.setLocation(input.getY(), -input.getX());
    }

    /**
     * Calculates a point that divides a line segment in a given m:n ratio.
     *
     * @param start First endpoint of the line segment
     * @param end Second endpoint of the line segment
     * @param m First part of the division ratio
     * @param n Second part of the division ratio
     * @param result Output parameter for the calculated point
     */
    public static void calcDivisionPoint(Point2D start,
                                         Point2D end,
                                         double m,
                                         double n,
                                         Point2D resultOut) {
        // https://en.wikipedia.org/wiki/Section_formula
        Point2D scaledStart = scale(start, n, new Point2D.Double());
        Point2D scaledEnd = scale(end, m, new Point2D.Double());
        add(scaledStart, scaledEnd, resultOut);
        deScale(resultOut, (m + n));
    }

    public static Point2D copyPoint(Point2D source) {
        Point2D.Double b = new Point2D.Double();
        b.setLocation(source);
        return b;
    }

    public static void normalize(Point2D a) {
        deScale(a, FastMath.hypot(a.getX(), a.getY()));
    }

    public static Point2D deScale(Point2D a, double factor) {
        deScale(a, factor, a);
        return a;
    }

    public static void deScale(Point2D a, double factor, Point2D r) {
        r.setLocation(a.getX() / factor, a.getY() / factor);
    }

    public static Point2D scale(Point2D a, double factor) {
        return scale(a, factor, a);
    }

    public static Point2D scale(Point2D a, double factor, Point2D r) {
        r.setLocation(a.getX() * factor, a.getY() * factor);
        return r;
    }

    public static Point2D setMagnitude(Point2D a, double factor) {
        normalize(a);
        scale(a, factor);
        return a;
    }

    public static boolean areEqual(Point2D a, Point2D b) {
        return Double.compare(a.getX(), b.getX()) == 0 && Double.compare(a.getY(), b.getY()) == 0;
    }

    public static boolean areEqualByEpsilon(Point2D a, Point2D b) {
        return FastMath.abs(a.getX() - b.getX()) < EPSILON && FastMath.abs(a.getY() - b.getY()) < EPSILON;
    }

    public static void copy(Point2D from, Point2D to) {
        to.setLocation(from.getX(), from.getY());
    }

    public static double distance(Point2D a, Point2D b) {
        return FastMath.hypot(a.getX() - b.getX(), a.getY() - b.getY());
    }

    public static double distance(Point2D a) {
        return FastMath.hypot(a.getX(), a.getY());
    }

    public static double distanceSq(Point2D a) {
        return a.getX() * a.getX() + a.getY() + a.getY();
    }

    public static Point2D add(Point2D a, double add) {
        a.setLocation(a.getX() + add, a.getY() + add);
        return a;
    }

    public static Point2D add(Point2D a, Point2D b) {
        add(a, b, a);
        return a;
    }

    public static Point2D add(Point2D... a) {
        for (int i = 1; i < a.length; i++) {
            add(a[0], a[i], a[0]);
        }
        return a[0];
    }

    public static Point2D add(Point2D a, Point2D b, Point2D r) {
        r.setLocation(a.getX() + b.getX(), a.getY() + b.getY());
        return r;
    }

    public static Point2D midPoint(Point2D p1, Point2D p2) {
        return new Point2D.Double(
            (p1.getX() + p2.getX()) / 2.0,
            (p1.getY() + p2.getY()) / 2.0);
    }

    public static Point2D interpolate(Point2D p1, Point2D p2, double t) {
        return new Point2D.Double(
            ImageMath.lerp(t, p1.getX(), p2.getX()),
            ImageMath.lerp(t, p1.getY(), p2.getY()));
    }

    public static void subtract(Point2D a, Point2D b, Point2D result) {
        result.setLocation(a.getX() - b.getX(), a.getY() - b.getY());
    }

    /**
     * Projects a point onto a line and returns the projected point.
     */
    public static Point2D.Double projectPointToLine(Line2D line, Point2D.Double p) {
        Point2D.Double lineStart = (Point2D.Double) line.getP1();
        Point2D.Double lineEnd = (Point2D.Double) line.getP2();

        Point2D.Double lineVector = new Point2D.Double(
            lineEnd.x - lineStart.x,
            lineEnd.y - lineStart.y);
        Point2D.Double pointVector = new Point2D.Double(
            p.x - lineStart.x,
            p.y - lineStart.y);
        double dotProduct = lineVector.x * pointVector.x + lineVector.y * pointVector.y;

        double lineVectorLengthSq = lineVector.x * lineVector.x + lineVector.y * lineVector.y;
        if (lineVectorLengthSq == 0) {
            return lineStart;
        }

        double t = dotProduct / lineVectorLengthSq;
        return new Point2D.Double(
            lineStart.x + t * lineVector.x,
            lineStart.y + t * lineVector.y);
    }

    /**
     * Returns the line orthogonal to the given line that passes through the point P
     */
    public static Line2D createOrthogonalLine(Line2D line, Point2D.Double p) {
        return new Line2D.Double(p, projectPointToLine(line, p));
    }

    public static void toRange(Point2D pos, double x1, double y1, double x2, double y2) {
        pos.setLocation(
            FastMath.toRange(x1, x2, pos.getX()),
            FastMath.toRange(y1, y2, pos.getY()));
    }

    public static double calcSquaredDistance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    /**
     * Converts polar coordinates to Cartesian coordinates.
     */
    public static Point2D polarToCartesian(double radius, double angle) {
        double x = radius * FastMath.cos(angle);
        double y = radius * FastMath.sin(angle);

        return new Point2D.Double(x, y);
    }

    /**
     * Converts an angle from the [-π, π] range (as returned by Math.atan2)
     * to the [0, 2π] range in counter-clockwise direction.
     */
    public static double atan2ToIntuitive(double angleRadians) {
        return angleRadians <= 0
            ? -angleRadians
            : Math.PI * 2 - angleRadians;
    }

    /**
     * The inverse function of atan2ToIntuitive
     */
    public static double intuitiveToAtan2(double angleRadians) {
        return angleRadians > Math.PI
            ? 2 * Math.PI - angleRadians
            : -angleRadians;
    }

    public static double toIntuitiveDegrees(double angleRadians) {
        double degrees = Math.toDegrees(angleRadians);
        return degrees <= 0
            ? -degrees
            : 360.0 - degrees;
    }
}
