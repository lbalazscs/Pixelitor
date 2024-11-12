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

package pixelitor.menus.view;

import pixelitor.gui.StatusBar;
import pixelitor.gui.WorkSpace;

import javax.swing.*;

/**
 * The {@link Action} that toggles the visibility of the status bar.
 */
public class ShowHideStatusBarAction extends ShowHideAction {
    public ShowHideStatusBarAction(WorkSpace workSpace) {
        super("Show Status Bar", "Hide Status Bar", workSpace);
    }

    @Override
    public boolean getCurrentVisibility() {
        return StatusBar.isShown();
    }

    @Override
    public boolean getStartupVisibility() {
        return workSpace.isStatusBarVisible();
    }

    @Override
    public void setVisibility(boolean value) {
        workSpace.setStatusBarVisible(value, true);
    }
}
