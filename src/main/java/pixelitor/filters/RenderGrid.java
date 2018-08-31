/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Shape;
import java.awt.geom.Path2D;

/**
 * The "Render/Shapes/Grid" filter
 */
public class RenderGrid extends ShapeFilter {
    private final GroupedRangeParam divisions = new GroupedRangeParam(
            "Divisions", 1, 4, 50, false);

    public RenderGrid() {
        addParamsToFront(divisions);
    }

    @Override
    protected Shape createShape(int width, int height) {
        Path2D shape = new Path2D.Double();

        double horShift = (center.getRelativeX() - 0.5) * width;
        double verShift = (center.getRelativeY() - 0.5) * height;

        // horizontal lines
        int numHorDivisions = divisions.getValue(0);
        double divisionHeight = height / (double) numHorDivisions;
        for (int i = 1; i < numHorDivisions; i++) {
            double lineY = i * divisionHeight + verShift;
            shape.moveTo(horShift, lineY);
            shape.lineTo(width + horShift, lineY);
        }

        // vertical lines
        int numVerDivisions = divisions.getValue(1);
        double divisionWidth = width / (double) numVerDivisions;
        for (int i = 1; i < numVerDivisions; i++) {
            double lineX = i * divisionWidth + horShift;
            shape.moveTo(lineX, verShift);
            shape.lineTo(lineX, height + verShift);
        }

        return shape;
    }
}