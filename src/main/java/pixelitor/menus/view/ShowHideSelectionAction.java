/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.selection.Selection;

/**
 * The action that either shows or hides the selection, depending on the current visibility
 */
public class ShowHideSelectionAction extends ShowHideAction {
    public ShowHideSelectionAction() {
        super("Show Selection Edges", "Hide Selection Edges");
    }

    @Override
    public boolean getCurrentVisibility() {
        ImageComponent ic = ImageComponents.getActiveIC();
        if (ic != null) {
            Selection selection = ic.getComp().getSelection();
            if (selection != null) {
                return !selection.isHidden();
            }
        }

        return true;
    }

    @Override
    public boolean getVisibilityAtStartUp() {
        return true;
    }

    @Override
    public void setVisibilityAction(boolean value) {
        ImageComponents.onActiveSelection(
                selection -> selection.setHidden(!value, true));
    }
}