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

package pixelitor.colors.palette;

/**
 * An abstract base class for palettes whose colors are dynamically generated
 * based on the available grid size in the {@link PalettePanel}.
 */
public abstract non-sealed class DynamicPalette extends Palette {
    protected int rowCount;
    protected int columnCount;

    protected DynamicPalette(int initialRowCount, int initialColumnCount) {
        setGridSize(initialRowCount, initialColumnCount);
    }

    /**
     * Sets the grid dimensions for which this palette should generate colors.
     */
    public void setGridSize(int rows, int columns) {
        this.rowCount = rows;
        this.columnCount = columns;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public int getRowCount() {
        return rowCount;
    }
}