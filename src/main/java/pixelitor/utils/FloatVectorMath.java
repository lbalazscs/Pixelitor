package pixelitor.utils;

import net.jafama.FastMath;

public class FloatVectorMath {

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
    public static void perpendiculars(float[] A, float[] B, float dist, float[] outA, float[] outB) {

        float[] temp2 = new float[2];

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
    public static void perpendiculars(float[] A, float[] B, float[] outA, float[] outB) {

        float[] temp2 = new float[2];

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
    public static void perpendiculars(float[] A, float[] outA, float[] outB) {
        // first quad point: [Ay, -Ax]
        outA[0] = -A[1];
        outA[1] = A[0];
        // second quad point: [-Ay, Ax]
        outB[0] = A[1];
        outB[1] = -A[0];
    }

    /**
     * <B>WARNING: It will modify the values of A and B!!!</B>
     *
     * <P>For two points A and B, this function will set the values of R such that:</P>
     * <OL>
     * <LI>
     * R lyes on the line segment joining A and B.
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
    public static void sectionFormula(float[] A, float[] B, float m, float n, float[] R) {
        add(scale(A, n), scale(B, m), R);
        deScale(R, (m + n));
    }

    public static void normalize(float[] A) {
        deScale(A, (float) FastMath.hypot(A[0], A[1]));
    }

    public static void deScale(float[] A, float factor) {
        A[0] /= factor;
        A[1] /= factor;
    }

    public static float[] scale(float[] A, float factor) {
        A[0] *= factor;
        A[1] *= factor;
        return A;
    }

    public static boolean areEqual(float[] A, float[] B) {
        // TODO: will using the epsilon method be any better??
        return Float.compare(A[0], B[0]) == 0 && Float.compare(A[1], B[1]) == 0;
    }

    public static float distance(float[] A, float[] B) {
        return (float) FastMath.hypot(A[0] - B[0], A[1] - B[1]);
    }

    public static float[] add(float[] A, float[] B, float[] R) {
        R[0] = A[0] + B[0];
        R[1] = A[1] + B[1];
        return R;
    }

    public static void midPoint(float[] A, float[] B, float[] R) {
        add(A, B, R);
        deScale(R, 2);
    }

    public static void subtract(float[] A, float[] B, float[] R) {
        R[0] = A[0] - B[0];
        R[1] = A[1] - B[1];
    }
}
