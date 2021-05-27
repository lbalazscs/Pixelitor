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

import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;

import java.awt.Shape;
import java.awt.geom.Path2D;

/**
 * The "Render/Shapes/Grid" filter
 */
public class Grid extends ShapeFilter {
    private static final int TYPE_RECTANGLE = 0;
    private static final int TYPE_TRIANGLE = 1;
    private static final int TYPE_HEXAGON = 2;

    private final IntChoiceParam type = new IntChoiceParam("Type", new Item[]{
            new Item("Rectangle", TYPE_RECTANGLE),
            new Item("Triangle", TYPE_TRIANGLE),
            new Item("Hexagon", TYPE_HEXAGON)
    });
    private final GroupedRangeParam divisions = new GroupedRangeParam(
            "Divisions", 1, 4, 49, false);

    public Grid() {
        addParamsToFront(type, divisions);
    }

    @Override
    protected Shape createShape(int width, int height) {
        return switch (type.getValue()) {
            case TYPE_RECTANGLE -> createRectangularGrid(width, height);
            case TYPE_HEXAGON -> createHexagonalGrid(width, height);
            case TYPE_TRIANGLE -> createTriangularGrid(width, height);
            default -> throw new IllegalStateException("Unexpected value: " + type.getValue());
        };
    }

    private Shape createTriangularGrid(int width, int height) {
        Path2D shape = new Path2D.Double();

        int horDiv = divisions.getValue(0);
        int verDiv = divisions.getValue(1);

        //                      /\
        // Width and height of /  \
        double cellW = width / (double) verDiv;
        double cellH = height / (double) horDiv;

        // Adding the line below just to relate stuff with Hexagonal Grid.
        // Though i dont expect anyone to understand most of it...
        //                          /\  /\  /\
        // Cell space is Width of  /  \/  \/  \
        //                        ^   ^
        // double cellSpace = cellW;

        // Cell interval is the horizontal length after which another cell starts above it.
        //     /\
        //    /  \
        //   /\
        //  /  \
        // ^ ^
        double cellInterval = cellW / 2;

        double horShift = (center.getRelativeX() - 0.5) * width;
        double verShift = (center.getRelativeY() - 0.5) * height;

        int horSeg = (int) (horShift / cellW);
        int verSeg = (int) (verShift / (2 * cellH));

        for (int i = -2 - horSeg; i < verDiv - horSeg + 1; i++) {
            for (int j = -2 * (verSeg + 1); j < horDiv - 2 * (verSeg - 2); j += 2) {
                BaselessTriangle(shape, i * cellW, j * cellH, cellW, cellH);
                BaselessTriangle(shape, i * cellW + cellInterval, (j + 1) * cellH, cellW, cellH);
            }
        }

        verSeg *= 2;

        // _ lines
        for (int i = -verSeg - 1; i < horDiv - verSeg + 2; i++) {
            line(shape, -horShift, i * cellH, width - horShift, i * cellH);
        }

        return shape;
    }

    private void BaselessTriangle(Path2D shape, double x, double y, double width, double height) {
        shape.moveTo(x, y);
        shape.lineTo(x + width / 2, y - height);
        shape.lineTo(x + width, y);
    }

    private Shape createRectangularGrid(int width, int height) {
        Path2D shape = new Path2D.Double();

        // Here one cell is defined as the smallest
        // square/rectangle you can see

        int horDiv = divisions.getValue(0);
        int verDiv = divisions.getValue(1);

        double cellW = width / (double) verDiv;
        double cellH = height / (double) horDiv;

        // Number of extra segments (lines) to be drawn
        // in order to cope up with transform shifts.
        // In extrema case, we draw just enough segments
        // to fill up 50% of view [height/width] from one side
        // while not drawing those from the other side as
        // they'll placed outside.
        double horShift = (center.getRelativeX() - 0.5) * width;
        double verShift = (center.getRelativeY() - 0.5) * height;
        int horSeg = (int) (horShift / cellW);
        int verSeg = (int) (verShift / cellH);

        // horizontal _ lines
        for (int i = -verSeg; i < horDiv - verSeg + 1; i++) {
            double lineY = i * cellH;
            line(shape, -horShift, lineY, width - horShift, lineY);
        }

        // vertical | lines
        for (int i = -horSeg; i < verDiv - horSeg + 1; i++) {
            double lineX = i * cellW;
            line(shape, lineX, -verShift, lineX, height - verShift);
        }

        return shape;
    }

    private static void line(Path2D shape, double x, double y, double x2, double y2) {
        shape.moveTo(x, y);
        shape.lineTo(x2, y2);
    }

    private Shape createHexagonalGrid(int width, int height) {
        Path2D shape = new Path2D.Double();

        int horDiv = divisions.getValue(0);
        int verDiv = divisions.getValue(1);

        //                       ___
        // Width and Height of  /   \
        double cellW = 2.0 * width / (3 * verDiv - 1);
        double cellH = height / (double) horDiv;

        //                          ___     ___     ___
        // Cell space is Width of  /   \___/   \___/   \___
        //                        ^       ^
        double cellSpace = 3 * cellW / 2;
        // Cell interval is the horizontal length after which another cell starts above it.
        //     ___
        //   _/_  \___
        //  /   \___
        // ^  ^
        double cellInterval = cellSpace / 2; // 3/4th of the cellW

        double horShift = (center.getRelativeX() - 0.5) * width;
        double verShift = (center.getRelativeY() - 0.5) * height;
        int horSeg = (int) (horShift / cellW);
        int verSeg = (int) (verShift / (2 * cellH));

        for (int i = -2 - horSeg; i < verDiv - horSeg + 1; i++) {
            for (int j = -2 * (verSeg + 1); j < horDiv - 2 * (verSeg - 2); j += 2) {
                hexagonTopHalf(shape, i * cellSpace,j * cellH, cellW, cellH);
                hexagonTopHalf(shape, i * cellSpace + cellInterval,  (j + 1) * cellH, cellW, cellH);
            }
        }

        return shape;
    }

    private void hexagonTopHalf(Path2D shape, double x, double y, double width, double height) {
        shape.moveTo(x, y);
        shape.lineTo(x + width / 4, y - height);
        shape.lineTo(x + 3 * width / 4, y - height);
        shape.lineTo(x + width, y);
    }
}