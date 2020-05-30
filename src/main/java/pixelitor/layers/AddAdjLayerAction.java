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

package pixelitor.layers;

import pixelitor.Composition.LayerAdder;
import pixelitor.OpenImages;
import pixelitor.filters.Invert;
import pixelitor.gui.View;
import pixelitor.gui.utils.NamedAction;
import pixelitor.utils.Icons;
import pixelitor.utils.ViewActivationListener;

import java.awt.event.ActionEvent;

/**
 * An Action that adds a new adjustment layer to the active composition.
 */
public class AddAdjLayerAction extends NamedAction
    implements ViewActivationListener {

    public static final AddAdjLayerAction INSTANCE = new AddAdjLayerAction();

    private AddAdjLayerAction() {
        super("Add Adjustment Layer",
            Icons.load("add_adj_layer.png"));
        setToolTip("Adds a new adjustment layer.");
        setEnabled(false);
        OpenImages.addActivationListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var comp = OpenImages.getActiveComp();
        var adjustmentLayer = new AdjustmentLayer(comp, "Invert", new Invert());

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