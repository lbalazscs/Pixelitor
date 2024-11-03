/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
 * Defines the available operations for combining
 * a new selection shape with an existing one.
 */
public enum ShapeCombinator {
    REPLACE("Replace") {
        @Override
        public Shape combine(Shape existingShape, Shape newShape) {
            // discard the previously selected area
            return newShape;
        }
    }, ADD("Add") {
        @Override
        public Shape combine(Shape existingShape, Shape newShape) {
            // adds the new selection area to the existing one
            Area combinedArea = new Area(existingShape);
            combinedArea.add(new Area(newShape));
            return combinedArea;
        }
    }, SUBTRACT("Subtract") {
        @Override
        public Shape combine(Shape existingShape, Shape newShape) {
            // removes the new selection area from the existing one
            Area remainingArea = new Area(existingShape);
            remainingArea.subtract(new Area(newShape));
            return remainingArea;
        }
    }, INTERSECT("Intersect") {
        @Override
        public Shape combine(Shape existingShape, Shape newShape) {
            // keeps only the areas that are common to both selections
            Area commonArea = new Area(existingShape);
            commonArea.intersect(new Area(newShape));
            return commonArea;
        }
    };

    private final String displayName;

    ShapeCombinator(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Combines two shapes according to this combination mode.
     */
    public abstract Shape combine(Shape existingShape, Shape newShape);

    @Override
    public String toString() {
        return displayName;
    }

    public String getHistoryName() {
        return this + " Selection";
    }
}
