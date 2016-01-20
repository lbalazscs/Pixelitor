/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.filters.gui.RangeParam;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * "Flower of Life" filter
 */
public class FlowerOfLife extends ShapeFilter {
    private final static int GRID_TYPE_TRIANGULAR = 1;
    private final static int GRID_TYPE_SQUARE = 2;
    private final static int GRID_TYPE_SQUARE_2 = 3;

    public static final double SQRT_2 = 1.4142135623730950488016887242097;

    private final RangeParam radius = new RangeParam("Radius", 1, 50, 100);
    private final RangeParam iterations = new RangeParam("Iterations", 1, 3, 10);
    private final IntChoiceParam grid = new IntChoiceParam("Grid Type", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Triangular", GRID_TYPE_TRIANGULAR),
            new IntChoiceParam.Value("Square", GRID_TYPE_SQUARE),
            new IntChoiceParam.Value("Square 2", GRID_TYPE_SQUARE_2)
    });

    public FlowerOfLife() {
        addParamsToFront(
                radius.adjustRangeToImageSize(0.2),
                iterations,
                grid);
    }

    @Override
    protected Shape createShape(int width, int height) {
        Path2D shape = new Path2D.Double();

        double r = radius.getValueAsDouble();
        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        Circle firstCircle = new Circle(cx, cy, r);
        Set<Circle> circles = new HashSet<>();
        circles.add(firstCircle);

        int gridType = grid.getValue();

        int numIterations = iterations.getValue();
        for (int it = 2; it <= numIterations; it++) {
            List<Circle> circlesSoFar = new ArrayList<>(circles);
            for (Circle circle : circlesSoFar) {
                List<Circle> neighbors;
                if (gridType == GRID_TYPE_TRIANGULAR) {
                    neighbors = circle.genTriangleGridNeighbors();
                } else if (gridType == GRID_TYPE_SQUARE) {
                    neighbors = circle.genSquareGridNeighbors();
                } else if (gridType == GRID_TYPE_SQUARE_2) {
                    neighbors = circle.genSquare2GridNeighbors();
                } else {
                    throw new IllegalStateException("gridType = " + gridType);
                }
                circles.addAll(neighbors);
            }
        }

//        int numCircles = circles.size();
//        System.out.println("FlowerOfLife::createShape: numIterations = " + numIterations + ", numCircles = " + numCircles);

        for (Circle circle : circles) {
            shape.append(circle.toShape(), false);
        }

        return shape;
    }

    private static class Circle {
        final double cx;
        final double cy;
        final double r;

        public Circle(double cx, double cy, double r) {
            this.cx = cx;
            this.cy = cy;
            this.r = r;
        }

        List<Circle> genTriangleGridNeighbors() {
            List<Circle> n = new ArrayList<>(6);
            double rowHeight = r * 0.86602540378443864676372317075294; // sqrt(3)/2
            double halfRadius = r / 2;
            n.add(new Circle(cx + r, cy, r)); // right
            n.add(new Circle(cx - r, cy, r)); // left
            n.add(new Circle(cx - halfRadius, cy - rowHeight, r)); // top left
            n.add(new Circle(cx + halfRadius, cy - rowHeight, r)); // top right
            n.add(new Circle(cx - halfRadius, cy + rowHeight, r)); // bottom left
            n.add(new Circle(cx + halfRadius, cy + rowHeight, r)); // bottom right
            return n;
        }

        List<Circle> genSquareGridNeighbors() {
            List<Circle> n = new ArrayList<>(6);
            double distance = r * SQRT_2;
            n.add(new Circle(cx - distance, cy, r)); // left
            n.add(new Circle(cx + distance, cy, r)); // right
            n.add(new Circle(cx, cy - distance, r)); // top
            n.add(new Circle(cx, cy + distance, r)); // bottom
            return n;
        }

        List<Circle> genSquare2GridNeighbors() {
            List<Circle> n = new ArrayList<>(6);
            double distance = r * SQRT_2;
            n.add(new Circle(cx - distance, cy - distance, r)); // top left
            n.add(new Circle(cx + distance, cy - distance, r)); // top right
            n.add(new Circle(cx - distance, cy + distance, r)); // bottom left
            n.add(new Circle(cx + distance, cy + distance, r)); // bottom right
            return n;
        }

        Shape toShape() {
            double d = 2 * r;
            return new Ellipse2D.Double(cx - r, cy - r, d, d);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Circle circle = (Circle) o;
            return Double.compare(circle.cx, cx) == 0 &&
                    Double.compare(circle.cy, cy) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(cx, cy);
        }
    }

    @Override
    protected float getGradientRadius(float cx, float cy) {
        int gridType = grid.getValue();
        float r = radius.getValueAsFloat();
        int it = iterations.getValue();

        if (gridType == GRID_TYPE_TRIANGULAR) {
            return it * r;
        } else if (gridType == GRID_TYPE_SQUARE) {
            return (float) (r + (it - 1) * SQRT_2 * r);
        } else if (gridType == GRID_TYPE_SQUARE_2) {
            return r + (it - 1) * 2 * r;
        } else {
            throw new IllegalStateException("gridType = " + gridType);
        }
    }
}
