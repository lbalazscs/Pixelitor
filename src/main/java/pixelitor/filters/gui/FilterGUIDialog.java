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

package pixelitor.filters.gui;

import pixelitor.filters.Filter;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.layers.Drawable;

/**
 * A dialog for the filter adjustments
 */
public class FilterGUIDialog extends OKCancelDialog {
    private final Filter activeFilter;
    private final Drawable dr;

    private FilterGUIDialog(FilterGUI filterGUI, Filter activeFilter, Drawable dr) {
        super(filterGUI, activeFilter.getName());
        this.activeFilter = activeFilter;
        this.dr = dr;
        setName("filterDialog");
    }

    public static void showDialog(FilterGUI filterGUI, Filter activeFilter, Drawable dr) {
        FilterGUIDialog dialog = new FilterGUIDialog(filterGUI, activeFilter, dr);
        dialog.setVisible(true);
    }

    @Override
    public void okAction() {
        dr.onDialogAccepted(activeFilter.getName());

        close();
    }

    @Override
    public void cancelAction() {
        dr.onDialogCanceled();

        close();
    }
}
