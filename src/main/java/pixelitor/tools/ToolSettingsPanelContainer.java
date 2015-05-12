/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.CardLayout;

/**
 *
 */
public final class ToolSettingsPanelContainer extends JPanel {
    public static final ToolSettingsPanelContainer INSTANCE = new ToolSettingsPanelContainer();

    private ToolSettingsPanelContainer() {
        setLayout(new CardLayout());

        Tool[] tools = Tools.getTools();
        for (Tool tool : tools) {
            ToolSettingsPanel p = new ToolSettingsPanel();
            tool.setSettingsPanel(p);
            tool.initSettingsPanel();
            add(p, tool.getName());
        }
    }

    public void showSettingsFor(Tool tool) {
        CardLayout cl = (CardLayout) (getLayout());
        cl.show(this, tool.getName());
    }

    public void randomizeToolSettings() {
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            ToolSettingsPanel tsp = (ToolSettingsPanel) getComponent(i);
            Utils.randomizeGUIWidgetsOn(tsp);
        }
    }
}
