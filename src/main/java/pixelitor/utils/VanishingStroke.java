package pixelitor.utils;

import java.awt.*;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;

import static java.awt.geom.PathIterator.*;

public class VanishingStroke implements Stroke {

    final float[] points = new float[6];

    @Override
    public Shape createStrokedShape(Shape p) {
        FlatteningPathIterator iterator = new FlatteningPathIterator(new BasicStroke(1).createStrokedShape(p).getPathIterator(null), 1);

        GeneralPath path = new GeneralPath();
        float moveX = 0, moveY = 0;

        while (!iterator.isDone()) {
            switch (iterator.currentSegment(points)) {

                case SEG_MOVETO:
                    path.moveTo(moveX = points[0], moveY = points[1]);
                    break;

                case SEG_CLOSE:
                    points[0] = moveX;
                    points[1] = moveY;

                case SEG_LINETO:
                    path.lineTo(points[0], points[1]);
                    break;
            }
            iterator.next();
        }

        return path;
    }

}
