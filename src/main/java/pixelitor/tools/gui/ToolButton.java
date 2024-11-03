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
import pixelitor.gui.utils.PAction;
import pixelitor.gui.utils.Themes;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.utils.debug.Debug;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ItemEvent;
import java.util.List;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;

/**
 * The button that activates a tool
 */
public class ToolButton extends JToggleButton {
    public static final int TOOL_ICON_SIZE = 28;
    private static Color darkThemeSelectedColor = Themes.DEFAULT_ACCENT_COLOR.asColor();

    private final Tool tool;

    private int numPresets;
    private JPopupMenu popup;

    public ToolButton(Tool tool) {
        this.tool = tool;
        tool.setButton(this);

        // used for component lookup when testing
        setName(tool.getName() + " Button");

        putClientProperty("JComponent.sizeVariant", "mini");

        setIcons(tool);

        setToolTipText("<html>" + tool.getName()
            + " (<b>" + tool.getActivationKey() + "</b>)");

        // An item listener is better than an action listener because it
        // is also triggered by keyboard focus traversal selections.
        addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Tools.start(tool);
            }
        });

        if (tool.canHaveUserPresets()) {
            createPresetsPopup(tool);
        }

//        setPreferredSize(preferredSize);
    }

    @Override
    public void updateUI() {
        if (tool != null) { // changing the theme
            setIcons(tool);
        }
        super.updateUI();
    }

    private void setIcons(Tool tool) {
        VectorIcon toolIcon = tool.createIcon();
        setIcon(toolIcon);

        if (Themes.getCurrent().isDark()) {
            setSelectedIcon(toolIcon.copy(darkThemeSelectedColor));
        } else {
            // set it explicitly, so that it's updated
            // when the theme changes from dark to light
            setSelectedIcon(toolIcon);
        }
    }

    private void createPresetsPopup(Tool tool) {
        List<UserPreset> startupPresets = UserPreset.detectPresetNames(tool.getPresetDirName());
        numPresets = startupPresets.size();

        popup = new JPopupMenu();

        if (AppMode.isDevelopment()) {
            popup.add(new PAction("Internal State...", () ->
                Debug.showTree(tool, tool.getName())));
            popup.addSeparator();
        }

        popup.add(tool.createSavePresetAction(this,
            this::addPreset, this::removePreset));
        if (!startupPresets.isEmpty()) {
            if (GUIUtils.CAN_USE_FILE_MANAGER) {
                popup.add(tool.createManagePresetsAction());
            }
            popup.addSeparator();
            for (UserPreset preset : startupPresets) {
                popup.add(preset.createAction(tool));
            }
        }

        setComponentPopupMenu(popup);
    }

    private void addPreset(UserPreset preset) {
        if (numPresets == 0) {
            if (GUIUtils.CAN_USE_FILE_MANAGER) {
                popup.add(tool.createManagePresetsAction());
            }
            popup.addSeparator();
        }
        popup.add(preset.createAction(tool));
        numPresets++;
    }

    private void removePreset(UserPreset preset) {
        Component[] menuComponents = popup.getComponents();
        for (Component item : menuComponents) {
            if (item instanceof JMenuItem menuItem) {
                if (menuItem.getText().equals(preset.getName())) {
                    popup.remove(menuItem);
                    numPresets--;
                    break;
                }
            }
        }
    }

    public Tool getTool() {
        return tool;
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (tool == Tools.BRUSH) {
            g2d.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
        }
        super.paintComponent(g2d);
    }

    public static void setDarkThemeSelectedColor(Color darkThemeSelectedColor) {
        ToolButton.darkThemeSelectedColor = darkThemeSelectedColor;
    }
}
