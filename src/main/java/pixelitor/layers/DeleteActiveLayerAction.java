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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.gui.View;
import pixelitor.gui.utils.AbstractViewEnabledAction;
import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.utils.Icons;

import javax.swing.*;

import static pixelitor.utils.Texts.i18n;

/**
 * An {@link Action} that deletes the active layer from the active composition.
 */
public class DeleteActiveLayerAction extends AbstractViewEnabledAction
    implements ActiveHolderListener {

    public static final DeleteActiveLayerAction INSTANCE = new DeleteActiveLayerAction();

    private DeleteActiveLayerAction() {
        super(i18n("delete_layer"),
            Icons.loadThemed("delete_layer.gif", ThemedImageIcon.RED));

        setToolTip("Deletes the active layer.");
        LayerEvents.addHolderListener(this);
    }

    @Override
    protected void onClick(Composition comp) {
        comp.deleteActiveLayer(true);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        LayerHolder holder = newView.getComp().getActiveHolder();
        enableDisable(holder, holder.getNumLayers());
    }

    @Override
    public void layerCountChanged(LayerHolder holder, int newLayerCount) {
        enableDisable(holder, newLayerCount);
    }

    @Override
    public void layerActivated(Layer layer) {
        LayerHolder holder = layer.getHolder();
        enableDisable(holder, holder.getNumLayers());
    }

    private void enableDisable(LayerHolder holder, int layerCount) {
        boolean shouldBeEnabled = holder.canBeEmpty()
            ? layerCount > 0
            : layerCount > 1;
        setEnabled(shouldBeEnabled);
    }

    @Override
    public void setEnabled(boolean newValue) {
        super.setEnabled(newValue);

        assert ConsistencyChecks.layerDeleteActionEnabled();
    }
}
