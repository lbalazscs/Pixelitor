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

import pixelitor.gui.utils.GUIUtils;
import pixelitor.utils.Texts;

import javax.swing.*;
import java.awt.Component;
import java.util.List;

import static pixelitor.filters.gui.UserPreset.detectPresetNames;

/**
 * A menu bar for dialogs that support presets or help.
 * Can be used by any component that implements {@link DialogMenuOwner}.
 */
public class DialogMenuBar extends JMenuBar {
    public static final String PRESETS = Texts.i18n("presets");
    public static final String BUILT_IN_PRESETS = Texts.i18n("builtin_presets");

    private final DialogMenuOwner owner;
    private JMenu presetsMenu;
    private int userPresetCount = 0;

    public DialogMenuBar(DialogMenuOwner owner) {
        this(owner, true);
    }

    public DialogMenuBar(DialogMenuOwner owner, boolean addPresets) {
        this.owner = owner;

        if (addPresets) {
            addPresetsMenu();
        }

        if (owner.hasHelp()) {
            add(this.owner.getHelp().createMenu());
        }
    }

    private void addPresetsMenu() {
        presetsMenu = new JMenu(PRESETS);

        if (owner.hasBuiltinPresets()) {
            addBuiltInPresets();
        }

        if (owner.shouldHaveUserPresetsMenu()) {
            addUserPresets();
        }

        assert presetsMenu.getMenuComponentCount() > 0;
        add(presetsMenu);
    }

    private void addUserPresets() {
        // add separator if we already have built-in presets
        if (owner.hasBuiltinPresets()) {
            presetsMenu.addSeparator();
        }

        addSavePresetMenuItem();

        // add detected, but unloaded user presets
        List<UserPreset> userPresets = detectPresetNames(owner.getPresetDirName());
        userPresetCount = userPresets.size();
        if (userPresetCount > 0) {
            addManagePresetsMenu();
            presetsMenu.addSeparator();

            for (UserPreset preset : userPresets) {
                presetsMenu.add(preset.createAction(owner));
            }
        }
    }

    private void addSavePresetMenuItem() {
        Action savePresetAction = owner.createSavePresetAction(presetsMenu,
            this::addNewUserPreset,
            this::removeUserPreset);
        addPresetMenuItem(savePresetAction, "savePreset");
    }

    private void addBuiltInPresets() {
        JMenu builtinPresets = new JMenu(BUILT_IN_PRESETS);
        Preset[] presets = owner.getBuiltinPresets();

        for (Preset preset : presets) {
            builtinPresets.add(preset.createAction(owner));
        }
        presetsMenu.add(builtinPresets);
    }

    private void addManagePresetsMenu() {
        if (!GUIUtils.CAN_USE_FILE_MANAGER) {
            return;
        }
        presetsMenu.add(owner.createManagePresetsAction());
    }

    private void addNewUserPreset(UserPreset preset) {
        // if this is the first user preset, also adds the
        // "Manage Presets" menu item and a separator
        if (userPresetCount == 0) {
            addManagePresetsMenu();
            presetsMenu.addSeparator();
        }
        addPresetMenuItem(preset.createAction(owner), preset.getName());

        userPresetCount++;
    }

    private void addPresetMenuItem(Action action, String name) {
        JMenuItem presetMI = new JMenuItem(action);
        presetMI.setName(name);
        presetsMenu.add(presetMI);
    }

    /**
     * Removes a user preset from the presets menu.
     * Called when a preset is about to be overwritten.
     */
    private void removeUserPreset(UserPreset preset) {
        Component[] menuComponents = presetsMenu.getMenuComponents();
        for (Component item : menuComponents) {
            if (item instanceof JMenuItem menuItem) {
                if (menuItem.getText().equals(preset.getName())) {
                    presetsMenu.remove(menuItem);
                    userPresetCount--;
                    break;
                }
            }
        }
    }

}
