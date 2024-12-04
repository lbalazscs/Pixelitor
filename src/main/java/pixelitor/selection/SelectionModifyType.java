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

import pixelitor.filters.gui.EnumParam;
import pixelitor.gui.GUIText;

import java.awt.Shape;
import java.awt.geom.Area;

/**
 * The modification type for a selection, used in the "Modify Selection" dialog.
 */
public enum SelectionModifyType {
    EXPAND("Expand") {
        @Override
        public Shape modify(Area previousSelection, Area borderArea) {
            previousSelection.add(borderArea);
            return previousSelection;
        }
    }, CONTRACT("Contract") {
        @Override
        public Shape modify(Area previousSelection, Area borderArea) {
            previousSelection.subtract(borderArea);
            return previousSelection;
        }
    }, BORDER("Border") {
        @Override
        public Shape modify(Area previousSelection, Area borderArea) {
            return borderArea;
        }
    }, OUTWARD_BORDER("Border Outwards Only") {
        @Override
        public Shape modify(Area previousSelection, Area borderArea) {
            borderArea.subtract(previousSelection);
            return borderArea;
        }
    }, INWARD_BORDER("Border Inwards Only") {
        @Override
        public Shape modify(Area previousSelection, Area borderArea) {
            previousSelection.intersect(borderArea);
            return previousSelection;
        }
    };

    private final String displayName;

    SelectionModifyType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Modifies a selection shape based on this modification type.
     */
    public abstract Shape modify(Area previousSelection, Area borderArea);

    public static EnumParam<SelectionModifyType> asParam() {
        return new EnumParam<>(GUIText.TYPE, SelectionModifyType.class);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
