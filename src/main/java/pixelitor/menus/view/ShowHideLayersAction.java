/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.LayersContainer;

/**
 * The action that either shows or hides the layers,
 * depending on the current visibility
 */
public class ShowHideLayersAction extends ShowHideAction {
    public static final ShowHideAction INSTANCE = new ShowHideLayersAction();

    private ShowHideLayersAction() {
        super("Show Layers", "Hide Layers");
    }

    @Override
    public boolean getCurrentVisibility() {
        return LayersContainer.areLayersShown();
    }

    @Override
    public boolean getStartupVisibility() {
        return WorkSpace.getLayersVisibility();
    }

    @Override
    public void setVisibility(boolean value) {
        WorkSpace.setLayersVisibility(value, true);
    }
}