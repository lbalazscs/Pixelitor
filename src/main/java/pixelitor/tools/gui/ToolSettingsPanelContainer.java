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

import pixelitor.gui.utils.GUIUtils;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.utils.Texts;

import javax.swing.*;
import java.awt.CardLayout;
import java.util.ResourceBundle;

/**
 * The {@link ToolSettingsPanel}s for each tool in a CardLayout
 */
public class ToolSettingsPanelContainer extends JPanel {
    public ToolSettingsPanelContainer() {
        super(new CardLayout());

        ResourceBundle resources = Texts.getResources();

        Tool[] tools = Tools.getAll();
        for (Tool tool : tools) {
            var p = new ToolSettingsPanel();
            tool.setSettingsPanel(p);
            tool.initSettingsPanel(resources);
            add(p, tool.getShortName());
        }
    }

    public void showSettingsOf(Tool tool) {
        CardLayout cl = (CardLayout) getLayout();
        cl.show(this, tool.getShortName());
    }

    public void randomizeToolSettings() {
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            ToolSettingsPanel tsp = (ToolSettingsPanel) getComponent(i);
            if (tsp.isVisible()) {
                try {
                    GUIUtils.randomizeChildren(tsp);
                } catch (AssertionError e) {
                    // assertj-swing sometimes loses the stack trace
                    // of Errors, this is a workaround
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
