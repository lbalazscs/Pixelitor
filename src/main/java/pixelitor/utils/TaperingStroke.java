package pixelitor.utils;

import net.jafama.FastMath;

import java.awt.*;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.awt.geom.PathIterator.*;
import static pixelitor.utils.Geometry.*;

public class TaperingStroke implements Stroke {

    private final float thickness;
    private final boolean reverse;

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

        List<Point2D> pts = new ArrayList<>();
        Point2D lastMoveToPoint = null;

        for (var it = new FlatteningPathIterator(p.getPathIterator(null), 1); !it.isDone(); it.next()) {

            switch (it.currentSegment(points)) {
                case SEG_CLOSE:
                    if (lastMoveToPoint != null)
                        pts.add(lastMoveToPoint);
                    break;
                case SEG_MOVETO:
                    lastMoveToPoint = new Point2D.Float(points[0], points[1]);
                    if (!pts.isEmpty()) {
                        if (reverse) Collections.reverse(pts);
                        drawTaperingStroke(pts, path);
                        pts.clear();
                    }
                case SEG_LINETO:
                    pts.add(new Point2D.Float(points[0], points[1]));
                    break;
            }
        }


        if (!pts.isEmpty()) {
            if (reverse) Collections.reverse(pts);
            drawTaperingStroke(pts, path);
        }

        return path;
    }

    private void drawTaperingStroke(List<Point2D> pts, GeneralPath path) {


        float[] distances = new float[pts.size() - 1];
        float totalDistance = 0;
        for (int i = 0; i < distances.length; i++) {
            Point2D a = pts.get(i);
            Point2D b = pts.get(i + 1);
            totalDistance += distances[i] = (float) FastMath.hypot(a.getX() - b.getX(), a.getY() - b.getY());
        }


        // first point
        Point2D P1 = pts.get(0);
        // second point
        Point2D P2 = pts.get(1);

        // first quad point
        Point2D Q1 = new Point2D.Float();
        // second quad point
        Point2D Q2 = new Point2D.Float();

        perpendiculars(P1, P2, thickness / 2, Q1, Q2);

        path.moveTo(Q1.getX(), Q1.getY());

        float distanceSoFar = distances[0];

        Point2D[] returnPath = new Point2D[pts.size() - 1];

        returnPath[0] = Q2;

        for (int i = 1, s = pts.size() - 1; i < s; i++) {

            // For now onwards, P1 will represent (i-1)th and P2 will represent ith point and P3 is (i+1)th point
            // But they can still be called first and second (as of context)
            // THE third point of consideration. the ith point.
            P1 = pts.get(i - 1);
            P2 = pts.get(i);
            Point2D P3 = pts.get(i + 1);

            // Our target?
            // To calculate a line passing through P2
            // such that it is equally aligned with P2P1 and P2P3
            // and has a length equal to thickness - thickness * distanceSoFar / totalDistance
            float requiredThickness = (thickness - thickness * distanceSoFar / totalDistance) / 2;
            distanceSoFar += distances[i - 1];

            // Our method?
            // We will first calculate the perpendiculars of P2P1 and P2P3 through P2
            // Later we can just take the mid points to get the two points Q3 and Q4.

            var perpendicularToP2P1_1 = new Point2D.Float();
            var perpendicularToP2P1_2 = new Point2D.Float();

            perpendiculars(P2, P1, perpendicularToP2P1_1, perpendicularToP2P1_2);

            var perpendicularToP2P3_1 = new Point2D.Float();
            var perpendicularToP2P3_2 = new Point2D.Float();

            perpendiculars(P2, P3, perpendicularToP2P3_1, perpendicularToP2P3_2);

            // third quad point
            Point2D Q3 = new Point2D.Float((perpendicularToP2P1_1.x + perpendicularToP2P3_2.x) / 2, (perpendicularToP2P1_1.y + perpendicularToP2P3_2.y) / 2);
            // Q3 = Q3 - P2
            subtract(Q3, P2, Q3);

            // fourth quad point
            Point2D Q4 = new Point2D.Float((perpendicularToP2P1_2.x + perpendicularToP2P3_1.x) / 2, (perpendicularToP2P1_2.y + perpendicularToP2P3_1.y) / 2);
            // Q4 = Q4 - P2
            subtract(Q4, P2, Q4);

            normalize(Q3);
            scale(Q3, requiredThickness);
            add(Q3, P2, Q3);

            normalize(Q4);
            scale(Q4, requiredThickness);
            add(Q4, P2, Q4);

            returnPath[i] = Q3;
            path.lineTo(Q4.getX(), Q4.getY());

        }

        Point2D Ql = pts.get(pts.size() - 1);
        path.lineTo(Ql.getX(), Ql.getY());

        for (int i = returnPath.length - 1; i >= 0; i--) {
            Point2D P = returnPath[i];
            path.lineTo(P.getX(), P.getY());
        }

        path.closePath();
    }


}
