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

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.selection.Selection;

import javax.swing.*;

/**
 * The {@link Action} that toggles the visibility of the selection.
 */
public class ShowHideSelectionAction extends ShowHideAction {
    public ShowHideSelectionAction() {
        super("show_sel_edges", "hide_sel_edges", null);
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
                selection.setHidden(!value);
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