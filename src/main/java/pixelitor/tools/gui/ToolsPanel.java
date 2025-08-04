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

import pixelitor.colors.FgBgColorSelector;
import pixelitor.colors.FgBgColors;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.StatusBar;
import pixelitor.gui.WorkSpace;
import pixelitor.gui.utils.TaskAction;
import pixelitor.layers.AddTextLayerAction;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The panel with the tool buttons and the color selector
 */
public class ToolsPanel extends JPanel {
    public ToolsPanel(PixelitorWindow pw, Dimension screenSize) {
        Dimension buttonSize = calcToolButtonSize(screenSize, pw);

        JComponent colorSelector = createColorSelector(pw);
        int heightHint = calcHeightHint(pw, colorSelector, buttonSize);

        JPanel buttonsPanel = new JPanel(new ToolButtonsLayout(buttonSize.width, buttonSize.height, 0, heightHint));
        addToolButtons(buttonsPanel);

        setLayout(new BorderLayout());
        add(buttonsPanel, BorderLayout.CENTER);
        add(colorSelector, BorderLayout.SOUTH);

        setupTShortCut();
    }

    private static int calcHeightHint(PixelitorWindow pw, JComponent colorSelector, Dimension buttonSize) {
        // get the preferred heights of all other components that take up vertical space.
        int menuBarHeight = pw.getJMenuBar().getPreferredSize().height;
        WorkSpace workSpace = pw.getWorkSpace();
        int toolSettingsHeight = workSpace.areToolsVisible() ? ToolSettingsPanelContainer.get().getPreferredSize().height : 0;
        int statusBarHeight = workSpace.isStatusBarVisible() ? StatusBar.get().getPreferredSize().height : 0;
        int colorSelectorHeight = colorSelector.getPreferredSize().height;

        // the window's insets include the title bar
        int windowInsetsHeight = pw.getInsets().top + pw.getInsets().bottom;

        // sum of all vertical space NOT available to the buttons panel
        int totalOtherHeight = menuBarHeight + toolSettingsHeight + statusBarHeight + colorSelectorHeight + windowInsetsHeight;

        // the total window height minus all other components
        int heightHint = pw.getHeight() - totalOtherHeight;

        // ensure the hint is a positive value
        heightHint = Math.max(heightHint, buttonSize.height);
        return heightHint;
    }

    private static void addToolButtons(JPanel buttonContainer) {
        ButtonGroup group = new ButtonGroup();

        List<Tool[]> sharedHotkeyGroups = Tools.getSharedHotkeyGroups();
        for (Tool[] toolGroup : sharedHotkeyGroups) {
            setupSharedHotkey(toolGroup);
        }

        Set<Tool> toolsWithSharedHotkeys = new HashSet<>();
        for (Tool[] toolGroup : sharedHotkeyGroups) {
            Collections.addAll(toolsWithSharedHotkeys, toolGroup);
        }

        for (Tool tool : Tools.getAll()) {
            ToolButton toolButton = new ToolButton(tool);
            buttonContainer.add(toolButton);
            group.add(toolButton);

            if (!toolsWithSharedHotkeys.contains(tool)) {
                setupHotkey(tool);
            }
        }
    }

    private static JComponent createColorSelector(PixelitorWindow pw) {
        FgBgColorSelector colorSelector = new FgBgColorSelector(pw);
        FgBgColors.setUI(colorSelector);
        return colorSelector;
    }

    private static void setupTShortCut() {
        // There is no text tool, but pressing T should add a text layer.
        // In the menu it was added using T, not t.
        GlobalEvents.registerHotkey('T', AddTextLayerAction.INSTANCE);
    }

    private static Dimension calcToolButtonSize(Dimension screen, PixelitorWindow pw) {
        int effectiveScreenHeight = (int) (screen.height / pw.getHiDPIScaling().getScaleY());

        int heightAdjustment;
        if (effectiveScreenHeight < 700) {
            // 720 is the lowest supported height.
            // The taskbar is already subtracted from the screen size.
            heightAdjustment = 8;
        } else if (effectiveScreenHeight < 770) {
            // Laptops with the height of 768 px are also common.
            heightAdjustment = 10;
        } else {
            // in the ideal case the button can be square
            heightAdjustment = 14;
        }

        return new Dimension(
            ToolButton.ICON_SIZE + 14,
            ToolButton.ICON_SIZE + heightAdjustment);
    }

    private static void setupHotkey(Tool tool) {
        Action activateToolAction = new TaskAction(() -> {
            if (Tools.activeTool != tool) {
                tool.activate();
            }
        });

        GlobalEvents.registerHotkey(tool.getHotkey(), activateToolAction);
    }

    private static void setupSharedHotkey(Tool... sharingTools) {
        Action cycleToolsAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int activeIndex = -1;
                for (int i = 0; i < sharingTools.length; i++) {
                    if (sharingTools[i].isActive()) {
                        activeIndex = i;
                        break;
                    }
                }

                // activate the next tool, or the first tool if
                // none of the sharing tools were active
                int nextIndex = (activeIndex + 1) % sharingTools.length;
                sharingTools[nextIndex].activate();
            }
        };
        // all tools in a group are expected to have the same hotkey
        char key = sharingTools[0].getHotkey();
        GlobalEvents.registerHotkey(key, cycleToolsAction);
    }
}
