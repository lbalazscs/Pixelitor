/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.VisibleForTesting;

import javax.swing.*;
import java.awt.CardLayout;

/**
 * The {@link ToolSettingsPanel}s for each tool in a CardLayout
 */
public class ToolSettingsPanelContainer extends JPanel {
    private static ToolSettingsPanelContainer instance;

    private ToolSettingsPanelContainer() {
        setLayout(new CardLayout());

        Tool[] tools = Tools.getAll();
        for (Tool tool : tools) {
            var p = new ToolSettingsPanel();
            tool.setSettingsPanel(p);
            tool.initSettingsPanel();
            add(p, tool.getShortName());
        }
    }

    public static ToolSettingsPanelContainer get() {
        if (instance == null) {
            instance = new ToolSettingsPanelContainer();
        }
        return instance;
    }

    @VisibleForTesting
    public static void setInstance(ToolSettingsPanelContainer instance) {
        ToolSettingsPanelContainer.instance = instance;
    }

    public void showSettingsFor(Tool tool) {
        CardLayout cl = (CardLayout) getLayout();
        cl.show(this, tool.getShortName());
    }

    public void randomizeToolSettings() {
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            ToolSettingsPanel tsp = (ToolSettingsPanel) getComponent(i);
            if (tsp.isVisible()) {
                try {
                    GUIUtils.randomizeWidgetsOn(tsp);
                } catch (Throwable e) {
                    // assertj-swing sometimes loses the stack trace
                    // of Errors, this is a workaround
                    if (e instanceof AssertionError) {
                        throw new RuntimeException(e);
                    } else {
                        throw e;
                    }
                }
            }
        }
    }
}
