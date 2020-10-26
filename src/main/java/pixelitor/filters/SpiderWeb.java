/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import com.jhlabs.image.ImageMath;
import net.jafama.FastMath;
import pixelitor.filters.gui.RangeParam;

import java.awt.geom.Path2D;

/**
 * A shape filter rendering a Lissajous curve
 *
 * See https://en.wikipedia.org/wiki/Lissajous_curve
 */
public class SpiderWeb extends ShapeFilter {
    public static final String NAME = "Spider Web";

    private final RangeParam numBranchesParam =
        new RangeParam("Number of Branches", 3, 12, 100);
    private final RangeParam numConnectionsParam =
        new RangeParam("Number of Connections", 0, 6, 50);
    private final RangeParam curvatureParam =
        new RangeParam("Curvature", -10, 2, 10);
    private final RangeParam rotate =
        new RangeParam("Rotate", 0, 0, 100);

    public SpiderWeb() {
        addParamsToFront(
            numBranchesParam,
            numConnectionsParam,
            curvatureParam,
            rotate
        );
    }

    @Override
    protected Path2D createShape(int width, int height) {
        Path2D shape = new Path2D.Double();

        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();
        double w = width / 2.0;
        double h = height / 2.0;

        int numBranches = numBranchesParam.getValue();
        double radius = Math.min(w, h);
        double angle = 2 * Math.PI / numBranches;
        double startAngle = angle * rotate.getPercentageValD();

        double[] cos = new double[numBranches];
        double[] sin = new double[numBranches];

        // draw the branches
        for (int br = 0; br < numBranches; br++) {
            shape.moveTo(cx, cy);

            double alpha = startAngle + br * angle;
            cos[br] = FastMath.cos(alpha);
            sin[br] = FastMath.sin(alpha);
            double x = radius * cos[br] + cx;
            double y = radius * sin[br] + cy;
            shape.lineTo(x, y);
        }

        // draw the connections
        int numConnections = numConnectionsParam.getValue();
        for (int conn = 1; conn <= numConnections; conn++) {
            double connDist = radius * conn / numConnections;
            double startX = 0;
            double startY = 0;
            double prevX = 0;
            double prevY = 0;
            for (int br = 0; br < numBranches; br++) {
                double x = connDist * cos[br] + cx;
                double y = connDist * sin[br] + cy;
                if (br == 0) {
                    startX = x;
                    startY = y;
                    shape.moveTo(x, y);
                } else {
                    connect(prevX, prevY, x, y, shape, cx, cy);
                }
                prevX = x;
                prevY = y;
            }
            connect(prevX, prevY, startX, startY, shape, cx, cy);
        }

        shape.closePath();

        return shape;
    }

    private void connect(double prevX, double prevY, double toX, double toY,
                         Path2D shape, double cx, double cy) {
        int curvature = curvatureParam.getValue();
        if (curvature == 0) {
            shape.lineTo(toX, toY);
        } else {
            double midX = (prevX + toX) / 2;
            double midY = (prevY + toY) / 2;
            double centerPull = curvature * 0.1;
            double controlX = ImageMath.lerp(centerPull, midX, cx);
            double controlY = ImageMath.lerp(centerPull, midY, cy);

            shape.quadTo(controlX, controlY, toX, toY);
        }
    }
}