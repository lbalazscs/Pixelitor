/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.layers.Drawable;
import pixelitor.tools.Tools;

import javax.swing.*;

import static pixelitor.gui.utils.Screens.Align.FRAME_RIGHT;

/**
 * A filter that has a GUI for customization
 */
public abstract class FilterWithGUI extends Filter implements DialogMenuOwner {
    protected String helpURL;

    protected FilterWithGUI() {
    }

    /**
     * Creates a new {@link FilterGUI} for this GUI filter.
     * The panel must be created at the moment of this call (can't be cached).
     * Creating a {@link FilterGUI} should also automatically calculate
     * the first preview of this filter based on the default settings.
     */
    public abstract FilterGUI createGUI(Drawable dr);

    public abstract void randomizeSettings();

    private JMenuBar getMenuBar() {
        boolean addPresets = canHaveUserPresets() || hasBuiltinPresets();
        if (!hasHelp() && !addPresets) {
            return null;
        }

        return new DialogMenuBar(this, addPresets);
    }

    @Override
    public boolean hasBuiltinPresets() {
        return false;
    }

    @Override
    public FilterState[] getBuiltinPresets() {
        // the subclasses implement this if they have built-in presets
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canHaveUserPresets() {
        return false;
    }

    @Override
    public UserPreset createUserPreset(String presetName) {
        // the subclasses override this if they can have user presets
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        // the subclasses override this if they can have user presets
        throw new UnsupportedOperationException();
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

    @Override
    public void startOn(Drawable dr) {
        dr.startPreviewing();

        Tools.editedObjectChanged(dr.getLayer());

        FilterGUI gui = createGUI(dr);
        new DialogBuilder()
            .title(getName())
            .menuBar(getMenuBar())
            .name("filterDialog")
            .content(gui)
            .align(FRAME_RIGHT)
            .withScrollbars()
            .enableCopyVisibleShortcut()
            .okAction(() -> dr.onFilterDialogAccepted(getName()))
            .cancelAction(dr::onFilterDialogCanceled)
            .show();
    }
}
