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

import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.PAction;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import static pixelitor.filters.gui.UserPreset.FILE_SEPARATOR;
import static pixelitor.filters.gui.UserPreset.PRESETS_DIR;

public interface PresetOwner {
    boolean canHaveUserPresets();

    // should be final, but Java doesn't allow both default and final
    default UserPreset createUserPreset(String presetName) {
        UserPreset preset = new UserPreset(presetName, getPresetDirName());
        saveStateTo(preset);
        return preset;
    }

    void saveStateTo(UserPreset preset);

    void loadUserPreset(UserPreset preset);

    String getPresetDirName();

    public default PAction createManagePresetsAction() {
        return new PAction("Manage Presets...") {
            @Override
            public void onClick() {
                try {
                    String dirPath = PRESETS_DIR + FILE_SEPARATOR + getPresetDirName();
                    Desktop.getDesktop().open(new File(dirPath));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    default Action createSavePresetAction(Component parent,
                                          Consumer<UserPreset> menuAdder,
                                          Consumer<UserPreset> menuRemover) {
        Action savePresetAction = new PAction("Save Preset...") {
            @Override
            public void onClick() {
                String presetName = Dialogs.showInputDialog(
                    parent, "Preset Name", "Preset Name:");
                if (presetName == null || presetName.isBlank()) {
                    return;
                }

                presetName = Utils.toFileName(presetName);
                UserPreset preset = createUserPreset(presetName);

                if (preset.fileExists()) {
                    String title = "Preset exists";
                    String msg = String.format("The preset \"%s\" already exists. Overwrite?",
                        preset.getName());
                    int msgType = JOptionPane.WARNING_MESSAGE;
                    if (!Dialogs.showYesNoDialog(parent, title, msg, msgType)) {
                        return;
                    }
                    // remove the old preset from the menus
                    menuRemover.accept(preset);
                }
                preset.save();

                menuAdder.accept(preset);
            }
        };
        return savePresetAction;
    }
}
