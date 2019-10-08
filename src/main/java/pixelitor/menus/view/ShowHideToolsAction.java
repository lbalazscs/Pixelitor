/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.PixelitorWindow;
import pixelitor.utils.AppPreferences;

/**
 * The action that either shows or hides the tools,
 * depending on the current visibility
 */
public class ShowHideToolsAction extends ShowHideAction {
    public static final ShowHideAction INSTANCE = new ShowHideToolsAction();

    private ShowHideToolsAction() {
        super("Show Tools", "Hide Tools");
    }

    @Override
    public boolean getCurrentVisibility() {
        return PixelitorWindow.getInstance().areToolsShown();
    }

    @Override
    public boolean getVisibilityAtStartUp() {
        return AppPreferences.WorkSpace.getToolsVisibility();
    }

    @Override
    public void setVisibility(boolean value) {
        AppPreferences.WorkSpace.setToolsVisibility(value);
    }
}