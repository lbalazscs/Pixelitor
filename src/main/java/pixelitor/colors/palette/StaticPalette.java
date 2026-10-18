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

/**
 * A palette that displays a fixed list of colors.
 * Resizing the panel will reflow the swatches into a new grid
 * but will not change the number or value of the colors.
 */
public final class StaticPalette extends Palette {
    private final List<Color> colors;

    public StaticPalette(String dialogTitle, List<Color> colors) {
        super(dialogTitle, PaletteConfig.NONE);
        this.colors = colors;
    }

    @Override
    public List<Color> getColors() {
        return colors;
    }

    @Override
    protected String getResizeHelp() {
        return "resize the dialog to reflow the colors";
    }
}
