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

import java.awt.Color;
import java.util.List;

/**
 * A data model that provides a list of colors for a {@link PalettePanel}.
 * The palette can be either dynamic (colors are generated based on grid size)
 * or static (a fixed list of colors is provided).
 */
public abstract sealed class Palette permits DynamicPalette, StaticPalette {
    protected PaletteConfig config;

    /**
     * Provides the list of colors for the palette.
     * For dynamic palettes, this list is regenerated based on the current grid size.
     * For static palettes, this returns a fixed list.
     */
    public abstract List<Color> getColors();

    /**
     * A hook for subclasses to react to configuration changes.
     */
    public void onConfigChanged() {
    }

    public PaletteConfig getConfig() {
        return config;
    }

    public abstract String getDialogTitle();

    public String getHelpText() {
        String resizeHelp = switch (this) {
            case DynamicPalette d -> "enlarge for more colors";
            case StaticPalette s -> "resize the dialog to reflow the colors";
        };

        return getDialogTitle() + ": " + resizeHelp + ", "
            + ColorSwatchClickHandler.STANDARD_HTML_HELP;
    }
}
