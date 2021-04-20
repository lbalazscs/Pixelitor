/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.GUIText;
import pixelitor.utils.Shapes;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.*;

/**
 * "Flower of Life" shape filter
 */
public class FlowerOfLife extends ShapeFilter {
    private static final int GRID_TYPE_TRIANGULAR = 1;
    private static final int GRID_TYPE_SQUARE = 2;
    private static final int GRID_TYPE_SQUARE_2 = 3;

    private static final double SQRT_2 = 1.4142135623730951;

    private final RangeParam radius = new RangeParam(GUIText.RADIUS, 1, 50, 100);
    private final RangeParam iterations = new RangeParam("Iterations", 1, 3, 10);
    private final IntChoiceParam grid = new IntChoiceParam("Grid Type", new Item[]{
        new Item("Triangular", GRID_TYPE_TRIANGULAR),
        new Item("Square", GRID_TYPE_SQUARE),
        new Item("Square 2", GRID_TYPE_SQUARE_2)
    });

    public FlowerOfLife() {
        addParamsToFront(
            radius.withAdjustedRange(0.2),
            iterations,
            grid);

        helpURL = "https://en.wikipedia.org/wiki/Overlapping_circles_grid";
    }

    @Override
    protected Shape createShape(int width, int height) {
        Path2D shape = new Path2D.Double();

        double r = radius.getValueAsDouble();
        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        Circle firstCircle = new Circle(cx, cy, r);
        Set<Circle> circlesSet = new HashSet<>();
        circlesSet.add(firstCircle);

        int gridType = grid.getValue();
        int numIterations = iterations.getValue();

        for (int it = 2; it <= numIterations; it++) {
            List<Circle> circlesSoFar = new ArrayList<>(circlesSet);
            for (Circle circle : circlesSoFar) {
                circlesSet.addAll(circle.calcNeighbors(gridType));
            }
        }

        for (Circle circle : circlesSet) {
            shape.append(circle.toShape(), false);
        }

        return shape;
    }

    private record Circle(double cx, double cy, double r) {
        private List<Circle> calcNeighbors(int gridType) {
            return switch (gridType) {
                case GRID_TYPE_TRIANGULAR -> calcTriangleGridNeighbors();
                case GRID_TYPE_SQUARE -> calcSquareGridNeighbors();
                case GRID_TYPE_SQUARE_2 -> calcSquare2GridNeighbors();
                default -> throw new IllegalStateException("gridType = " + gridType);
            };
        }

        private List<Circle> calcTriangleGridNeighbors() {
            List<Circle> n = new ArrayList<>(6);
            double rowHeight = r * 0.8660254037844386; // sqrt(3)/2
            double halfRadius = r / 2;
            n.add(new Circle(cx + r, cy, r)); // right
            n.add(new Circle(cx - r, cy, r)); // left
            n.add(new Circle(cx - halfRadius, cy - rowHeight, r)); // top left
            n.add(new Circle(cx + halfRadius, cy - rowHeight, r)); // top right
            n.add(new Circle(cx - halfRadius, cy + rowHeight, r)); // bottom left
            n.add(new Circle(cx + halfRadius, cy + rowHeight, r)); // bottom right
            return n;
        }

        private List<Circle> calcSquareGridNeighbors() {
            List<Circle> n = new ArrayList<>(6);
            double distance = r * SQRT_2;
            n.add(new Circle(cx - distance, cy, r)); // left
            n.add(new Circle(cx + distance, cy, r)); // right
            n.add(new Circle(cx, cy - distance, r)); // top
            n.add(new Circle(cx, cy + distance, r)); // bottom
            return n;
        }

        private List<Circle> calcSquare2GridNeighbors() {
            List<Circle> n = new ArrayList<>(6);
            double distance = r * SQRT_2;
            n.add(new Circle(cx - distance, cy - distance, r)); // top left
            n.add(new Circle(cx + distance, cy - distance, r)); // top right
            n.add(new Circle(cx - distance, cy + distance, r)); // bottom left
            n.add(new Circle(cx + distance, cy + distance, r)); // bottom right
            return n;
        }

        Shape toShape() {
            return Shapes.createCircle(cx, cy, r);
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

        return switch (gridType) {
            case GRID_TYPE_TRIANGULAR -> it * r;
            case GRID_TYPE_SQUARE -> (float) (r + (it - 1) * SQRT_2 * r);
            case GRID_TYPE_SQUARE_2 -> r + (it - 1) * 2 * r;
            default -> throw new IllegalStateException("gridType = " + gridType);
        };
    }
}
