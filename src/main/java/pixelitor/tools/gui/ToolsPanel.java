/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.AddTextLayerAction;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import static javax.swing.BoxLayout.Y_AXIS;

/**
 * The panel with the tool buttons and the color selector
 */
public class ToolsPanel extends JPanel {
    public ToolsPanel(PixelitorWindow pw, Dimension screenSize) {
        setLayout(new BoxLayout(this, Y_AXIS));

        addToolButtons(screenSize);
        add(Box.createVerticalGlue());
        addColorSelector(pw);

        setupTShortCut();
    }

    private void addToolButtons(Dimension screenSize) {
        ButtonGroup group = new ButtonGroup();
        Dimension buttonSize = calcToolButtonSize(screenSize);
        Tool[] tools = Tools.getAll();
        for (Tool tool : tools) {
            ToolButton toolButton = new ToolButton(tool, buttonSize);
            toolButton.setAlignmentX(CENTER_ALIGNMENT);
            add(toolButton);
            group.add(toolButton);
            setupKeyboardShortcut(tool);
        }
    }

    private void addColorSelector(PixelitorWindow pw) {
        FgBgColorSelector colorSelector = new FgBgColorSelector(pw);
        FgBgColors.setUI(colorSelector);
        colorSelector.setAlignmentX(CENTER_ALIGNMENT);
        add(colorSelector);
    }

    private static void setupTShortCut() {
        // there is no text tool, but pressing T should add a text layer
        // in the menu it was added using T, not t
        GlobalEvents.addHotKey('T', AddTextLayerAction.INSTANCE);
    }

    private static Dimension calcToolButtonSize(Dimension screen) {
        // the icons are 30x30
        Dimension buttonSize;
        if (screen.height <= 768) { // many laptops have 1366x768, minus the taskbar
            buttonSize = new Dimension(44, 38); // compromise
        } else {
            buttonSize = new Dimension(44, 44); // ideal
        }
        return buttonSize;
    }

    private static void setupKeyboardShortcut(Tool tool) {
        Action activateAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Tools.currentTool != tool) {
                    tool.activate();
                }
            }
        };

        GlobalEvents.addHotKey(tool.getActivationKey(), activateAction);
    }
}
