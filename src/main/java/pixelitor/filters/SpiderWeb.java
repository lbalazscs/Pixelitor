/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.FilterButtonModel;
import pixelitor.filters.gui.RangeParam;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.Serial;
import java.util.Random;

/**
 * A filter that renders a spider web.
 */
public class SpiderWeb extends CurveFilter {
    @Serial
    private static final long serialVersionUID = 3170284746386621728L;

    public static final String NAME = "Spider Web";

    private final RangeParam numBranchesParam =
        new RangeParam("Number of Branches", 3, 12, 100);
    private final RangeParam numConnectionsParam =
        new RangeParam("Number of Connections", 0, 6, 50);
    private final RangeParam curvatureParam =
        new RangeParam("Curvature", -10, 2, 10);
    private final RangeParam randomnessParam =
        new RangeParam("Randomness", 0, 0, 100);

    public SpiderWeb() {
        FilterButtonModel reseedAction = paramSet.createReseedAction("", "Reseed Randomness");

        randomnessParam.setupEnableOtherIfNotZero(reseedAction);

        addParamsToFront(
            numBranchesParam,
            numConnectionsParam,
            curvatureParam,
            randomnessParam.withSideButton(reseedAction)
        );
    }

    @Override
    protected Path2D createCurve(int width, int height) {
        Path2D path = new Path2D.Double();

        Random random = paramSet.getLastSeedRandom();
        double randomness = randomnessParam.getValue() / 100.0;

        double cx = transform.getCx(width);
        double cy = transform.getCy(height);
        Point2D center = new Point2D.Double(cx, cy);

        int numConnections = numConnectionsParam.getValue();
        int numBranches = numBranchesParam.getValue();
        double radius = Math.min(width / 2.0, height / 2.0);

        Point2D.Double[][] connectionPoints = calcConnectionPoints(
            numBranches, numConnections, radius, cx, cy, random, randomness);

        // draw the branches
        for (int br = 0; br < numBranches; br++) {
            path.moveTo(cx, cy);

            // draw a branch as multiple segments using the precalculated points
            for (int conn = 0; conn < numConnections; conn++) {
                Point2D.Double point = connectionPoints[br][conn];
                path.lineTo(point.x, point.y);
            }
        }

        // draw the connections
        int curvature = curvatureParam.getValue();
        for (int conn = 0; conn < numConnections; conn++) {
            Point2D startPoint = null;
            Point2D prevPoint = null;

            for (int br = 0; br < numBranches; br++) {
                Point2D.Double point = connectionPoints[br][conn];

                if (br == 0) {
                    startPoint = point;
                    path.moveTo(point.x, point.y);
                } else {
                    connect(prevPoint, point, center, path, curvature, random, randomness);
                }
                prevPoint = point;
            }
            connect(prevPoint, startPoint, center, path, curvature, random, randomness);
        }

        path.closePath();
        return path;
    }

    private static void connect(Point2D start, Point2D end, Point2D center,
                                Path2D shape, int curvature,
                                Random random, double randomness) {
        double startX = start.getX();
        double startY = start.getY();
        double endX = end.getX();
        double endY = end.getY();

        if (curvature == 0) {
            shape.lineTo(endX, endY);
        } else {
            double midX = (startX + endX) / 2;
            double midY = (startY + endY) / 2;

            if (randomness > 0) {
                double maxOffset = Math.min(Math.abs(startX - endX),
                    Math.abs(startY - endY)) * 0.1 * randomness;
                midX += perturb(maxOffset, random);
                midY += perturb(maxOffset, random);
            }

            double centerPull = curvature * 0.1;
            double controlX = ImageMath.lerp(centerPull, midX, center.getX());
            double controlY = ImageMath.lerp(centerPull, midY, center.getY());

            shape.quadTo(controlX, controlY, endX, endY);
        }
    }

    private static Point2D.Double[][] calcConnectionPoints(int numBranches, int numConnections,
                                                           double baseRadius, double cx, double cy,
                                                           Random random, double randomness) {
        Point2D.Double[][] points = new Point2D.Double[numBranches][numConnections];
        double angleIncrement = 2 * Math.PI / numBranches;

        double maxAngleDeviation = angleIncrement * 0.45; // prevent crossing
        double maxRadiusDeviation = baseRadius * 0.1;

        for (int br = 0; br < numBranches; br++) {
            double baseAngle = br * angleIncrement;
            double angle = randomizeValue(baseAngle, maxAngleDeviation, randomness, random);

            double branchRadius = randomizeValue(baseRadius, maxRadiusDeviation, randomness, random);
            double connectionSpacing = branchRadius / numConnections;

            double cos = FastMath.cos(angle);
            double sin = FastMath.sin(angle);

            for (int conn = 0; conn < numConnections; conn++) {
                double baseDistance = branchRadius * (conn + 1) / numConnections;

                double maxDeviation = connectionSpacing * 0.45; // prevent crossing
                double distance = randomizeValue(baseDistance, maxDeviation, randomness, random);

                double x = distance * cos + cx;
                double y = distance * sin + cy;
                points[br][conn] = new Point2D.Double(x, y);
            }
        }
        return points;
    }

    private static double randomizeValue(double baseValue, double maxDeviation, double randomness, Random random) {
        if (randomness == 0) {
            return baseValue;
        }
        double actualDeviation = maxDeviation * randomness;
        return baseValue + perturb(actualDeviation, random);
    }

    /**
     * Returns a random value between -maxDeviation
     * and +maxDeviation with uniform distribution.
     */
    private static double perturb(double maxDeviation, Random random) {
        return (random.nextDouble() * 2 - 1) * maxDeviation;
    }
}