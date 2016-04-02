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

package pixelitor.colors.palette;

/**
 * Generates colors for a 2D palette
 */
public abstract class Palette {
    protected int numRows;
    protected int numCols;
    protected PaletteConfig config;

    protected Palette(int numRows, int numCols) {
        setSize(numRows, numCols);
    }

    public abstract void addButtons(VariationsPanel panel);

    public void onConfigChange() {
    }

    public PaletteConfig getConfig() {
        return config;
    }

    public void setSize(int numRows, int numCols) {
        this.numCols = numCols;
        this.numRows = numRows;
    }

    public int getNumCols() {
        return numCols;
    }

    public int getNumRows() {
        return numRows;
    }

    public abstract String getDialogTitle();

    public String getStatusHelp() {
        return getDialogTitle() + ": enlarge for more colors, "
                + "click to set the foreground color, "
                + "right-click to set the background color, "
                + "Ctrl-click to clear the marking";
    }
}

