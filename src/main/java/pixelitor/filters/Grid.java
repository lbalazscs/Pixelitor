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
        double divWid = width / (double) horDiv;
        double divHei = height / (double) verDiv / 2; // Height of one triangle

        // / lines
        for (int i = 1 - verDiv; i < horDiv; i++) {
            line(shape, i * divWid, height, (i + verDiv) * divWid, 0);
        }

        // \ lines
        for (int i = 1 - verDiv; i < horDiv; i++) {
            line(shape, i * divWid, 0, (i + verDiv) * divWid, height);
        }

        // Earlier it was the number of Vertical Divisions,
        // Now it's the number of triangles.
        verDiv *= 2;

        // _ lines
        for (int i = 1; i < verDiv; i++) {
            line(shape, 0, i * divHei, width, i * divHei);
        }

        return shape;
    }

    private Shape createRectangularGrid(int width, int height) {
        Path2D shape = new Path2D.Double();

        int horiDiv = divisions.getValue(0);
        int vertDiv = divisions.getValue(1);

        double cellW = width / (double) vertDiv;
        double cellH = height / (double) horiDiv;

        // Number of extra segments (lines) to be drawn
        // in order to cope up with transform shifts.
        // In extrema case, we draw just enough segments
        // to fill up 50% of view [height/width] from one side
        // while not drawing those from the other side as
        // they'll placed outside.:
        double horShift = (center.getRelativeX() - 0.5) * width;
        double verShift = (center.getRelativeY() - 0.5) * height;
        int horSeg = (int) (horShift / cellW);
        int verSeg = (int) (verShift / cellH);

        // horizontal _ lines
        for (int i = -verSeg; i < horiDiv - verSeg + 1; i++) {
            double lineY = i * cellH;
            line(shape, -horShift, lineY, width - horShift, lineY);
        }

        // vertical | lines
        for (int i = -horSeg; i < vertDiv - horSeg + 1; i++) {
            double lineX = i * cellW;
            line(shape, lineX, -verShift, lineX, height-verShift);
        }

        return shape;
    }

    private static void line(Path2D shape, double x, double y, double x2, double y2) {
        shape.moveTo(x, y);
        shape.lineTo(x2, y2);
    }

    private Shape createHexagonalGrid(int width, int height) {
        Path2D shape = new Path2D.Double();

        int slabs = divisions.getValue(0);
        int levels = divisions.getValue(1);

        double slabWidth = 2.0 * width / (3 * slabs - 1);
        double slabHeight = height / (double) levels;

        double slabSpace = 3 * slabWidth / 2;
        double slabInterval = slabSpace / 2; // 3/4th of the slabWidth

        for (int i = -1; i < slabs; i++) {
            for (int j = -1; j < levels; j += 2) {
                slab(shape, i * slabSpace, height - j * slabHeight, slabWidth, slabHeight);
                slab(shape, i * slabSpace + slabInterval, height - (j + 1) * slabHeight, slabWidth, slabHeight);
            }
        }

        return shape;
    }

    private void slab(Path2D shape, double x, double y, double width, double height) {
        shape.moveTo(x, y);
        shape.lineTo(x + width / 4, y - height);
        shape.lineTo(x + 3 * width / 4, y - height);
        shape.lineTo(x + width, y);
    }
}