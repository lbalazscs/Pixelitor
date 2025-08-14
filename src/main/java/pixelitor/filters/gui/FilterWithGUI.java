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

package pixelitor.filters.gui;

import pixelitor.filters.Filter;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.io.Serial;

/**
 * An abstract base class for filters that have a customizable GUI panel.
 */
public abstract class FilterWithGUI extends Filter implements DialogMenuOwner {
    @Serial
    private static final long serialVersionUID = -7575676579160980928L;

    protected transient Help help;

    protected FilterWithGUI() {
    }

    /**
     * Creates a new {@link FilterGUI} panel for this GUI filter.
     * Panel creation should trigger the initial preview based on default settings.
     * @param layer the layer to which this filter is applied
     * @param reset if true, resets the GUI to default settings
     * @return a newly created FilterGUI instance configured with this filter
     */
    public abstract FilterGUI createGUI(Filterable layer, boolean reset);

    /**
     * Randomizes the filter's parameters.
     */
    public abstract void randomize();

    /**
     * Returns a menu bar if help or presets are available, or null otherwise.
     */
    public JMenuBar getMenuBar() {
        boolean addPresets = shouldHaveUserPresetsMenu() || hasBuiltinPresets();
        if (!addPresets && !hasHelp()) {
            return null;
        }

        return new DialogMenuBar(this, addPresets);
    }

    @Override
    public boolean supportsUserPresets() {
        // most filters with a GUI are expected to support user presets
        return true;
    }

    @Override
    public String getPresetDirName() {
        // uses the filter's name as the directory name for storing presets
        return getName();
    }

    @Override
    public boolean hasHelp() {
        return help != null;
    }

    @Override
    public Help getHelp() {
        return help;
    }
}
