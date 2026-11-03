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

import pixelitor.gui.utils.GUIUtils;
import pixelitor.tools.gui.ToolButton;

import javax.swing.*;
import java.awt.Component;

/**
 * Encapsulates the logic for building and updating user preset menus.
 * Shared between {@link DialogMenuBar} and {@link ToolButton}.
 */
public class UserPresetMenuHelper {
    private static final String USER_PRESET_MENU_KEY = "isUserPreset";

    private final PresetOwner owner;
    private final JPopupMenu popupMenu; // the popup of a JMenu, or a standalone JPopupMenu
    private final Component dialogParent;
    private boolean userPresetSectionStarted; // "Manage Presets" item + separator added

    public UserPresetMenuHelper(PresetOwner owner, JComponent menu, Component dialogParent) {
        this.owner = owner;
        this.popupMenu = switch (menu) {
            case JMenu m -> m.getPopupMenu();
            case JPopupMenu pm -> pm;
            default -> throw new IllegalArgumentException(
                "Unsupported menu type: " + menu.getClass().getName());
        };
        this.dialogParent = dialogParent;
    }

    public void addSaveAndUserPresets() {
        addSavePresetMenuItem();

        for (UserPreset preset : UserPreset.detectPresets(owner.getPresetDirName())) {
            addUserPreset(preset); // the first call also starts the user preset section
        }
    }

    private void addSavePresetMenuItem() {
        Action savePresetAction = PresetActions.createSaveAction(
            owner, dialogParent,
            this::addUserPreset,
            this::removeUserPreset);

        JMenuItem presetMI = new JMenuItem(savePresetAction);
        presetMI.setName("savePreset");
        popupMenu.add(presetMI);
    }

    private void addManagePresetsMenu() {
        if (GUIUtils.CAN_USE_FILE_MANAGER) {
            popupMenu.add(PresetActions.createManageAction(owner, dialogParent));
        }
    }

    private void addUserPreset(UserPreset preset) {
        if (!userPresetSectionStarted) {
            // the first user preset is preceded by the "Manage Presets" item and a separator
            addManagePresetsMenu();
            popupMenu.addSeparator();
            userPresetSectionStarted = true;
        }
        addUserPresetMenuItem(preset);
    }

    private void addUserPresetMenuItem(UserPreset preset) {
        JMenuItem presetMI = new JMenuItem(preset.createAction(owner));
        presetMI.setName(preset.getName());

        // ensure that user presets can be distinguished from utility menu items
        presetMI.putClientProperty(USER_PRESET_MENU_KEY, Boolean.TRUE);

        popupMenu.add(presetMI);
    }

    /**
     * Removes the menu item corresponding to a user preset.
     * Called when a preset is about to be overwritten.
     */
    private void removeUserPreset(UserPreset preset) {
        String presetName = preset.getName();
        JMenuItem caseInsensitiveMatch = null;

        for (Component c : popupMenu.getComponents()) {
            if (c instanceof JMenuItem menuItem && isMenuItemUserPreset(menuItem)) {
                String itemName = menuItem.getName();
                if (presetName.equals(itemName)) {
                    // exact match (preferable for Linux)
                    popupMenu.remove(menuItem);
                    return;
                }
                if (caseInsensitiveMatch == null && presetName.equalsIgnoreCase(itemName)) {
                    // file name collision on Windows
                    caseInsensitiveMatch = menuItem;
                }
            }
        }

        if (caseInsensitiveMatch != null) {
            popupMenu.remove(caseInsensitiveMatch);
        }
    }

    private static boolean isMenuItemUserPreset(JMenuItem menuItem) {
        return Boolean.TRUE.equals(menuItem.getClientProperty(USER_PRESET_MENU_KEY));
    }
}
