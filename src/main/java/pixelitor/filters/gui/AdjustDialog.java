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

package pixelitor.filters.gui;

import pixelitor.filters.Filter;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.layers.ImageLayer;

/**
 * A dialog for the filter adjustments
 */
public class AdjustDialog extends OKCancelDialog {
    private final Filter activeFilter;
    private final ImageLayer layer;

    private AdjustDialog(AdjustPanel adjustPanel, Filter activeFilter, ImageLayer layer) {
        super(adjustPanel, activeFilter.getName());
        this.activeFilter = activeFilter;
        this.layer = layer;
        setName("filterDialog");
    }

    public static void showDialog(AdjustPanel adjustPanel, Filter activeFilter, ImageLayer layer) {
        AdjustDialog dialog = new AdjustDialog(adjustPanel, activeFilter, layer);
        dialog.setVisible(true);
    }

    @Override
    public void dialogAccepted() {
        layer.okPressedInDialog(activeFilter.getName());

        close();
    }

    @Override
    public void dialogCanceled() {
        layer.cancelPressedInDialog();

        close();
    }
}
