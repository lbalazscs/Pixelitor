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

package pixelitor.selection;

import java.awt.Shape;
import java.awt.geom.Area;

public enum SelectionModifyType {
    EXPAND("Expand") {
        @Override
        public Shape createModifiedShape(Area previous, Area outlineShape) {
            previous.add(outlineShape);
            return previous;
        }
    }, CONTRACT("Contract") {
        @Override
        public Shape createModifiedShape(Area previous, Area outlineShape) {
            previous.subtract(outlineShape);
            return previous;
        }
    }, BORDER("Border") {
        @Override
        public Shape createModifiedShape(Area previous, Area outlineShape) {
            return outlineShape;
        }
    }, BORDER_OUT("Border Outwards Only") {
        @Override
        public Shape createModifiedShape(Area previous, Area outlineShape) {
            outlineShape.subtract(previous);
            return outlineShape;
        }
    }, BORDER_IN("Border Inwards Only") {
        @Override
        public Shape createModifiedShape(Area previous, Area outlineShape) {
            previous.intersect(outlineShape);
            return previous;
        }
    };

    private final String guiName;

    SelectionModifyType(String guiName) {
        this.guiName = guiName;
    }

    public abstract Shape createModifiedShape(Area previous, Area outlineShape);

    @Override
    public String toString() {
        return guiName;
    }
}
