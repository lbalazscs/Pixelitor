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
import pixelitor.gui.WorkSpace;

import java.awt.event.ActionEvent;

/**
 * An Action that toggles the visibility of any toggleable {@link AppPanel}.
 */
public class ShowHidePanelAction extends ShowHideAction {
    private final AppPanel panel;
    private final WorkSpace workSpace;

    public ShowHidePanelAction(AppPanel panel, WorkSpace workSpace) {
        super(panel.getShowActionKey(), panel.getHideActionKey(), workSpace.isVisible(panel));
        this.panel = panel;
        this.workSpace = workSpace;
    }

    @Override
    public boolean isVisible() {
        return workSpace.isVisible(panel);
    }

    @Override
    public void setVisibility(boolean value) {
        workSpace.setPanelVisible(panel, value, true);
    }

    @Override
    protected void onClick(ActionEvent e) {
        workSpace.setPanelVisible(panel, !isVisible(), true);
    }

    public AppPanel getPanel() {
        return panel;
    }
}
