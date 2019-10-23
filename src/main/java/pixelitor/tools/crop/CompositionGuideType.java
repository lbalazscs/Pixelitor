/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.crop;

/**
 * Crop composition guide types
 */
public enum CompositionGuideType {
    NONE("None"),
    RULE_OF_THIRDS("Rule of Thirds"),
    GOLDEN_SECTIONS("Golden Sections"),
    GOLDEN_SPIRAL("Golden Spiral"),
    DIAGONALS("Diagonal Lines"),
    TRIANGLES("Triangles"),
    GRID("Grid");

    private final String guiName;

    CompositionGuideType(String guiName) {
        this.guiName = guiName;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
