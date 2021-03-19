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

import pixelitor.filters.ParametrizedFilter;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.PAction;
import pixelitor.utils.OpenInBrowserAction;
import pixelitor.utils.Texts;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static pixelitor.filters.gui.UserPreset.*;

/**
 * The menu bar of the filters that have one.
 * It is also used for the text layer dialog.
 */
public class DialogMenuBar extends JMenuBar {
    public static final String PRESETS = Texts.i18n("presets");
    public static final String BUILT_IN_PRESETS = Texts.i18n("builtin_presets");

    private final DialogMenuOwner owner;
    private JMenu presetsMenu;
    private int numUserPresets = 0;
    private static final boolean CAN_USE_FILE_MANAGER = Desktop.isDesktopSupported()
        && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);

    public DialogMenuBar(DialogMenuOwner owner, boolean addPresets) {
        this.owner = owner;

        if (addPresets) {
            addPresetsMenu();
        }

        if (owner.hasHelp()) {
            JMenu helpMenu = new JMenu("Help");
            helpMenu.add(new OpenInBrowserAction("Online Help", owner.getHelpURL()));
            add(helpMenu);
        }
    }

    private void addPresetsMenu() {
        presetsMenu = new JMenu(PRESETS);

        if (owner.hasBuiltinPresets()) {
            JMenu builtinPresets = new JMenu(BUILT_IN_PRESETS);
            FilterState[] presets = owner.getBuiltinPresets();

            ParametrizedFilter pf = (ParametrizedFilter) owner;
            ParamSet paramSet = pf.getParamSet();

            for (FilterState preset : presets) {
                builtinPresets.add(preset.asAction(paramSet));
            }
            presetsMenu.add(builtinPresets);
        }

        if (owner.canHaveUserPresets()) {
            if (owner.hasBuiltinPresets()) {
                presetsMenu.addSeparator();
            }
            Action savePresetAction = new PAction("Save Preset...") {
                @Override
                public void onClick() {
                    String presetName = Dialogs.showInputDialog(
                        presetsMenu, "Preset Name", "Preset Name:");
                    if (presetName == null || presetName.isBlank()) {
                        return;
                    }

                    presetName = Utils.toFileName(presetName);
                    UserPreset preset = owner.createUserPreset(presetName);
                    addNewUserPreset(preset, owner);
                }
            };
            JMenuItem savePresetMI = new JMenuItem(savePresetAction);
            savePresetMI.setName("savePreset");
            presetsMenu.add(savePresetMI);
            List<UserPreset> userPresets = loadPresets(owner.getPresetDirName());
            numUserPresets = userPresets.size();
            if (numUserPresets > 0) {
                addManagePresetsMenu();
                presetsMenu.addSeparator();

                for (UserPreset preset : userPresets) {
                    presetsMenu.add(preset.asAction(owner));
                }
            }

            add(presetsMenu);
        }
    }

    private void addManagePresetsMenu() {
        if (!CAN_USE_FILE_MANAGER) {
            return;
        }
        presetsMenu.add(new PAction("Manage Presets...") {
            @Override
            public void onClick() {
                try {
                    String dirPath = PRESETS_DIR + FILE_SEPARATOR + owner.getPresetDirName();
                    Desktop.getDesktop().open(new File(dirPath));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void addNewUserPreset(UserPreset preset, DialogMenuOwner owner) {
        if (numUserPresets == 0) {
            addManagePresetsMenu();
            presetsMenu.addSeparator();
        } else if (preset.fileExists()) {
            String title = "Preset exists";
            String msg = String.format("The preset \"%s\" already exists. Overwrite?",
                preset.getName());
            int msgType = JOptionPane.WARNING_MESSAGE;
            if (!Dialogs.showYesNoDialog(presetsMenu, title, msg, msgType)) {
                return;
            }
            // "yes" was pressed, remove the old preset from the menu
            Component[] menuComponents = presetsMenu.getMenuComponents();
            for (Component item : menuComponents) {
                if (item instanceof JMenuItem) {
                    JMenuItem menuItem = (JMenuItem) item;
                    if (menuItem.getText().equals(preset.getName())) {
                        presetsMenu.remove(menuItem);
                        break;
                    }
                }
            }
        }
        Action presetAction = preset.asAction(owner);
        JMenuItem presetMI = new JMenuItem(presetAction);
        presetMI.setName(preset.getName());
        presetsMenu.add(presetMI);
        preset.save();
        numUserPresets++;
    }
}
