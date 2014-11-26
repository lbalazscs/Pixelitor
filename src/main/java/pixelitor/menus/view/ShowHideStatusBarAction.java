/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.menus.view;

import pixelitor.PixelitorWindow;
import pixelitor.utils.AppPreferences;

/**
 *
 */
public class ShowHideStatusBarAction extends ShowHideAction {
    public ShowHideStatusBarAction() {
        super("Show Status Bar", "Hide Status Bar");
    }

    @Override
    public boolean getCurrentVisibility() {
        PixelitorWindow pixelitorWindow = PixelitorWindow.getInstance();
        return pixelitorWindow.isStatusBarShown();
    }

    @Override
    public boolean getVisibilityAtStartUp() {
        return AppPreferences.WorkSpace.getStatusBarVisibility();
    }

    @Override
    public void setVisibilityAction(boolean value) {
        AppPreferences.WorkSpace.setStatusBarVisibility(value);
    }
}
