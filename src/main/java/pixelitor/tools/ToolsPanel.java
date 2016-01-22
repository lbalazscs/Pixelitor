/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.tools;

import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.layers.AddTextLayerAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * A panel where the user can select the tools
 */
public class ToolsPanel extends JPanel {

    public ToolsPanel() {
        Box verticalBox = Box.createVerticalBox();
        ButtonGroup group = new ButtonGroup();

        Tool[] tools = Tools.getTools();
        for (Tool tool : tools) {
            ToolButton toolButton = new ToolButton(tool);
            verticalBox.add(toolButton);
            group.add(toolButton);
            setupKeyboardShortcut(tool);
        }

        add(verticalBox);
        setDefaultTool();

        // in the menu it was added using T, not t
        Action textToolAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddTextLayerAction.INSTANCE.actionPerformed(null);
//                    TextLayer.createNew(PixelitorWindow.getInstance());
//                    TextFilter.getInstance().execute();
            }
        };
        GlobalKeyboardWatch.addKeyboardShortCut('t', true, "text", textToolAction);
    }

    private static void setupKeyboardShortcut(Tool tool) {
        Action pressToolAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Tools.currentTool != tool) {
                    tool.getButton().doClick();
                }
            }
        };

        String toolName = tool.getName();
        char activationChar = tool.getActivationKeyChar();

        GlobalKeyboardWatch.addKeyboardShortCut(activationChar, true, toolName, pressToolAction);
    }

    private static void setDefaultTool() {
        Tools.setCurrentTool(Tools.BRUSH);
        Tools.currentTool.getButton().setSelected(true);
    }
}
