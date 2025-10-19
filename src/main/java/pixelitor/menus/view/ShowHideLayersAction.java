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
package pixelitor.menus.view;

import pixelitor.gui.WorkSpace;

import javax.swing.*;

/**
 * The {@link Action} that toggles the visibility of the layers.
 */
public class ShowHideLayersAction extends ShowHideAction {
    public ShowHideLayersAction(WorkSpace workSpace) {
        super("show_layers", "hide_layers", workSpace);
    }

    @Override
    public boolean isVisible() {
        return workSpace.areLayersVisible();
    }

    @Override
    public void setVisibility(boolean value) {
        workSpace.setLayersVisible(value, true);
    }
}
