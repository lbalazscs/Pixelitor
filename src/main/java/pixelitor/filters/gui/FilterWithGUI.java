/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.io.Serial;

/**
 * A filter that has a GUI for customization.
 *
 * Note that filters without a GUI can still use dialogs if
 * they are started from places like "Random Filter".
 */
public abstract class FilterWithGUI extends Filter implements DialogMenuOwner {
    @Serial
    private static final long serialVersionUID = -7575676579160980928L;

    protected transient String helpURL;

    protected FilterWithGUI() {
    }

    /**
     * Creates a new {@link FilterGUI} for this GUI filter.
     * The panel must be created at the moment of this call (can't be cached).
     * Creating a {@link FilterGUI} should also automatically calculate
     * the first preview of this filter based on the default settings.
     * If the reset argument is false, then the GUI must be initialized
     * with the settings stored in this filter.
     */
    public abstract FilterGUI createGUI(Filterable layer, boolean reset);

    public abstract void randomize();

    public JMenuBar getMenuBar() {
        boolean addPresets = canHaveUserPresets() || hasBuiltinPresets();
        if (!hasHelp() && !addPresets) {
            return null;
        }

        return new DialogMenuBar(this, addPresets);
    }

    @Override
    public boolean canHaveUserPresets() {
        return true;
    }

    @Override
    public String getPresetDirName() {
        return getName();
    }

    @Override
    public boolean hasHelp() {
        return helpURL != null;
    }

    @Override
    public String getHelpURL() {
        return helpURL;
    }
}
