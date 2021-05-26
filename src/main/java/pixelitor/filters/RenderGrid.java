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

import net.jafama.FastMath;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;

import java.awt.Shape;
import java.awt.geom.Path2D;

/**
 * The "Render/Shapes/Grid" filter
 */
public class RenderGrid extends ShapeFilter {

    private final IntChoiceParam type = new IntChoiceParam("Type", new IntChoiceParam.Item[]{
            new IntChoiceParam.Item("Triangular", 0),
            new IntChoiceParam.Item("Square", 1),
            new IntChoiceParam.Item("Hexagon", 2)
    });
    private final GroupedRangeParam divisions = new GroupedRangeParam(
            "Divisions", 1, 4, 49, false);

    public RenderGrid() {
        addParamsToFront(type, divisions);
    }

    @Override
    protected Shape createShape(int width, int height) {
        return switch (type.getValue()) {
            case 1 -> createRectangularGrid(width, height);
            case 2 -> createHexagonalGrid(width, height);
            default -> createTriangularGrid(width, height);
        };
    }

    private Shape createTriangularGrid(int width, int height) {
        Path2D shape = new Path2D.Double();

        double horShift = (center.getRelativeX() - 0.5) * width;
        double verShift = (center.getRelativeY() - 0.5) * height;

        int horDiv = divisions.getValue(0);
        int verDiv = divisions.getValue(1);
        double divWid = width / (double) horDiv;
        double divHei = height / (double) verDiv / 2; // Height of one triangle

        // / lines
        for (int i = 1 - verDiv; i < horDiv; i++)
            line(shape, i * divWid + horShift, height + verShift, (i + verDiv) * divWid, 0);

        // \ lines
        for (int i = 1 - verDiv; i < horDiv; i++)
            line(shape, i * divWid + horShift, verShift, (i + verDiv) * divWid, height);

        verDiv *= 2; // Earlier it was the number of Vertical Divisions, Now it's the number of triangles.

        // _ lines
        for (int i = 1; i < verDiv; i++)
            line(shape, horShift, i * divHei + verShift, width, i * divHei);


        return shape;
    }

    private Shape createRectangularGrid(int width, int height) {
        Path2D shape = new Path2D.Double();

        double horShift = (center.getRelativeX() - 0.5) * width;
        double verShift = (center.getRelativeY() - 0.5) * height;

        // horizontal lines
        int numHorDivisions = divisions.getValue(0);
        double divisionHeight = height / (double) numHorDivisions;
        for (int i = 1; i < numHorDivisions; i++) {
            double lineY = i * divisionHeight;
            line(shape, horShift, lineY + verShift, width, lineY);
        }

        // vertical lines
        int numVerDivisions = divisions.getValue(1);
        double divisionWidth = width / (double) numVerDivisions;
        for (int i = 1; i < numVerDivisions; i++) {
            double lineX = i * divisionWidth;
            line(shape, lineX + horShift, verShift, lineX, height);
        }

        return shape;
    }

    private void line(Path2D shape, double x, double y, double x2, double y2) {
        shape.moveTo(x, y);
        shape.lineTo(x2, y2);
    }

    private Shape createHexagonalGrid(int width, int height) {
        Path2D shape = new Path2D.Double();

        double horShift = (center.getRelativeX() - 0.5) * width;
        double verShift = (center.getRelativeY() - 0.5) * height;

        int slabs = divisions.getValue(0);
        int levels = divisions.getValue(1);

        double slabWidth = 2. * width / (3 * slabs - 1);
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