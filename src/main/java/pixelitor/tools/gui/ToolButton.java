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

package pixelitor.tools.gui;

import pixelitor.AppMode;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.TaskAction;
import pixelitor.gui.utils.Themes;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.utils.debug.Debug;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.util.List;

/**
 * A button that activates a tool.
 */
public class ToolButton extends JToggleButton {
    public static final int ICON_SIZE = 28;
    private static Color darkThemeSelectedColor = Themes.DEFAULT_ACCENT_COLOR.asColor();

    private final Tool tool;

    private int numPresets;
    private JPopupMenu presetsMenu;

    public ToolButton(Tool tool) {
        this.tool = tool;
        tool.setButton(this);

        // used for component lookup when testing
        setName(tool.getName() + " Button");

        putClientProperty("JComponent.sizeVariant", "mini");

        setupIcons(tool);

        setToolTipText("<html>" + tool.getName()
            + " (<b>" + tool.getActivationKey() + "</b>)");

        // Adds a listener to activate the tool when the button is selected.
        // An item listener is better than an action listener because it
        // is also triggered by keyboard focus traversal selections.
        addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Tools.start(tool);
            }
        });

        if (tool.canHaveUserPresets()) {
            initPresetsPopup(tool);
        }
    }

    @Override
    public void updateUI() {
        if (tool != null) { // changing the theme
            setupIcons(tool);
        }
        super.updateUI();
    }

    private void setupIcons(Tool tool) {
        VectorIcon toolIcon = tool.createIcon();
        setIcon(toolIcon);

        VectorIcon selectedIcon = Themes.getActive().isDark()
            ? toolIcon.copy(darkThemeSelectedColor)
            : toolIcon;
        setSelectedIcon(selectedIcon);
    }

    private void initPresetsPopup(Tool tool) {
        List<UserPreset> startupPresets = UserPreset.detectPresetNames(tool.getPresetDirName());
        numPresets = startupPresets.size();

        presetsMenu = new JPopupMenu();

        if (AppMode.isDevelopment()) {
            presetsMenu.add(new TaskAction("Internal State...", () ->
                Debug.showTree(tool, tool.getName())));
            presetsMenu.addSeparator();
        }

        presetsMenu.add(tool.createSavePresetAction(this,
            this::addPresetMenuItem, this::removePresetMenuItem));

        if (!startupPresets.isEmpty()) {
            if (GUIUtils.CAN_USE_FILE_MANAGER) {
                presetsMenu.add(tool.createManagePresetsAction());
            }
            presetsMenu.addSeparator();
            for (UserPreset preset : startupPresets) {
                presetsMenu.add(preset.createAction(tool));
            }
        }

        setComponentPopupMenu(presetsMenu);
    }

    private void addPresetMenuItem(UserPreset preset) {
        if (numPresets == 0) {
            if (GUIUtils.CAN_USE_FILE_MANAGER) {
                presetsMenu.add(tool.createManagePresetsAction());
            }
            presetsMenu.addSeparator();
        }
        presetsMenu.add(preset.createAction(tool));
        numPresets++;
    }

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
