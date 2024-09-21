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
import static pixelitor.tools.gui.ToolButton.TOOL_ICON_SIZE;

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
        // There is no text tool, but pressing T should add a text layer.
        // In the menu it was added using T, not t.
        GlobalEvents.registerHotKey('T', AddTextLayerAction.INSTANCE);
    }

    private static Dimension calcToolButtonSize(Dimension screen, PixelitorWindow pw) {
        Dimension buttonSize;

        int effectiveScreenHeight = (int) (screen.height / pw.getHiDPIScaling().getScaleY());
        if (effectiveScreenHeight < 700) {
            // 720 is the lowest supported height.
            // The taskbar is already subtracted from the screen size.
            buttonSize = new Dimension(TOOL_ICON_SIZE + 14, TOOL_ICON_SIZE + 8);
        } else if (effectiveScreenHeight < 770) {
            // Laptops with the height of 768 px are also common.
            buttonSize = new Dimension(TOOL_ICON_SIZE + 14, TOOL_ICON_SIZE + 10);
        } else {
            // in the ideal case the button can be square
            buttonSize = new Dimension(TOOL_ICON_SIZE + 14, TOOL_ICON_SIZE + 14);
        }

        return buttonSize;
    }

    private static void setupKeyboardShortcut(Tool tool) {
        Action activateAction = new PAction(() -> {
            if (Tools.currentTool != tool) {
                tool.activate();
            }
        });

        GlobalEvents.registerHotKey(tool.getActivationKey(), activateAction);
    }
}
