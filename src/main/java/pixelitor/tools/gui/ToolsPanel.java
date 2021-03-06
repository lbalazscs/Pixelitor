/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.PAction;
import pixelitor.layers.AddTextLayerAction;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;

import javax.swing.*;
import java.awt.Dimension;

import static javax.swing.BoxLayout.Y_AXIS;

/**
 * The panel with the tool buttons and the color selector
 */
public class ToolsPanel extends JPanel {
    public ToolsPanel(PixelitorWindow pw, Dimension screenSize) {
        setLayout(new BoxLayout(this, Y_AXIS));

        addToolButtons(screenSize, pw);
        add(Box.createVerticalGlue());
        addColorSelector(pw);

        setupTShortCut();
    }

    private void addToolButtons(Dimension screenSize, PixelitorWindow pw) {
        ButtonGroup group = new ButtonGroup();
        Dimension buttonSize = calcToolButtonSize(screenSize, pw);
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

    private static Dimension calcToolButtonSize(Dimension screen, PixelitorWindow pw) {
        // the icons are 30x30
        Dimension buttonSize;
        int threshold = (int) (768 * pw.getHiDPIScaling().getScaleY());
        if (screen.height <= threshold) { // many laptops have 1366x768, minus the taskbar
            buttonSize = new Dimension(44, 38); // compromise
        } else {
            buttonSize = new Dimension(44, 44); // ideal
        }
        return buttonSize;
    }

    private static void setupKeyboardShortcut(Tool tool) {
        Action activateAction = new PAction() {
            @Override
            public void onClick() {
                if (Tools.currentTool != tool) {
                    tool.activate();
                }
            }
        };

        GlobalEvents.addHotKey(tool.getActivationKey(), activateAction);
    }
}
