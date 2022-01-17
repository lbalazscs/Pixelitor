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

package pixelitor.menus.view;

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.selection.Selection;

/**
 * The action that either shows or hides the selection,
 * depending on the current visibility
 */
public class ShowHideSelectionAction extends ShowHideAction {
    public ShowHideSelectionAction() {
        super("Show Selection Edges", "Hide Selection Edges");
    }

    @Override
    public boolean getCurrentVisibility() {
        var selection = Views.getActiveSelection();
        if (selection != null) {
            return !selection.isHidden();
        }

        return true;
    }

    @Override
    public boolean getStartupVisibility() {
        return true;
    }

    @Override
    public void setVisibility(boolean value) {
        Composition comp = Views.getActiveComp();
        if (comp != null) {
            Selection selection = comp.getSelection();
            if (selection != null) {
                selection.setHidden(!value, true);
            }
        }
    }

    public void updateTextFrom(Selection selection) {
        if (selection == null) {
            setHideText();
        } else {
            updateText(!selection.isHidden());
        }
    }
}