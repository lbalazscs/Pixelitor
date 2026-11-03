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

package pixelitor.filters.gui;

import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.TaskAction;
import pixelitor.io.FileUtils;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Static utility methods related to preset actions.
 */
public class PresetActions {
    private PresetActions() {
    }

    /**
     * Creates an action that opens the {@link UserPreset}
     * directory in the system's file manager.
     */
    public static TaskAction createManageAction(PresetOwner owner, Component dialogParent) {
        return new TaskAction("Manage Presets...", () -> {
            try {
                File directory = UserPreset.getSaveDir(owner.getPresetDirName());
                if (!directory.exists()) {
                    Dialogs.showDirectoryNotFoundError(dialogParent, directory);
                    return;
                }
                Desktop.getDesktop().open(directory);
            } catch (IOException ex) {
                Messages.showException(ex);
            }
        });
    }

    /**
     * Creates an {@link Action} that prompts the user to save
     * the current state as a new {@link UserPreset}.
     */
    public static Action createSaveAction(PresetOwner owner, Component parent,
                                          Consumer<UserPreset> menuAdder,
                                          Consumer<UserPreset> menuRemover) {
        return new TaskAction("Save Preset...", () ->
            promptAndSave(owner, parent, menuAdder, menuRemover));
    }

    private static void promptAndSave(PresetOwner owner, Component parent,
                                      Consumer<UserPreset> menuAdder,
                                      Consumer<UserPreset> menuRemover) {
        String presetName = Dialogs.showInputQuestion(
            parent, "Preset Name", "Preset Name:");
        if (presetName == null || presetName.isBlank()) {
            return;
        }

        presetName = FileUtils.sanitizeToFileName(presetName);
        UserPreset preset = owner.createUserPreset(presetName);
        if (preset == null) { // invalid input, the user was already warned
            return;
        }

        boolean overwriting = preset.fileExists();
        if (overwriting && !confirmOverwrite(parent, preset.getName())) {
            return;
        }

        // throws if writing fails, so the menu stays consistent with the disk
        preset.save();

        if (overwriting) {
            // remove the old menu item before adding the new one
            menuRemover.accept(preset);
        }
        menuAdder.accept(preset);
    }

    private static boolean confirmOverwrite(Component parent, String presetName) {
        String title = "Preset Exists";
        String msg = String.format("The preset \"%s\" already exists. Overwrite?",
            presetName);
        int msgType = JOptionPane.WARNING_MESSAGE;
        return Dialogs.showYesNoQuestion(parent, title, msg, msgType);
    }
}
