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

import pixelitor.colors.FgBgColorSelector;
import pixelitor.colors.FgBgColors;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.utils.TaskAction;
import pixelitor.layers.AddTextLayerAction;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The panel with the tool buttons and the color selector
 */
public class ToolsPanel extends JPanel {
    public ToolsPanel() {
        setLayout(new BorderLayout());

        add(createButtonsPanel(), BorderLayout.NORTH);
        add(createColorSelector(), BorderLayout.SOUTH);

        setupTHotkey();
    }

    private static JPanel createButtonsPanel() {
        int numCols = 2;
        JPanel buttonsPanel = new JPanel(new GridLayout(0, numCols));
        ButtonGroup group = new ButtonGroup();

        List<Tool[]> sharedHotkeyGroups = Tools.getSharedHotkeyGroups();
        for (Tool[] toolGroup : sharedHotkeyGroups) {
            setupSharedHotkey(toolGroup);
        }

        Set<Tool> toolsWithSharedHotkeys = new HashSet<>();
        for (Tool[] toolGroup : sharedHotkeyGroups) {
            Collections.addAll(toolsWithSharedHotkeys, toolGroup);
        }

        Tool[] tools = Tools.getAll();
        for (Tool tool : tools) {
            ToolButton toolButton = new ToolButton(tool);
            buttonsPanel.add(toolButton);
            group.add(toolButton);

            if (!toolsWithSharedHotkeys.contains(tool)) {
                setupHotkey(tool);
            }
        }
        return buttonsPanel;
    }

    private static JComponent createColorSelector() {
        FgBgColorSelector colorSelector = new FgBgColorSelector();
        FgBgColors.setUI(colorSelector);
        return colorSelector;
    }

    private static void setupTHotkey() {
        // There is no text tool, but pressing T should add a text layer.
        // In the menu it was added using T, not t.
        GlobalEvents.registerHotkey('T', AddTextLayerAction.INSTANCE);
    }

    private static void setupHotkey(Tool tool) {
        Action activateToolAction = new TaskAction(() -> {
            if (Tools.activeTool != tool) {
                tool.activate();
            }
        });

        GlobalEvents.registerHotkey(tool.getHotkey(), activateToolAction);
    }

    private static void setupSharedHotkey(Tool... toolGroup) {
        Action cycleToolsAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int activeIndex = -1;
                for (int i = 0; i < toolGroup.length; i++) {
                    if (toolGroup[i].isActive()) {
                        activeIndex = i;
                        break;
                    }
                }

                // activate the next tool, or the first tool if
                // none of the tools in the group were active
                int nextIndex = (activeIndex + 1) % toolGroup.length;
                toolGroup[nextIndex].activate();
            }
        };
        // all tools in a group are expected to have the same hotkey
        char key = toolGroup[0].getHotkey();
        GlobalEvents.registerHotkey(key, cycleToolsAction);
    }
}
