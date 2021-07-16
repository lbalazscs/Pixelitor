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
    private static final int TYPE_HEXAGON = 1;
    private static final int TYPE_TRIANGLE = 2;
    private static final int TYPE_DIAMOND = 3;
    private static final int TYPE_FISH_SCALE = 4;
    private static final int TYPE_DRAGON_SCALE = 5;

    private final IntChoiceParam type = new IntChoiceParam("Type", new Item[]{
        new Item("Rectangles", TYPE_RECTANGLE),
        new Item("Hexagons", TYPE_HEXAGON),
        new Item("Triangles", TYPE_TRIANGLE),
        new Item("Diamonds", TYPE_DIAMOND),
        new Item("Fish Scales", TYPE_FISH_SCALE),
        new Item("Dragon Scales", TYPE_DRAGON_SCALE)
    });
    private final GroupedRangeParam divisions = new GroupedRangeParam(
        "Divisions", 1, 4, 49, false);

    public Grid() {
        addParamsToFront(type, divisions);
    }

    @Override
    protected Path2D createShape(int width, int height) {
        return switch (type.getValue()) {
            case TYPE_RECTANGLE -> createRectangularGrid(width, height);
            case TYPE_HEXAGON -> createHexagonalGrid(width, height);
            case TYPE_TRIANGLE -> createTriangularGrid(width, height, false);
            case TYPE_DIAMOND -> createTriangularGrid(width, height, true);
            case TYPE_FISH_SCALE -> createScalesGrid(width, height, false);
            case TYPE_DRAGON_SCALE -> createScalesGrid(width, height, true);
            default -> throw new IllegalStateException("Unexpected value: " + type.getValue());
        };
    }

    private Path2D createTriangularGrid(int width, int height, boolean diamond) {
        Path2D shape = new Path2D.Double();

        int horDiv = divisions.getValue(0);
        int verDiv = divisions.getValue(1);

        //                      /\
        // Width and height of /  \
        double cellW = width / (double) horDiv;
        double cellH = height / (double) verDiv;

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

        for (int i = -2 - horSeg; i < horDiv - horSeg + 1; i++) {
            for (int j = -2 * (verSeg + 1); j < verDiv - 2 * (verSeg - 2); j += 2) {
                baselessTriangle(shape, i * cellW, j * cellH, cellW, cellH);
                baselessTriangle(shape, i * cellW + cellInterval, (j + 1) * cellH, cellW, cellH);
            }
        }

        // _ lines
        if (!diamond) {
            verSeg *= 2;
            for (int i = -verSeg - 1; i < verDiv - verSeg + 2; i++) {
                line(shape, -horShift, i * cellH, width - horShift, i * cellH);
            }
        }

        return shape;
    }

    private static void baselessTriangle(Path2D shape, double x, double y, double width, double height) {
        shape.moveTo(x, y);
        shape.lineTo(x + width / 2, y - height);
        shape.lineTo(x + width, y);
    }

    private Path2D createRectangularGrid(int width, int height) {
        Path2D shape = new Path2D.Double();

        // Here one cell is defined as the smallest
        // square/rectangle you can see

        int horDiv = divisions.getValue(0);
        int verDiv = divisions.getValue(1);

        double cellW = width / (double) horDiv;
        double cellH = height / (double) verDiv;

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
        for (int i = -verSeg; i < verDiv - verSeg + 1; i++) {
            double lineY = i * cellH;
            line(shape, -horShift, lineY, width - horShift, lineY);
        }

        // vertical | lines
        for (int i = -horSeg; i < horDiv - horSeg + 1; i++) {
            double lineX = i * cellW;
            line(shape, lineX, -verShift, lineX, height - verShift);
        }

        return shape;
    }

    private static void line(Path2D shape, double x, double y, double x2, double y2) {
        shape.moveTo(x, y);
        shape.lineTo(x2, y2);
    }

    private Path2D createHexagonalGrid(int width, int height) {
        Path2D shape = new Path2D.Double();

        int horDiv = divisions.getValue(0);
        int verDiv = divisions.getValue(1);

        //                       ___
        // Width and Height of  /   \
        double cellW = 2.0 * width / (3 * horDiv - 1);
        double cellH = height / (double) verDiv;

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

        for (int i = -2 - horSeg; i < horDiv - horSeg + 1; i++) {
            for (int j = -2 * (verSeg + 1); j < verDiv - 2 * (verSeg - 2); j += 2) {
                hexagonTopHalf(shape, i * cellSpace, j * cellH, cellW, cellH);
                hexagonTopHalf(shape, i * cellSpace + cellInterval, (j + 1) * cellH, cellW, cellH);
            }
        }

        return shape;
    }

    private static void hexagonTopHalf(Path2D shape, double x, double y, double width, double height) {
        shape.moveTo(x, y);
        shape.lineTo(x + width / 4, y - height);
        shape.lineTo(x + 3 * width / 4, y - height);
        shape.lineTo(x + width, y);
    }

    private Path2D createScalesGrid(int width, int height, boolean dragon) {
        Path2D shape = new Path2D.Double();

        int horDiv = divisions.getValue(0);
        int verDiv = divisions.getValue(1);

        double cellW = width / (double) horDiv;
        double cellH = height / (double) verDiv;

        double cellInterval = cellW / 2;

        double horShift = (center.getRelativeX() - 0.5) * width;
        double verShift = (center.getRelativeY() - 0.5) * height;

        int horSeg = (int) (horShift / cellW);
        int verSeg = (int) (verShift / (2 * cellH));

        for (int i = -2 - horSeg; i < horDiv - horSeg + 1; i++) {
            for (int j = -2 * (verSeg + 1); j < verDiv - 2 * (verSeg - 2); j += 2) {
                scale(shape, i * cellW, j * cellH, cellW, cellH, dragon);
                scale(shape, i * cellW + cellInterval, (j + 1) * cellH, cellW, cellH, dragon);
            }
        }

        return shape;
    }

    private static void scale(Path2D shape, double x, double y, double w, double h, boolean dragon) {
        shape.moveTo(x, y - h);

        double cp1X, cp2X;
        if (dragon) {
            cp1X = x + w * 0.46;
            cp2X = x + w * 0.54;
        } else {
            cp1X = x + w / 4;
            cp2X = x + 3 * w / 4;
        }
        shape.curveTo(
            cp1X, y + h / 3,
            cp2X, y + h / 3,
            x + w, y - h
        );
    }
}