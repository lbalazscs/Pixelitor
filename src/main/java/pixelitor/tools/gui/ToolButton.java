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

package pixelitor.tools.gui;

import pixelitor.AppMode;
import pixelitor.filters.gui.UserPresetMenuHelper;
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
import java.awt.event.ItemEvent;

/**
 * A button that activates a tool.
 */
public class ToolButton extends JToggleButton {
    public static final int ICON_SIZE = 28;
    public static Color darkThemeActiveIconColor = Themes.DEFAULT_ACCENT_COLOR.asColor();

    private final Tool tool;

    private JPopupMenu presetsMenu;
    private boolean popupMenuPopulated = false;

    public ToolButton(Tool tool) {
        this.tool = tool;
        tool.setButton(this);

        // used for component lookup when testing
        setName(tool.getName() + " Button");

        putClientProperty("JComponent.sizeVariant", "mini");

        setupIcons(tool);

        setToolTipText("<html>" + tool.getName() + " (<b>" + tool.getHotkey() + "</b>)");

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
            ? toolIcon.copyWithColor(darkThemeActiveIconColor)
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
                    populatePresetsMenu();
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
    private void populatePresetsMenu() {
        if (AppMode.isDevelopment()) {
            presetsMenu.add(new TaskAction("Internal State...", () ->
                Debug.showTree(tool, tool.getName())));
            presetsMenu.addSeparator();
        }

        new UserPresetMenuHelper(tool, presetsMenu, this)
            .addSaveAndUserPresets();
    }

    public Tool getTool() {
        return tool;
    }

    public static void setDarkThemeActiveIconColor(Color darkThemeActiveIconColor) {
        ToolButton.darkThemeActiveIconColor = darkThemeActiveIconColor;
    }
}
