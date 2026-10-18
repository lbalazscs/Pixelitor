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
import java.util.List;
import java.util.Objects;

/**
 * A data model that provides a list of colors for a {@link PalettePanel}.
 * The palette can be either dynamic (colors are generated based on grid size)
 * or static (a fixed list of colors is provided).
 */
public abstract sealed class Palette permits DynamicPalette, StaticPalette {
    private final String dialogTitle;
    private final PaletteConfig config;

    protected Palette(String dialogTitle, PaletteConfig config) {
        this.dialogTitle = Objects.requireNonNull(dialogTitle);
        this.config = Objects.requireNonNull(config);
    }

    /**
     * Provides the list of colors for the palette.
     * For dynamic palettes, this list is regenerated based on the current grid size.
     * For static palettes, this returns a fixed list.
     */
    public abstract List<Color> getColors();

    public PaletteConfig getConfig() {
        return config;
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public String getHelpText(ColorSwatchClickHandler clickHandler) {
        return dialogTitle + ": " + getResizeHelp() + ", " + clickHandler.getHelpText();
    }

    protected abstract String getResizeHelp();
}
