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

package pixelitor.menus.view;

import pixelitor.gui.AppPanel;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.WorkSpace;

import java.util.EnumSet;
import java.util.Set;

/**
 * The {@link javax.swing.Action} that toggles the visibility of all
 * toggleable panels at the same time.
 * The show action only re-shows the UI elements hidden by this action.
 */
public class ShowHideAllAction extends ShowHideAction {
    private final Set<AppPanel> previouslyShownPanels = EnumSet.noneOf(AppPanel.class);
    private final WorkSpace workSpace;

    public ShowHideAllAction(WorkSpace workSpace) {
        super("restore_ws", "hide_all", workSpace.hasAnyPanelVisible());
        this.workSpace = workSpace;
    }

    @Override
    public boolean isVisible() {
        return workSpace.hasAnyPanelVisible();
    }

    @Override
    public void setVisibility(boolean show) {
        var pw = PixelitorWindow.get();

        // when hiding, remember which toggleable panels are currently visible
        if (!show) {
            previouslyShownPanels.clear();
            for (AppPanel panel : AppPanel.PANELS) {
                if (panel.hasToggleAction() && workSpace.isVisible(panel)) {
                    previouslyShownPanels.add(panel);
                }
            }
        }

        // apply changes across all toggleable panels

        for (AppPanel panel : AppPanel.PANELS) {
            if (!panel.hasToggleAction()) {
                continue;
            }

            boolean targetVisible;
            if (show) {
                targetVisible = previouslyShownPanels.isEmpty()
                    ? panel.isDefaultVisible()
                    : previouslyShownPanels.contains(panel);
            } else {
                targetVisible = false;
            }

            if (workSpace.isVisible(panel) != targetVisible) {
                workSpace.setPanelVisible(panel, targetVisible, false);
            }
        }

        // revalidate only once at the end
        pw.getContentPane().revalidate();
        pw.getSidePanel().revalidate();

        synchronizeState();
    }

    public void synchronizeState() {
        updateText(isVisible());
    }
}
