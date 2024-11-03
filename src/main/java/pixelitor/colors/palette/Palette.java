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

package pixelitor.colors.palette;

/**
 * Base class for color palette generators.
 * A {@link Palette} is the data model for a {@link PalettePanel}.
 */
public abstract class Palette {
    protected int rowCount;
    protected int columnCount;
    protected PaletteConfig config;

    protected Palette(int rowCount, int columnCount) {
        setGridSize(rowCount, columnCount);
    }

    /**
     * Populates the given panel with color swatches arranged in a grid.
     */
    public abstract void addButtons(PalettePanel panel);

    public void onConfigChanged() {
    }

    public PaletteConfig getConfig() {
        return config;
    }

    public void setGridSize(int rows, int columns) {
        this.columnCount = columns;
        this.rowCount = rows;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public abstract String getDialogTitle();

    public String getHelpText() {
        return getDialogTitle() + ": enlarge for more colors, "
            + ColorSwatchClickHandler.STANDARD_HTML_HELP;
    }
}

