/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.Help;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;
import pixelitor.utils.CustomShapes;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jhlabs.image.ImageMath.HALF_SQRT_3;
import static com.jhlabs.image.ImageMath.SQRT_2;

/**
 * "Flower of Life" filter.
 */
public class FlowerOfLife extends CurveFilter {
    @Serial
    private static final long serialVersionUID = 7213113245913645714L;

    public static final String NAME = "Flower of Life";

    private static final int GRID_TYPE_TRIANGULAR = 1;
    private static final int GRID_TYPE_SQUARE = 2;
    private static final int GRID_TYPE_SQUARE_2 = 3;

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

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Overlapping_circles_grid");
    }

    @Override
    protected Path2D createCurve(int width, int height) {
        Path2D shape = new Path2D.Double();

        double r = radius.getValueAsDouble();
        double cx = transform.getCx(width);
        double cy = transform.getCy(height);

        Circle firstCircle = new Circle(cx, cy, r);
        Set<Circle> circlesSet = new HashSet<>();
        circlesSet.add(firstCircle);

        int gridType = grid.getValue();
        int numIterations = iterations.getValue();

        List<Circle> newlyAdded = List.of(firstCircle);
        for (int it = 2; it <= numIterations; it++) {
            List<Circle> nextNew = new ArrayList<>();
            // only computes neighbors for the circles that were
            // newly added during the preceding iteration
            for (Circle circle : newlyAdded) {
                for (Circle neighbor : circle.calcNeighbors(gridType)) {
                    if (circlesSet.add(neighbor)) {
                        nextNew.add(neighbor);
                    }
                }
            }
            newlyAdded = nextNew;
        }

        boolean manyPoints = transform.hasNonlinDistort();
        for (Circle circle : circlesSet) {
            shape.append(circle.toShape(manyPoints), false);
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
            double rowHeight = r * HALF_SQRT_3;
            double halfRadius = r / 2;
            return List.of(
                new Circle(cx + r, cy, r), // right
                new Circle(cx - r, cy, r), // left
                new Circle(cx - halfRadius, cy - rowHeight, r), // top left
                new Circle(cx + halfRadius, cy - rowHeight, r), // top right
                new Circle(cx - halfRadius, cy + rowHeight, r), // bottom left
                new Circle(cx + halfRadius, cy + rowHeight, r) // bottom right
            );
        }

        private List<Circle> calcSquareGridNeighbors() {
            double distance = r * SQRT_2;
            return List.of(
                new Circle(cx - distance, cy, r), // left
                new Circle(cx + distance, cy, r), // right
                new Circle(cx, cy - distance, r), // top
                new Circle(cx, cy + distance, r) // bottom
            );
        }

        private List<Circle> calcSquare2GridNeighbors() {
            double distance = r * SQRT_2;
            return List.of(
                new Circle(cx - distance, cy - distance, r), // top left
                new Circle(cx + distance, cy - distance, r), // top right
                new Circle(cx - distance, cy + distance, r), // bottom left
                new Circle(cx + distance, cy + distance, r) // bottom right
            );
        }

        Shape toShape(boolean manyPoints) {
            return manyPoints
                ? CustomShapes.createCircle(cx, cy, r, 24)
                : CustomShapes.createCircle(cx, cy, r);
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
