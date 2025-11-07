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

package pixelitor.tools.gui;

import pixelitor.AppMode;
import pixelitor.filters.gui.PresetActions;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.TaskAction;
import pixelitor.gui.utils.Themes;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.utils.debug.Debug;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.util.List;

/**
 * A button that activates a tool.
 */
public class ToolButton extends JToggleButton {
    public static final int ICON_SIZE = 28;
    public static Color darkThemeSelectedColor = Themes.DEFAULT_ACCENT_COLOR.asColor();

    private final Tool tool;

    private int numPresets;
    private JPopupMenu presetsMenu;
    private boolean popupMenuPopulated = false;

    public ToolButton(Tool tool) {
        this.tool = tool;
        tool.setButton(this);

        // used for component lookup when testing
        setName(tool.getName() + " Button");

        putClientProperty("JComponent.sizeVariant", "mini");

        setupIcons(tool);

        setToolTipText("<html>" + tool.getName()
            + " (<b>" + tool.getHotkey() + "</b>)");

        // Activates the tool when the button is selected.
        // An item listener is better than an action listener because it
        // is also triggered by keyboard focus traversal selections.
        addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Tools.start(tool);
            }
        });

        if (tool.shouldHaveUserPresetsMenu()) {
            initPresetsPopup(tool);
        }
    }

    @Override
    public void updateUI() {
        // this method can be called by the super constructor before 'tool' is initialized
        if (tool != null) { // changing the theme
            setupIcons(tool);
        }
        super.updateUI();
    }

    private void setupIcons(Tool tool) {
        VectorIcon toolIcon = VectorIcon.createToolIcon(tool.createIconPainter());
        setIcon(toolIcon);

        VectorIcon selectedIcon = Themes.getActive().isDark()
            ? toolIcon.copyWithColor(darkThemeSelectedColor)
            : toolIcon;
        setSelectedIcon(selectedIcon);
    }

    /**
     * Creates a right-click popup menu that can be used to save, load,
     * and manage configuration presets for the tool represented by this button.
     */
    private void initPresetsPopup(Tool tool) {
        presetsMenu = new JPopupMenu();

        // the popup menu is populated only when it is about to become visible
        presetsMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                if (!popupMenuPopulated) {
                    populatePresetsMenu(tool);
                    popupMenuPopulated = true;
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent event) {
            }
        });

        setComponentPopupMenu(presetsMenu);
    }

    /**
     * Loads presets from disk and populates the menu items.
     */
    private void populatePresetsMenu(Tool tool) {
        List<UserPreset> startupPresets = UserPreset.detectPresetNames(tool.getPresetDirName());
        numPresets = startupPresets.size();

        if (AppMode.isDevelopment()) {
            presetsMenu.add(new TaskAction("Internal State...", () ->
                Debug.showTree(tool, tool.getName())));
            presetsMenu.addSeparator();
        }

        presetsMenu.add(PresetActions.createSaveAction(tool, this,
            this::addPresetMenuItem, this::removePresetMenuItem));

        // if any existing presets were detected, add them to the menu
        if (!startupPresets.isEmpty()) {
            if (GUIUtils.CAN_USE_FILE_MANAGER) {
                presetsMenu.add(PresetActions.createManageAction(tool));
            }
            presetsMenu.addSeparator();
            for (UserPreset preset : startupPresets) {
                presetsMenu.add(preset.createAction(tool));
            }
        }
    }

    /**
     * Callback to dynamically add a new menu item to the presets
     * menu after a user has successfully saved a new preset.
     */
    private void addPresetMenuItem(UserPreset preset) {
        if (numPresets == 0) {
            // if this is the very first preset being added, then this
            // was not added during initialization, so add it now
            if (GUIUtils.CAN_USE_FILE_MANAGER) {
                presetsMenu.add(PresetActions.createManageAction(tool));
            }
            presetsMenu.addSeparator();
        }

        presetsMenu.add(preset.createAction(tool));
        numPresets++;
    }

    /**
     * Callback to dynamically remove the old menu item
     * when the user overwrites an existing preset.
     */
    private void removePresetMenuItem(UserPreset preset) {
        Component[] menuComponents = presetsMenu.getComponents();
        for (Component item : menuComponents) {
            if (item instanceof JMenuItem menuItem && menuItem.getText().equals(preset.getName())) {
                presetsMenu.remove(menuItem);
                numPresets--;
                break;
            }
        }
    }

    public Tool getTool() {
        return tool;
    }

    public static void setDarkThemeSelectedColor(Color darkThemeSelectedColor) {
        ToolButton.darkThemeSelectedColor = darkThemeSelectedColor;
    }
}
