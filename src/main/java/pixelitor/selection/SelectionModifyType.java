/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.EnumParam;
import pixelitor.gui.GUIText;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Area;

/**
 * The modification type for a selection, used in the "Modify Selection" dialog.
 */
public enum SelectionModifyType {
    EXPAND("Expand") {
        @Override
        protected Shape modifyArea(Area previousSelection, Area borderArea) {
            previousSelection.add(borderArea);
            return previousSelection;
        }
    }, CONTRACT("Contract") {
        @Override
        protected Shape modifyArea(Area previousSelection, Area borderArea) {
            previousSelection.subtract(borderArea);
            return previousSelection;
        }
    }, BORDER("Border") {
        @Override
        protected Shape modifyArea(Area previousSelection, Area borderArea) {
            return borderArea;
        }
    }, OUTWARD_BORDER("Border Outwards Only") {
        @Override
        protected Shape modifyArea(Area previousSelection, Area borderArea) {
            borderArea.subtract(previousSelection);
            return borderArea;
        }
    }, INWARD_BORDER("Border Inwards Only") {
        @Override
        protected Shape modifyArea(Area previousSelection, Area borderArea) {
            previousSelection.intersect(borderArea);
            return previousSelection;
        }
    };

    private final String displayName;

    SelectionModifyType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Modifies a shape based on this modification type.
     */
    public Shape modifyShape(Shape currentShape, float amount) {
        BasicStroke borderStroke = new BasicStroke(amount);
        Shape borderShape = borderStroke.createStrokedShape(currentShape);
        Area currentArea = new Area(currentShape);
        Area borderArea = new Area(borderShape);
        return modifyArea(currentArea, borderArea);
    }

    protected abstract Shape modifyArea(Area previousSelection, Area borderArea);

    public static EnumParam<SelectionModifyType> asParam() {
        return new EnumParam<>(GUIText.TYPE, SelectionModifyType.class);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
