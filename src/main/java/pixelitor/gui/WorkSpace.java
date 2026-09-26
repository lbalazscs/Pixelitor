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

package pixelitor.gui;

import pixelitor.menus.view.ShowHideAllAction;
import pixelitor.menus.view.ShowHidePanelAction;

import java.util.EnumMap;
import java.util.Map;

import static pixelitor.utils.AppPreferences.mainPrefs;

/**
 * Coordinates the visibility, toggle actions, and persistence of the toggleable panels.
 */
public class WorkSpace {
    private final Map<AppPanel, Boolean> visibilityMap = new EnumMap<>(AppPanel.class);
    private final PixelitorWindow pw;
    private boolean frameInitialized = false;

    private final Map<AppPanel, ShowHidePanelAction> panelActions = new EnumMap<>(AppPanel.class);
    private final ShowHideAllAction allAction;

    public WorkSpace(PixelitorWindow pw) {
        this.pw = pw;

        for (AppPanel panel : AppPanel.PANELS) {
            // only load preferences for panels that have their own persistent key
            if (panel.hasPrefKey()) {
                boolean visible = mainPrefs.getBoolean(panel.getPrefKey(), panel.isDefaultVisible());
                visibilityMap.put(panel, visible);
            }
        }

        // TOOL_SETTINGS visibility strictly follows TOOLS
        visibilityMap.put(AppPanel.TOOL_SETTINGS, isVisible(AppPanel.TOOLS));

        // initialize toggle actions for toggleable panels
        for (AppPanel panel : AppPanel.PANELS) {
            if (panel.hasToggleAction()) {
                panelActions.put(panel, new ShowHidePanelAction(panel, this));
            }
        }

        allAction = new ShowHideAllAction(this);
    }

    public boolean isVisible(AppPanel panel) {
        assert !frameInitialized || visibilityMap.get(panel) == panel.isShown();
        return visibilityMap.getOrDefault(panel, false);
    }

    public boolean hasAnyPanelVisible() {
        for (AppPanel panel : AppPanel.PANELS) {
            if (panel.hasToggleAction() && isVisible(panel)) {
                return true;
            }
        }
        return false;
    }

    public void setPanelVisible(AppPanel panel, boolean visible, boolean revalidate) {
        visibilityMap.put(panel, visible);
        pw.setPanelVisible(panel, visible, revalidate);

        if (panel == AppPanel.TOOLS) {
            visibilityMap.put(AppPanel.TOOL_SETTINGS, visible);
            pw.setPanelVisible(AppPanel.TOOL_SETTINGS, visible, revalidate);
        }

        // automatically synchronize the panel's toggle action text
        ShowHidePanelAction action = panelActions.get(panel);
        if (action != null) {
            action.updateText(visible);
        }

        // keep "Hide All / Restore Workspace" action synchronized
        if (allAction != null) {
            allAction.synchronizeState();
        }
    }

    public void restoreDefaults() {
        for (AppPanel panel : AppPanel.PANELS) {
            // only independently toggleable panels need their defaults restored directly
            if (panel.hasToggleAction()) {
                if (panel.isShown() != panel.isDefaultVisible()) {
                    setPanelVisible(panel, panel.isDefaultVisible(), false);
                }
            }
        }
        pw.getContentPane().revalidate();
        pw.getSidePanel().revalidate();
    }

    public void savePreferences() {
        for (AppPanel panel : AppPanel.PANELS) {
            // only persist panels with their own prefKey
            if (panel.hasPrefKey()) {
                mainPrefs.putBoolean(panel.getPrefKey(), visibilityMap.get(panel));
            }
        }
    }

    public ShowHidePanelAction getAction(AppPanel panel) {
        return panelActions.get(panel);
    }

    public ShowHideAllAction getAllAction() {
        return allAction;
    }

    public void setFrameInitialized(boolean frameInitialized) {
        this.frameInitialized = frameInitialized;
    }
}
