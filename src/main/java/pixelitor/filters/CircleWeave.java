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

package pixelitor.filters;

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.Geometry;
import pixelitor.utils.Shapes;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * The "Circle Weave" filter.
 */
public class CircleWeave extends CurveFilter {
    public static final String NAME = "Circle Weave";

    private static final int TYPE_MYSTIC_ROSE = 0;
    private static final int TYPE_CIRCLE_GRID = 1;
    private static final int TYPE_TIMES_TABLE = 2;

    private final IntChoiceParam typeParam = new IntChoiceParam("Type", new Item[]{
        new Item("Mystic Rose", TYPE_MYSTIC_ROSE),
        new Item("Circle Grid", TYPE_CIRCLE_GRID),
        new Item("Modular Times Table", TYPE_TIMES_TABLE),
    });
    private final RangeParam numPoints = new RangeParam("Number of Points", 3, 16, 200);
    private final RangeParam mParam = new RangeParam("Multiplier", 1, 2, 100);

    public CircleWeave() {
        typeParam.setupEnableOtherIf(mParam, item -> item.valueIs(TYPE_TIMES_TABLE));
        addParamsToFront(
            typeParam,
            numPoints,
            mParam
        );
    }

    @Override
    protected Path2D createCurve(int width, int height) {
        Point2D[] points = calcPoints(width, height, numPoints.getValue());

        int m = mParam.getValue();
        int type = typeParam.getValue();
        boolean nonlin = hasNonlinDistort();

        return switch (type) {
            case TYPE_MYSTIC_ROSE -> createMysticRose(points, nonlin);
            case TYPE_CIRCLE_GRID -> createCircles(points, nonlin, width, height);
            case TYPE_TIMES_TABLE -> createTimesTable(m, points, nonlin);
            default -> throw new IllegalStateException("type = " + type);
        };
    }

    private static Path2D createMysticRose(Point2D[] points, boolean nonlin) {
        Path2D path = new Path2D.Double();

        for (int i = 0, numPoints = points.length; i < numPoints; i++) {
            Point2D from = points[i];
            for (int j = 0; j < points.length; j++) {
                if (i > j) { // draw only in one direction
                    Point2D to = points[j];
                    path.moveTo(from.getX(), from.getY());
                    Shapes.elasticLine(path, from, to, nonlin);
                }
            }
        }
        return path;
    }

    private Path2D createCircles(Point2D[] points, boolean nonlin, int width, int height) {
        double radius = getRadius(width, height) / 2.0;
        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();
        Point2D imageCenter = new Point2D.Double(cx, cy);

        Path2D path = new Path2D.Double();
        for (Point2D point : points) {
            Point2D c = Geometry.midPoint(point, imageCenter);

            Shape circle = nonlin ?
                Shapes.createCircle(c.getX(), c.getY(), radius, 24)
                : Shapes.createCircle(c.getX(), c.getY(), radius);
            path.append(circle, false);
        }
        return path;
    }

    private static Path2D createTimesTable(int m, Point2D[] points, boolean nonlin) {
        Path2D path = new Path2D.Double();
        int numPoints = points.length;
        for (int i = 0; i < numPoints; i++) {
            int j = (m * i) % numPoints;
            Point2D from = points[i];
            Point2D to = points[j];
            path.moveTo(from.getX(), from.getY());
            Shapes.elasticLine(path, from, to, nonlin);
        }
        return path;
    }

    private Point2D[] calcPoints(int width, int height, int numPoints) {
        Point2D[] points = new Point2D[numPoints];
        double r = getRadius(width, height);
        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        double angleIncrement = 2 * Math.PI / numPoints;
        for (int i = 0; i < points.length; i++) {
            double angle = i * angleIncrement - Math.PI / 2.0;
            points[i] = new Point2D.Double(
                cx + r * Math.cos(angle),
                cy + r * Math.sin(angle));
        }
        return points;
    }

    private static double getRadius(int width, int height) {
        return Math.min(width, height) * 0.45;
    }
}