/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gui;

import pixelitor.filters.Filter;
import pixelitor.layers.Drawable;

/**
 * A filter that has a GUI for customization
 */
public abstract class FilterWithGUI extends Filter {
    protected FilterWithGUI() {
    }

    /**
     * Creates a new {@link FilterGUI} for this GUI filter.
     * The panel must be created at the moment of this call (cannot be cached)
     * Creating a {@link FilterGUI} should also automatically run the first
     * preview run of this filter based on the default settings
     */
    public abstract FilterGUI createGUI(Drawable dr);

    public abstract void randomizeSettings();

    @Override
    public void startOn(Drawable dr) {
        dr.startPreviewing();

        FilterGUI gui = createGUI(dr);
        FilterDialog dialog = new FilterDialog(gui, this, dr);
        dialog.setVisible(true);
    }
}
