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

import net.jafama.FastMath;
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

    private static final int TYPE_CIRCLE_GRID = 1;
    private static final int TYPE_MAURER_ROSE = 2;
    private static final int TYPE_TIMES_TABLE = 3;
    private static final int TYPE_MYSTIC_ROSE = 4;

    private final IntChoiceParam typeParam = new IntChoiceParam("Type", new Item[]{
        new Item("Circle Grid", TYPE_CIRCLE_GRID),
        new Item("Maurer Rose", TYPE_MAURER_ROSE),
        new Item("Modular Times Table", TYPE_TIMES_TABLE),
        new Item("Mystic Rose", TYPE_MYSTIC_ROSE),
    });
    private final RangeParam numPointsParam = new RangeParam("Number of Points", 3, 18, 200);
    private final RangeParam innerRadiusPercent = new RangeParam("Inner Radius %", 0, 100, 100);
    private final RangeParam tuningParam = new RangeParam("Tuning", -100, 0, 100);

    private double cx;
    private double cy;
    private double outerRadius;

    public CircleWeave() {
//        Predicate<Item> maurer = item -> item.valueIs(TYPE_MAURER_ROSE);
//        Predicate<Item> times = item -> item.valueIs(TYPE_TIMES_TABLE);
//        typeParam.setupDisableOtherIf(innerRadiusPercent, maurer.or(times));
//        typeParam.setupEnableOtherIf(multiplierParam, maurer.or(times));

        typeParam.setupLimitOtherToMax(numPointsParam, selected ->
            switch (selected.value()) {
                case TYPE_CIRCLE_GRID -> 50;
                case TYPE_MAURER_ROSE, TYPE_TIMES_TABLE -> 200;
                case TYPE_MYSTIC_ROSE -> 18;
                default -> throw new IllegalStateException("type = " + selected.value());
            });

        addParamsToFront(
            typeParam,
            numPointsParam,
            innerRadiusPercent,
            tuningParam
        );
    }

    @Override
    protected Path2D createCurve(int width, int height) {
        initGeometry(width, height);

        int numPoints = numPointsParam.getValue();
        int tuning = tuningParam.getValue();
        double innerRatio = innerRadiusPercent.getPercentage();
        int type = typeParam.getValue();
        boolean distorted = transform.hasNonlinDistort();

        return switch (type) {
            case TYPE_CIRCLE_GRID -> createCircleGrid(numPoints, tuning, innerRatio, distorted);
            case TYPE_MAURER_ROSE -> createMaurerRose(numPoints, tuning, innerRatio);
            case TYPE_TIMES_TABLE -> createModularMultiplication(tuning, numPoints, distorted);
            case TYPE_MYSTIC_ROSE -> createMysticRose(numPoints, innerRatio, distorted);
            default -> throw new IllegalStateException("type = " + type);
        };
    }

    private Path2D createCircleGrid(int numPoints, int tuning, double innerRatio, boolean distorted) {
        Point2D[] vertices = calcVertexPoints(numPoints, outerRadius);
        double baseRadius = outerRadius * innerRatio / 2.0;
        Point2D center = new Point2D.Double(cx, cy);

        double radiusTuning = tuning / 100.0;

        Path2D path = new Path2D.Double();
        for (int i = 0; i < vertices.length; i++) {
            double angle = 2 * Math.PI * i / vertices.length;
            double radiusVariation = 1.0 + radiusTuning * Math.sin(angle);
            double radius = baseRadius * radiusVariation;

            addInterpolatedCircle(path, vertices[i], center, innerRatio / 2, radius, distorted);
        }
        return path;
    }

    private static void addInterpolatedCircle(Path2D path, Point2D point, Point2D imageCenter, double t, double radius, boolean distorted) {
        Point2D c = Geometry.interpolate(point, imageCenter, t);
        Shape circle = distorted
            ? Shapes.createCircle(c.getX(), c.getY(), radius, 24)
            : Shapes.createCircle(c.getX(), c.getY(), radius);
        path.append(circle, false);
    }

    private Path2D createMaurerRose(int numPetals, int tuning, double innerRatio) {
        Path2D path = new Path2D.Double();

        int multiplier = 1 + Math.abs(tuning);
        double innerRadius = outerRadius * innerRatio;

        path.moveTo(cx, cy);
        for (int t = 1; t <= 360; t++) {
            double k = multiplier * Math.toRadians(t);
            double r = t % 2 == 0
                ? outerRadius * FastMath.sin(numPetals * k)
                : innerRadius * FastMath.sin(numPetals * k);
            double x = cx + r * FastMath.cos(k);
            double y = cy + r * FastMath.sin(k);
            path.lineTo(x, y);
        }

        path.closePath();
        return path;
    }

    private Path2D createMysticRose(int numPoints, double innerRadiusRatio, boolean distorted) {
        Point2D[] vertices = calcVertexPoints(numPoints, outerRadius);
        Point2D[] innerVertices = innerRadiusRatio == 1.0
            ? vertices
            : calcVertexPoints(numPoints, outerRadius * innerRadiusRatio);

        Path2D path = new Path2D.Double();
        double tuning = tuningParam.getValue() / 100.0;

        boolean evenPoints = numPoints % 2 == 0;
        int halfNumPoints = numPoints / 2;

        for (int i = 0; i < numPoints; i++) {
            Point2D from = vertices[i];
            for (int j = 0; j < numPoints; j++) {
                if (innerRadiusRatio == 1.0 && i >= j) {
                    continue; // avoid duplicate lines
                }

                Point2D to = innerVertices[j];
                path.moveTo(from.getX(), from.getY());

                if (tuning == 0 || i == j) {
                    Shapes.elasticLine(path, from, to, distorted);
                    continue;
                }
                if (evenPoints && Math.abs(i - j) == halfNumPoints) {
                    // draw straight line if the indices are opposite
                    Shapes.elasticLine(path, from, to, distorted);
                    continue;
                }

                // for a positive tuning value, the curve always bends outward
                // from the center, and for a negative value, it always bends inward
                double curvature = ((j - i + numPoints) % numPoints) > halfNumPoints
                    ? tuning
                    : -tuning;
                Shapes.curvedLine(path, curvature, from, to);
            }
        }
        return path;
    }

    private Path2D createModularMultiplication(int tuning, int numPoints, boolean distorted) {
        int multiplier = 1 + Math.abs(tuning);

        double innerRadiusRatio = innerRadiusPercent.getPercentage();
        Point2D[] vertices = calcVertexPoints(numPoints, outerRadius);
        Point2D[] innerVertices = innerRadiusRatio == 1.0
            ? vertices
            : calcVertexPoints(numPoints, outerRadius * innerRadiusRatio);

        Path2D path = new Path2D.Double();
        for (int i = 0; i < numPoints; i++) {
            int j = (multiplier * i) % numPoints;
            Point2D from = vertices[i];
            Point2D to = i % 2 == 0
                ? vertices[j]
                : innerVertices[j];
            path.moveTo(from.getX(), from.getY());
            Shapes.elasticLine(path, from, to, distorted);
        }
        return path;
    }

    private Point2D[] calcVertexPoints(int numPoints, double radius) {
        Point2D[] points = new Point2D[numPoints];

        double angleIncrement = 2 * Math.PI / numPoints;
        for (int i = 0; i < numPoints; i++) {
            double angle = i * angleIncrement - Math.PI / 2.0;
            points[i] = new Point2D.Double(
                cx + radius * Math.cos(angle),
                cy + radius * Math.sin(angle));
        }
        return points;
    }

    private void initGeometry(int width, int height) {
        outerRadius = Math.min(width, height) * 0.45;

        cx = transform.getCx(width);
        cy = transform.getCy(height);
    }
}