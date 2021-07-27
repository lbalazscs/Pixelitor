package pixelitor.utils;

import net.jafama.FastMath;

import java.awt.geom.Point2D;

public class Geometry {

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
     * @param A    A 2 valued array representing the abscissa X and ordinate Y of point A.
     * @param B    A 2 valued array representing the abscissa X and ordinate Y of point B.
     * @param dist The distance to be maintained between any one of the perpendicular point and the point A.
     * @param outA A 2 valued empty array which will represent one of the two perpendicular points.
     * @param outB A 2 valued empty array which will represent one of the two perpendicular points.
     */
    public static void perpendiculars(Point2D A, Point2D B, float dist, Point2D outA, Point2D outB) {

        Point2D temp2 = new Point2D.Float();

        // direction with magnitude = B - A
        subtract(B, A, temp2);
        // unit vector
        normalize(temp2);
        // to scale to fit distance
        scale(temp2, dist);

        // outA, outB = temp2 rotated 90 degrees either way
        perpendiculars(temp2, outA, outB);

        // translating the results at A
        add(outA, A, outA);
        add(outB, A, outB);
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
     * @param A    A 2 valued array representing the abscissa X and ordinate Y of point A.
     * @param B    A 2 valued array representing the abscissa X and ordinate Y of point B.
     * @param outA A 2 valued empty array which will represent one of the two perpendicular points.
     * @param outB A 2 valued empty array which will represent one of the two perpendicular points.
     */
    public static void perpendiculars(Point2D A, Point2D B, Point2D outA, Point2D outB) {

        Point2D temp2 = new Point2D.Float();

        // direction with magnitude = B - A
        subtract(B, A, temp2);
        // unit vector
        normalize(temp2);

        // outA, outB = temp2 rotated 90 degrees either way
        perpendiculars(temp2, outA, outB);

        // translating the results at A
        add(outA, A, outA);
        add(outB, A, outB);
    }


    /**
     * For a point A this function will set the values of outA and outB such that
     * <OL>
     * <LI>
     * outA is the resulting point vector when A is rotated counter clock wise about origin.
     * </LI>
     * <LI>
     * outB is the resulting point vector when A is rotated clock wise about origin.
     * </LI>
     * </OL>
     *
     * @param A    A 2 valued array representing the abscissa X and ordinate Y of point A.
     * @param outA A 2 valued empty array which will represent one of the two perpendicular points.
     * @param outB A 2 valued empty array which will represent one of the two perpendicular points.
     */
    public static void perpendiculars(Point2D A, Point2D outA, Point2D outB) {
        // first quad point: [Ay, -Ax]
        outA.setLocation(-A.getY(), A.getX());
        // second quad point: [-Ay, Ax]
        outB.setLocation(A.getY(), -A.getX());
    }

    /**
     * <B>WARNING: It will modify the values of A and B!!!</B>
     *
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
     * @param A A 2 valued array representing the abscissa X and ordinate Y of point A.
     * @param B A 2 valued array representing the abscissa X and ordinate Y of point B.
     * @param m Former part of the ratio of division.
     * @param n Later part of the ratio of division.
     * @param R A 2 valued empty array which will represent a point dividing AB.
     */
    public static void sectionFormula(Point2D A, Point2D B, float m, float n, Point2D R) {
        add(scale(A, n), scale(B, m), R);
        deScale(R, (m + n));
    }

    public static Point2D newFrom(Point2D A) {
        Point2D.Float B = new Point2D.Float();
        B.setLocation(A);
        return B;
    }

    public static void normalize(Point2D A) {
        deScale(A, (float) FastMath.hypot(A.getX(), A.getY()));
    }

    public static void deScale(Point2D A, float factor) {
        A.setLocation(A.getX() / factor, A.getY() / factor);
    }

    public static Point2D scale(Point2D A, float factor) {
        A.setLocation(A.getX() * factor, A.getY() * factor);
        return A;
    }

    public static boolean areEqual(Point2D A, Point2D B) {
        // TODO: will using the epsilon method be any better??
        return Double.compare(A.getX(), B.getX()) == 0 && Double.compare(A.getY(), B.getY()) == 0;
    }

    public static void copy(Point2D A, Point2D B) {
        A.setLocation(B.getX(), B.getY());
    }

    public static float distance(Point2D A, Point2D B) {
        return (float) FastMath.hypot(A.getX() - B.getX(), A.getY()- B.getY());
    }

    public static Point2D add(Point2D A, Point2D B, Point2D R) {
        R.setLocation(A.getX() + B.getX(), A.getY() + B.getY());
        return R;
    }

    public static void midPoint(Point2D A, Point2D B, Point2D R) {
        add(A, B, R);
        deScale(R, 2);
    }

    public static void subtract(Point2D A, Point2D B, Point2D R) {
        R.setLocation(A.getX() - B.getX(), A.getY() - B.getY());
    }
}
