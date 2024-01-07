/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.GUIText;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.utils.OpenInBrowserAction;
import pixelitor.utils.Texts;

import javax.swing.*;
import java.awt.Component;
import java.util.List;

import static pixelitor.filters.gui.UserPreset.loadPresets;

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

    public DialogMenuBar(DialogMenuOwner owner) {
        this(owner, true);
    }

    public DialogMenuBar(DialogMenuOwner owner, boolean addPresets) {
        this.owner = owner;

        if (addPresets) {
            addPresetsMenu();
        }

        if (OpenInBrowserAction.CAN_BROWSE && owner.hasHelp()) {
            JMenu helpMenu = new JMenu(GUIText.HELP);
            helpMenu.add(new OpenInBrowserAction("Wikipedia", owner.getHelpURL()));
            add(helpMenu);
        }
    }

    private void addPresetsMenu() {
        presetsMenu = new JMenu(PRESETS);

        if (owner.hasBuiltinPresets()) {
            JMenu builtinPresets = new JMenu(BUILT_IN_PRESETS);
            Preset[] presets = owner.getBuiltinPresets();

            for (Preset preset : presets) {
                builtinPresets.add(preset.asAction(owner));
            }
            presetsMenu.add(builtinPresets);
        }

        if (owner.canHaveUserPresets()) {
            if (owner.hasBuiltinPresets()) {
                presetsMenu.addSeparator();
            }
            Action savePresetAction = owner.createSavePresetAction(presetsMenu,
                preset -> addNewUserPreset(preset, owner),
                this::removeOldPreset);
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
        if (!GUIUtils.CAN_USE_FILE_MANAGER) {
            return;
        }
        presetsMenu.add(owner.createManagePresetsAction());
    }

    private void addNewUserPreset(UserPreset preset, PresetOwner owner) {
        if (numUserPresets == 0) {
            addManagePresetsMenu();
            presetsMenu.addSeparator();
        }
        JMenuItem presetMI = new JMenuItem(preset.asAction(owner));
        presetMI.setName(preset.getName());
        presetsMenu.add(presetMI);
        numUserPresets++;
    }

    private void removeOldPreset(UserPreset preset) {
        Component[] menuComponents = presetsMenu.getMenuComponents();
        for (Component item : menuComponents) {
            if (item instanceof JMenuItem menuItem) {
                if (menuItem.getText().equals(preset.getName())) {
                    presetsMenu.remove(menuItem);
                    numUserPresets--;
                    break;
                }
            }
        }
    }
}
