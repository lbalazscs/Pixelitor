/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
 * Geometry-related static utility methods.
 */
public class Geometry {
    public static final double GOLDEN_RATIO = 1.618033988749895;

    private static final double EPSILON = 0.0001;

    private Geometry() {
        // utility class
    }

    /**
     * <P>For two points A and B this function will set the value of outA and outB such that:</P>
     * <OL>
     * <LI>
     * outA and outB are equidistant to A. The distance is dist.
     * </LI>
     * <LI>
     * The line through outA and outB passes through A.
     * </LI>
     * <LI>
     * The line through outA and outB is perpendicular to the line joining A and B.
     * </LI>
     * </OL>
     * <p>
     * <P>With A as center, starting from B, moving clockwise will give outB and moving counter clock wise will give outA.</P>
     *
     * @param a    A 2 valued array representing the abscissa X and ordinate Y of point A.
     * @param b    A 2 valued array representing the abscissa X and ordinate Y of point B.
     * @param dist The distance to be maintained between any one of the perpendicular point and the point A.
     * @param outA A 2 valued empty array which will represent one of the two perpendicular points.
     * @param outB A 2 valued empty array which will represent one of the two perpendicular points.
     */
    public static void perpendiculars(Point2D a, Point2D b, double dist, Point2D outA, Point2D outB) {
        Point2D temp2 = new Point2D.Double();

        // direction with magnitude = B - A
        subtract(b, a, temp2);
        // unit vector
        normalize(temp2);
        // to scale to fit distance
        scale(temp2, dist);

        // outA, outB = temp2 rotated 90 degrees either way
        perpendiculars(temp2, outA, outB);

        // translating the results at A
        add(outA, a, outA);
        add(outB, a, outB);
    }

    /**
     * <P>For two points A and B this function will set the value of outA and outB such that:</P>
     * <OL>
     * <LI>
     * outA and outB are equidistant to A. The distance is 1 unit.
     * </LI>
     * <LI>
     * The line through outA and outB passes through A.
     * </LI>
     * <LI>
     * The line through outA and outB is perpendicular to the line joining A and B.
     * </LI>
     * </OL>
     * <p>
     * <P>With A as center, starting from B, moving clockwise will give outB and moving counter clock wise will give outA.</P>
     *
     * @param a    A 2 valued array representing the abscissa X and ordinate Y of point A.
     * @param b    A 2 valued array representing the abscissa X and ordinate Y of point B.
     * @param outA A 2 valued empty array which will represent one of the two perpendicular points.
     * @param outB A 2 valued empty array which will represent one of the two perpendicular points.
     */
    public static void perpendiculars(Point2D a, Point2D b, Point2D outA, Point2D outB) {
        Point2D temp2 = new Point2D.Double();

        // direction with magnitude = B - A
        subtract(b, a, temp2);
        // unit vector
        normalize(temp2);

        // outA, outB = temp2 rotated 90 degrees either way
        perpendiculars(temp2, outA, outB);

        // translating the results at A
        add(outA, a, outA);
        add(outB, a, outB);
    }

    /**
     * For a point A, this function will set the values of outA and outB such that
     * <OL>
     * <LI>
     * outA is the resulting point vector when A is rotated counter clock wise about origin.
     * </LI>
     * <LI>
     * outB is the resulting point vector when A is rotated clock wise about origin.
     * </LI>
     * </OL>
     *
     * @param a    A 2 valued array representing the abscissa X and ordinate Y of point A.
     * @param outA A 2 valued empty array which will represent one of the two perpendicular points.
     * @param outB A 2 valued empty array which will represent one of the two perpendicular points.
     */
    public static void perpendiculars(Point2D a, Point2D outA, Point2D outB) {
        // first quad point: [Ay, -Ax]
        outA.setLocation(-a.getY(), a.getX());
        // second quad point: [-Ay, Ax]
        outB.setLocation(a.getY(), -a.getX());
    }

    /**
     * <P>For two points A and B, this function will set the values of R such that:</P>
     * <OL>
     * <LI>
     * R lies on the line segment joining A and B.
     * </LI>
     * <LI>
     * R divides AB in the ratio m:n.
     * </LI>
     * <LI>
     * AR / RB == m / n.
     * </LI>
     * </OL>
     * <p>
     *
     * @param a A 2 valued array representing the abscissa X and ordinate Y of point A.
     * @param b A 2 valued array representing the abscissa X and ordinate Y of point B.
     * @param m Former part of the ratio of division.
     * @param n Later part of the ratio of division.
     * @param r A 2 valued empty array which will represent a point dividing AB.
     */
    public static void sectionFormula(Point2D a, Point2D b, double m, double n, Point2D r) {
        add(scale(a, n, new Point2D.Double()), scale(b, m, new Point2D.Double()), r);
        deScale(r, (m + n));
    }

    public static Point2D newFrom(Point2D a) {
        Point2D.Double b = new Point2D.Double();
        b.setLocation(a);
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

    public static void copy(Point2D a, Point2D b) {
        a.setLocation(b.getX(), b.getY());
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

    public static void midPoint(Point2D a, Point2D b, Point2D r) {
        add(a, b, r);
        deScale(r, 2);
    }

    public static Point2D midPoint(Point2D p1, Point2D p2) {
        return new Point2D.Double(
            (p1.getX() + p2.getX()) / 2.0,
            (p1.getY() + p2.getY()) / 2.0
        );
    }

    public static Point2D interpolate(Point2D p1, Point2D p2, double t) {
        return new Point2D.Double(
            ImageMath.lerp(t, p1.getX(), p2.getX()),
            ImageMath.lerp(t, p1.getY(), p2.getY())
        );
    }

    public static void subtract(Point2D a, Point2D b, Point2D r) {
        r.setLocation(a.getX() - b.getX(), a.getY() - b.getY());
    }

    /**
     * Calculate the projected point of the given point on the given line
     *
     * @return projected point p.
     */
    public static Point2D.Double projectPointOnLine(Line2D line, Point2D.Double p) {
        Point2D.Double l1 = (Point2D.Double) line.getP1();
        Point2D.Double l2 = (Point2D.Double) line.getP2();

        // dot product of vectors v1, v2
        Point2D.Double v1 = new Point2D.Double(l2.x - l1.x, l2.y - l1.y);
        Point2D.Double v2 = new Point2D.Double(p.x - l1.x, p.y - l1.y);
        double d = v1.x * v2.x + v1.y * v2.y;

        // squared length of vector v1
        double v1Length = v1.x * v1.x + v1.y * v1.y;
        if (v1Length == 0) {
            return l1;
        }

        return new Point2D.Double(
            (int) (l1.x + (d * v1.x) / v1Length),
            (int) (l1.y + (d * v1.y) / v1Length));
    }

    /**
     * Calculate the line orthogonal to the given line that passes through the point P
     *
     * @return orthogonal line
     */
    public static Line2D orthogonalLineThroughPoint(Line2D line, Point2D.Double p) {
        return new Line2D.Double(p, projectPointOnLine(line, p));
    }

    public static void toRange(Point2D pos, double x1, double y1, double x2, double y2) {
        pos.setLocation(
            FastMath.toRange(x1, x2, pos.getX()),
            FastMath.toRange(y1, y2, pos.getY())
        );
    }

    public static double calcSquaredDistance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return dx * dx + dy * dy;
    }
}
