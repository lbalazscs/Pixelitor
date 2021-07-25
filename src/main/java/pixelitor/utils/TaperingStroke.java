package pixelitor.utils;

import net.jafama.FastMath;

import java.awt.*;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.awt.geom.PathIterator.*;
import static pixelitor.utils.FloatVectorMath.*;

public class TaperingStroke implements Stroke {

    float thickness;
    boolean reverse;

    public TaperingStroke(float thickness) {
        this(thickness, false);
    }

    public TaperingStroke(float thickness, boolean reverse) {
        this.thickness = thickness;
        this.reverse = reverse;
    }

    @Override
    public Shape createStrokedShape(Shape p) {

        final float[] points = new float[6];

        GeneralPath path = new GeneralPath();

        List<float[]> pts = new ArrayList<>();
        float[] lastMoveToPoint = null;

        for (var it = new FlatteningPathIterator(p.getPathIterator(null), 1); !it.isDone(); it.next()) {

            switch (it.currentSegment(points)) {
                case SEG_CLOSE:
                    if (lastMoveToPoint != null)
                        pts.add(lastMoveToPoint);
                    break;
                case SEG_MOVETO:
                    lastMoveToPoint = new float[]{points[0], points[1]};
                    if (!pts.isEmpty()) {
                        if (reverse) Collections.reverse(pts);
                        drawTaperingStroke(pts, path);
                        pts.clear();
                    }
                case SEG_LINETO:
                    pts.add(new float[]{points[0], points[1]});
                    break;
            }
        }


        if (!pts.isEmpty()) {
            if(reverse)Collections.reverse(pts);
            drawTaperingStroke(pts, path);
        }

        return path;
    }

    private void drawTaperingStroke(List<float[]> pts, GeneralPath path) {


        float[] distances = new float[pts.size() - 1];
        float totalDistance = 0;
        for (int i = 0; i < distances.length; i++) {
            float[] a = pts.get(i);
            float[] b = pts.get(i + 1);
            totalDistance += distances[i] = (float) FastMath.hypot(a[0] - b[0], a[1] - b[1]);
        }


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

        float distanceSoFar = distances[0];

        float[][] returnPathf = new float[pts.size() - 1][2];

        returnPathf[0] = Q2;

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
            float requiredThickness = (thickness - thickness * distanceSoFar / totalDistance) / 2;
            distanceSoFar += distances[i - 1];

            // Our method?
            // We will first calculate the perpendiculars of P2P1 and P2P3 through P2
            // Later we can just take the mid points to get the two points Q3 and Q4.

            float[] Perp_P2P1_1 = new float[2];
            float[] Perp_P2P1_2 = new float[2];

            perpendiculars(P2, P1, Perp_P2P1_1, Perp_P2P1_2);

            float[] Perp_P2P3_1 = new float[2];
            float[] Perp_P2P3_2 = new float[2];

            perpendiculars(P2, P3, Perp_P2P3_1, Perp_P2P3_2);

            // third quad point
            float[] Q3 = new float[]{(Perp_P2P1_1[0] + Perp_P2P3_2[0]) / 2, (Perp_P2P1_1[1] + Perp_P2P3_2[1]) / 2};
            // Q3 = Q3 - P2
            subtract(Q3, P2, Q3);

            // fourth quad point
            float[] Q4 = new float[]{(Perp_P2P1_2[0] + Perp_P2P3_1[0]) / 2, (Perp_P2P1_2[1] + Perp_P2P3_1[1]) / 2};
            // Q4 = Q4 - P2
            subtract(Q4, P2, Q4);

            normalize(Q3);
            scale(Q3, requiredThickness);
            add(Q3, P2, Q3);

            normalize(Q4);
            scale(Q4, requiredThickness);
            add(Q4, P2, Q4);

            returnPathf[i] = Q3;
            path.lineTo(Q4[0], Q4[1]);

        }

        float[] Ql = pts.get(pts.size() - 1);
        path.lineTo(Ql[0], Ql[1]);

        for (int i = returnPathf.length - 1; i >= 0; i--) {
            float[] P = returnPathf[i];
            path.lineTo(P[0], P[1]);
        }

        path.closePath();
    }


}
