/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An abstract base class for palettes whose colors are dynamically generated
 * based on the available grid size in the {@link PalettePanel}.
 */
public abstract non-sealed class DynamicPalette extends Palette {
    // remembers the last grid size per palette class so that a newly
    // opened dialog gets the size the user last gave the previous one
    private static final Map<Class<? extends DynamicPalette>, GridSize> lastSizes = new HashMap<>();

    private record GridSize(int rows, int cols) {
    }

    protected int numRows;
    protected int numCols;

    protected DynamicPalette(String dialogTitle, PaletteConfig config, int defaultRows, int defaultCols) {
        super(dialogTitle, config);
        GridSize size = lastSizes.getOrDefault(getClass(), new GridSize(defaultRows, defaultCols));
        this.numRows = size.rows();
        this.numCols = size.cols();
    }

    @Override
    public final List<Color> getColors() {
        List<Color> colors = new ArrayList<>(numRows * numCols);
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                colors.add(createColor(row, col));
            }
        }
        return colors;
    }

    /**
     * Creates the color of the swatch at the given grid position.
     */
    protected abstract Color createColor(int row, int col);

    /**
     * Sets the grid dimensions for which this palette should generate colors.
     */
    public final void setGridSize(int rows, int columns) {
        assert rows >= 1 && columns >= 1;
        this.numRows = rows;
        this.numCols = columns;
        lastSizes.put(getClass(), new GridSize(rows, columns));
    }

    /**
     * Brightness rising from 1/numCols (first column) to 1.0 (last column).
     */
    protected float calcColumnBrightness(int col) {
        return (col + 1) / (float) numCols;
    }

    /**
     * The interpolation factor of a column: 0 for the first, 1 for the last.
     */
    protected float calcColumnMixFactor(int col) {
        return numCols == 1 ? 0.0f : col / (float) (numCols - 1);
    }

    /**
     * The vertical position of a row: -1 for the first, 1 for the last, 0 for a single row.
     */
    protected float calcRowOffset(int row) {
        return numRows == 1 ? 0.0f : 2.0f * row / (numRows - 1) - 1.0f;
    }

    public int getNumRows() {
        return numRows;
    }

    public int getNumCols() {
        return numCols;
    }

    @Override
    protected String getResizeHelp() {
        return "enlarge the dialog for more colors";
    }
}
