package pixelitor.utils;

import net.jafama.FastMath;

import java.awt.*;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;

public class VanishingStroke implements Stroke {

    float thickness = 10;

    final float[] points = new float[6];

    public VanishingStroke() {
    }

    public VanishingStroke(float th) {
        thickness = th;
    }

    @Override
    public Shape createStrokedShape(Shape p) {

        List<float[]> pts = new ArrayList<>();

//        for (FlatteningPathIterator it = new FlatteningPathIterator(new BasicStroke(1).createStrokedShape(p).getPathIterator(null), 1); !it.isDone(); it.next()) {
        for (FlatteningPathIterator it = new FlatteningPathIterator(p.getPathIterator(null), 1); !it.isDone(); it.next()) {
            switch (it.currentSegment(points)) {
                case SEG_MOVETO, SEG_LINETO -> pts.add(new float[]{points[0], points[1]});
            }
        }

        float[] distances = new float[pts.size() - 1];
        float totalDistance = 0;
        for (int i = 0; i < distances.length; i++) {
            float[] a = pts.get(i);
            float[] b = pts.get(i + 1);
            totalDistance += distances[i] = (float) FastMath.hypot(a[0] - b[0], a[1] - b[1]);
        }

        GeneralPath path = new GeneralPath();

        // first point
        float[] P1 = pts.get(0);
        // second point
        float[] P2 = pts.get(1);

        // first quad point
        float[] Q1 = new float[2];
        // second quad point
        float[] Q2 = new float[2];

        perpendiculars(P1, P2, thickness / 2, Q1, Q2);

        path.moveTo(Q1[0], Q1[1]);
//        path.lineTo(Q2[0], Q2[1]);

        float distanceSoFar = distances[0];

        List<float[]> returnPath = new ArrayList<>();
        returnPath.add(Q2);

        for (int i = 1, s = pts.size() - 1; i < s; i++) {

            // For now onwards, P1 will represent (i-1)th and P2 will represent ith point and P3 is (i+1)th point
            // But they can still be called first and second (as of context)
            // THE third point of consideration. the ith point.
            P1 = pts.get(i - 1);
            P2 = pts.get(i);
            float[] P3 = pts.get(i + 1);

            // Our target?
            // To calculate a line passing through P2
            // such that it is equally aligned with P2P1 and P2P3
            // and has a length equal to thickness - thickness * distanceSoFar / totalDistance
            float requiredThickness = thickness - thickness * distanceSoFar / totalDistance;
            distanceSoFar += distances[i - 1];

            // Our method?
            // We will first calculate the perpendiculars of P2P1 and P2P3 through P2
            // Later we can just take the mid points to get the two points Q3 and Q4.

            float[] Perp_P2P1_1 = new float[2];
            float[] Perp_P2P1_2 = new float[2];

            perpendiculars(P2, P1, requiredThickness / 2, Perp_P2P1_1, Perp_P2P1_2);

            float[] Perp_P2P3_1 = new float[2];
            float[] Perp_P2P3_2 = new float[2];

            perpendiculars(P2, P3, requiredThickness / 2, Perp_P2P3_1, Perp_P2P3_2);

            // third quad point
            float[] Q3 = new float[2];
            // fourth quad point
            float[] Q4 = new float[2];

            Q3[0] = (Perp_P2P1_1[0] + Perp_P2P3_2[0]) / 2;
            Q3[1] = (Perp_P2P1_1[1] + Perp_P2P3_2[1]) / 2;

            Q4[0] = (Perp_P2P1_2[0] + Perp_P2P3_1[0]) / 2;
            Q4[1] = (Perp_P2P1_2[1] + Perp_P2P3_1[1]) / 2;

            path.lineTo(Q4[0], Q4[1]);

            returnPath.add(Q3);

//            path.lineTo(Q3[0], Q3[1]);
//            path.lineTo(Q4[0], Q4[1]);
//            path.lineTo(Q1[0], Q1[1]);
//
//            path.moveTo(Q3[0], Q3[1]);
//            path.lineTo(Q4[0], Q4[1]);

            Q1 = Q3;
            Q2 = Q4;
        }

        float[] Ql = pts.get(pts.size() - 1);
        path.lineTo(Ql[0], Ql[1]);
//        path.lineTo(Q4[0], Q4[1]);

        for (int i = returnPath.size()-1; i >=0; i--) {
            float[] P = returnPath.get(i);
            path.lineTo(P[0], P[1]);
        }


        /*GeneralPath path = new GeneralPath();

        FlatteningPathIterator iterator = new FlatteningPathIterator(new BasicStroke(1).createStrokedShape(p).getPathIterator(null), 1);
        FlatteningPathIterator iterator = new FlatteningPathIterator(p.getPathIterator(null), 1);

        float moveX = 0, moveY = 0;
        while (!iterator.isDone()) {
            switch (iterator.currentSegment(points)) {

                case SEG_MOVETO:
                    path.moveTo(moveX = points[0], moveY = points[1]);
                    System.out.println("m "+ Arrays.toString(points));
                    break;

                case SEG_CLOSE:
                    points[0] = moveX;
                    points[1] = moveY;
                    System.out.print("s");

                case SEG_LINETO:
                    path.lineTo(points[0], points[1]);
                    System.out.println("l "+ Arrays.toString(points));
                    break;
            }
            iterator.next();
        }

        path.lineTo(40,30);
        path.closePath();*/


        return path;
    }

    private static float[] temp2 = new float[2];

    public static void perpendiculars(float[] A, float[] B, float distance, float[] outA, float[] outB) {

        // direction with magnitude = B - A
        temp2[0] = A[0] - B[0];
        temp2[1] = A[1] - B[1];
        // unit vector
        float mag = (float) FastMath.hypot(temp2[0], temp2[1]);
        temp2[0] /= mag;
        temp2[1] /= mag;
        // to scale to fit distance
        temp2[0] *= distance;
        temp2[1] *= distance;

        // first quad point: P1 + [Dy, -Dx]
        outA[0] = A[0] + temp2[1];
        outA[1] = A[1] - temp2[0];
        // second quad point: P1 + [-Dy, Dx]
        outB[0] = A[0] - temp2[1];
        outB[1] = A[1] + temp2[0];
    }

    public static void perpendiculars(float[] A, float[] B, float[] outA, float[] outB) {

        // direction with magnitude = B - A
        temp2[0] = A[0] - B[0];
        temp2[1] = A[1] - B[1];
        // unit vector
        float mag = (float) FastMath.hypot(temp2[0], temp2[1]);
        temp2[0] /= mag;
        temp2[1] /= mag;

        // first quad point: P1 + [Dy, -Dx]
        outA[0] = A[0] + temp2[1];
        outA[1] = A[1] - temp2[0];
        // second quad point: P1 + [-Dy, Dx]
        outB[0] = A[0] - temp2[1];
        outB[1] = A[1] + temp2[0];
    }

}
