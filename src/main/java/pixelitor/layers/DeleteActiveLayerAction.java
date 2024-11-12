/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.ConsistencyChecks;
import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.gui.utils.PAction;
import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.utils.Icons;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;

import static pixelitor.utils.Texts.i18n;

/**
 * An {@link Action} that deletes the active layer from the active composition.
 */
public class DeleteActiveLayerAction extends PAction
    implements ViewActivationListener, ActiveHolderListener {

    public static final DeleteActiveLayerAction INSTANCE = new DeleteActiveLayerAction();

    private DeleteActiveLayerAction() {
        super(i18n("delete_layer"),
            Icons.loadThemed("delete_layer.gif", ThemedImageIcon.RED),
            () -> Views.getActiveComp().deleteActiveLayer(true));

        setToolTip("Deletes the active layer.");
        setEnabled(false);
        Views.addActivationListener(this);
        Layers.addHolderListener(this);
    }

    @Override
    public void allViewsClosed() {
        setEnabled(false);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        LayerHolder holder = newView.getComp().getActiveHolder();
        enableDisable(holder, holder.getNumLayers());
    }

    @Override
    public void numLayersChanged(LayerHolder holder, int newLayerCount) {
        enableDisable(holder, newLayerCount);
    }

    @Override
    public void layerActivated(Layer layer) {
        LayerHolder holder = layer.getHolder();
        enableDisable(holder, holder.getNumLayers());
    }

    @Override
    public void layersReordered(LayerHolder holder) {

    }

    private void enableDisable(LayerHolder holder, int layerCount) {
        if (holder.canBeEmpty()) {
            setEnabled(layerCount > 0);
        } else {
            setEnabled(layerCount > 1);
        }
    }

    @Override
    public void setEnabled(boolean newValue) {
        super.setEnabled(newValue);

        assert ConsistencyChecks.layerDeleteActionEnabled();
    }
}
