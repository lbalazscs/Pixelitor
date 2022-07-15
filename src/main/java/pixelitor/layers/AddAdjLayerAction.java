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

package pixelitor.layers;

import pixelitor.Composition.LayerAdder;
import pixelitor.Views;
import pixelitor.filters.Filter;
import pixelitor.filters.GradientMap;
import pixelitor.gui.View;
import pixelitor.gui.utils.PAction;
import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.utils.Icons;
import pixelitor.utils.ViewActivationListener;

/**
 * An Action that adds a new adjustment layer to the active composition.
 */
public class AddAdjLayerAction extends PAction implements ViewActivationListener {
    public static final AddAdjLayerAction INSTANCE = new AddAdjLayerAction();

    private AddAdjLayerAction() {
        super("Add Adjustment Layer",
            Icons.loadThemed("add_adj_layer.png", ThemedImageIcon.GREEN));
        setToolTip("Adds a new adjustment layer.");
        setEnabled(false);
        Views.addActivationListener(this);
    }

    @Override
    protected void onClick() {
        var comp = Views.getActiveComp();
        Filter filter = new GradientMap();
        filter.setName(GradientMap.NAME);
        var adjustmentLayer = new AdjustmentLayer(comp, GradientMap.NAME, filter);

        new LayerAdder(comp)
            .withHistory("New Adjustment Layer")
            .add(adjustmentLayer);
    }

    @Override
    public void allViewsClosed() {
        setEnabled(false);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        setEnabled(true);
    }
}