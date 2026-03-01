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
import java.util.List;

/**
 * Encapsulates the logic for building and updating user preset menus.
 * Shared between {@link DialogMenuBar} and {@link ToolButton}.
 */
public class UserPresetMenuHelper {
    private static final String USER_PRESET_MENU_KEY = "isUserPreset";

    private final PresetOwner owner;
    private final JComponent menu; // a JMenu or a JPopupMenu
    private final Component dialogParent;
    private boolean managePresetsMenuAdded = false;

    public UserPresetMenuHelper(PresetOwner owner, JComponent menu, Component dialogParent) {
        this.owner = owner;
        this.menu = menu;
        this.dialogParent = dialogParent;
    }

    public void addSaveAndUserPresets() {
        addSavePresetMenuItem();

        List<UserPreset> userPresets = UserPreset.detectPresetNames(owner.getPresetDirName());
        if (!userPresets.isEmpty()) {
            addManagePresetsMenu();
            addSeparator();
            managePresetsMenuAdded = true;

            for (UserPreset preset : userPresets) {
                addUserPresetMenuItem(preset);
            }
        }
    }

    private void addSavePresetMenuItem() {
        Action savePresetAction = PresetActions.createSaveAction(
            owner, dialogParent,
            this::addNewUserPreset,
            this::removeUserPreset);

        JMenuItem presetMI = new JMenuItem(savePresetAction);
        presetMI.setName("savePreset");
        menu.add(presetMI);
    }

    private void addManagePresetsMenu() {
        if (!GUIUtils.CAN_USE_FILE_MANAGER) {
            return;
        }
        Action manageAction = PresetActions.createManageAction(owner, dialogParent);
        if (menu instanceof JMenu m) {
            m.add(manageAction);
        } else if (menu instanceof JPopupMenu pm) {
            pm.add(manageAction);
        }
    }

    private void addNewUserPreset(UserPreset preset) {
        if (!managePresetsMenuAdded) {
            addManagePresetsMenu();
            addSeparator();
            managePresetsMenuAdded = true;
        }
        addUserPresetMenuItem(preset);
    }

    private void addUserPresetMenuItem(UserPreset preset) {
        JMenuItem presetMI = new JMenuItem(preset.createAction(owner));
        presetMI.setName(preset.getName());

        // ensure that user presets can be distinguished from utility menu items
        presetMI.putClientProperty(USER_PRESET_MENU_KEY, Boolean.TRUE);

        menu.add(presetMI);
    }

    /**
     * Removes the menu item corresponding to a user preset.
     * Called when a preset is about to be overwritten.
     */
    private void removeUserPreset(UserPreset preset) {
        Component[] menuComponents;
        if (menu instanceof JMenu m) {
            menuComponents = m.getMenuComponents();
        } else {
            menuComponents = menu.getComponents();
        }

        JMenuItem fallbackMatch = null;

        for (Component item : menuComponents) {
            if (item instanceof JMenuItem menuItem) {
                if (isMenuItemUserPreset(menuItem)) {
                    String itemName = menuItem.getName();

                    if (preset.getName().equals(itemName)) {
                        // exact match found (preferable for case-sensitive OS like Linux)
                        menu.remove(menuItem);
                        return;
                    } else if (preset.getName().equalsIgnoreCase(itemName)) {
                        // keep track of a case-insensitive match for Windows/Mac collisions
                        fallbackMatch = menuItem;
                    }
                }
            }
        }

        // if no exact case match was found, use the case-insensitive fallback
        if (fallbackMatch != null) {
            menu.remove(fallbackMatch);
        }
    }

    private static boolean isMenuItemUserPreset(JMenuItem menuItem) {
        return Boolean.TRUE.equals(menuItem.getClientProperty(USER_PRESET_MENU_KEY));
    }

    private void addSeparator() {
        if (menu instanceof JMenu m) {
            m.addSeparator();
        } else if (menu instanceof JPopupMenu pm) {
            pm.addSeparator();
        }
    }
}
