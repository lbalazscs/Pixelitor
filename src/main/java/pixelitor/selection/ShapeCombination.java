/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
package pixelitor.selection;

import java.awt.Shape;
import java.awt.geom.Area;

/**
 * Describes how a new selection is combined with an existing selection.
 * Corresponds to the "New Selection" combo box in the Selection Tool.
 */
public enum ShapeCombination {
    REPLACE("Replace") {
        @Override
        public Shape combine(Shape oldShape, Shape newShape) {
            return newShape;
        }
    }, ADD("Add") {
        @Override
        public Shape combine(Shape oldShape, Shape newShape) {
            Area oldArea = new Area(oldShape);
            Area newArea = new Area(newShape);
            oldArea.add(newArea);
            return oldArea;
        }
    }, SUBTRACT("Subtract") {
        @Override
        public Shape combine(Shape oldShape, Shape newShape) {
            Area oldArea = new Area(oldShape);
            Area newArea = new Area(newShape);
            oldArea.subtract(newArea);
            return oldArea;
        }
    }, INTERSECT("Intersect") {
        @Override
        public Shape combine(Shape oldShape, Shape newShape) {
            Area oldArea = new Area(oldShape);
            Area newArea = new Area(newShape);
            oldArea.intersect(newArea);
            return oldArea;
        }
    };

    private final String guiName;

    ShapeCombination(String guiName) {
        this.guiName = guiName;
    }

    /**
     * Calculates the combined shape from the existing shape and the new one
     */
    public abstract Shape combine(Shape oldShape, Shape newShape);

    @Override
    public String toString() {
        return guiName;
    }

    public String getNameForUndo() {
        return toString() + " Selection";
    }
}
