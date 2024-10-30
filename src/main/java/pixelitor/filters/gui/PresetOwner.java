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

package pixelitor.filters.gui;

import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.PAction;
import pixelitor.io.FileUtils;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import static pixelitor.filters.gui.UserPreset.PRESETS_DIR;

/**
 * Represents a component (such as a filter or tool) that can
 * load (and possibly save) its current state as {@link Preset} objects.
 */
public interface PresetOwner {
    /**
     * Determines if this component supports {@link UserPreset}s.
     */
    boolean canHaveUserPresets();

    /**
     * Returns the directory name where this component's
     * {@link UserPreset}s are stored. The directory will be created as
     * a subdirectory of the application's preset root directory.
     */
    String getPresetDirName();

    /**
     * Creates a new user preset with the current state of this component.
     * Should not be called if {@link #canHaveUserPresets()} returns false.
     */
    default UserPreset createUserPreset(String presetName) {
        UserPreset preset = new UserPreset(presetName, getPresetDirName());
        saveStateTo(preset);
        return preset;
    }

    /**
     * Saves the current state of this component to the given {@link UserPreset}.
     */
    void saveStateTo(UserPreset preset);

    /**
     * Loads the state stored in the given {@link UserPreset}.
     */
    void loadUserPreset(UserPreset preset);

    /**
     * Loads a {@link FilterState} into this component.
     */
    default void loadFilterState(FilterState filterState, boolean reset) {
        // used only for parametrized filters
        throw new UnsupportedOperationException();
    }

    /**
     * Creates an action that opens the {@link UserPreset} directory
     * in the system's file manager.
     */
    default PAction createManagePresetsAction() {
        return new PAction("Manage Presets...", () -> {
            try {
                String dirPath = PRESETS_DIR + File.separator + getPresetDirName();
                Desktop.getDesktop().open(new File(dirPath));
            } catch (IOException ex) {
                Messages.showException(ex);
            }
        });
    }

    /**
     * Creates an {@link Action} that prompts the user to save
     * the current state as a new {@link UserPreset}.
     */
    default Action createSavePresetAction(Component parent,
                                          Consumer<UserPreset> menuAdder,
                                          Consumer<UserPreset> menuRemover) {
        return new PAction("Save Preset...", () ->
            savePreset(parent, menuRemover, menuAdder));
    }

    private void savePreset(Component parent, Consumer<UserPreset> menuRemover, Consumer<UserPreset> menuAdder) {
        String presetName = Dialogs.showInputDialog(
            parent, "Preset Name", "Preset Name:");
        if (presetName == null || presetName.isBlank()) {
            return;
        }

        presetName = FileUtils.sanitizeToFileName(presetName);
        UserPreset preset = createUserPreset(presetName);

        if (preset.fileExists()) {
            boolean overwrite = confirmOverwrite(parent, preset.getName());
            if (!overwrite) {
                return;
            }
            // remove the overwritten preset from the menus
            menuRemover.accept(preset);
        }
        preset.save();
        menuAdder.accept(preset);
    }

    private static boolean confirmOverwrite(Component parent, String presetName) {
        String title = "Preset exists";
        String msg = String.format("The preset \"%s\" already exists. Overwrite?",
            presetName);
        int msgType = JOptionPane.WARNING_MESSAGE;
        return Dialogs.showYesNoDialog(parent, title, msg, msgType);
    }
}
