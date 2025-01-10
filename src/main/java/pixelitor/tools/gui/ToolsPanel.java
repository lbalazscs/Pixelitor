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
import pixelitor.gui.utils.TaskAction;
import pixelitor.layers.AddTextLayerAction;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

/**
 * The panel with the tool buttons and the color selector
 */
public class ToolsPanel extends JPanel {
    public ToolsPanel(PixelitorWindow pw, Dimension screenSize) {
        Dimension buttonSize = calcToolButtonSize(screenSize, pw);

        // we need to give a hint to the layout manager, but at
        // this point neither the panel nor the window size is known
        int heightHint = screenSize.height - 168;

        JPanel buttonsPanel = new JPanel(new ToolButtonsLayout(buttonSize.width, buttonSize.height, 0, heightHint));
        addToolButtons(buttonsPanel);

        setLayout(new BorderLayout());
        add(buttonsPanel, BorderLayout.CENTER);
        addColorSelector(pw);

        setupTShortCut();
    }

    private static void addToolButtons(JPanel toolsPanel) {
        ButtonGroup group = new ButtonGroup();
        Tool[] tools = Tools.getAll();
        for (Tool tool : tools) {
            ToolButton toolButton = new ToolButton(tool);
            toolsPanel.add(toolButton);
            group.add(toolButton);
            if (!tool.hasSharedHotkey()) {
                setupHotkey(tool);
            }
        }
        // manually register the hotkeys of the sharing tools
        setupSharedHotkey(Tools.RECTANGLE_SELECTION, Tools.ELLIPSE_SELECTION);
        setupSharedHotkey(Tools.LASSO_SELECTION, Tools.POLY_SELECTION);
    }

    private void addColorSelector(PixelitorWindow pw) {
        FgBgColorSelector colorSelector = new FgBgColorSelector(pw);
        FgBgColors.setUI(colorSelector);
        add(colorSelector, BorderLayout.SOUTH);
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
        Action activateAction = new TaskAction(() -> {
            if (Tools.activeTool != tool) {
                tool.activate();
            }
        });

        GlobalEvents.registerHotkey(tool.getHotkey(), activateAction);
    }

    private static void setupSharedHotkey(Tool... sharingTools) {
        Action multiToolAction = new AbstractAction() {
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
        char key = sharingTools[0].getHotkey();
        GlobalEvents.registerHotkey(key, multiToolAction);
    }
}
