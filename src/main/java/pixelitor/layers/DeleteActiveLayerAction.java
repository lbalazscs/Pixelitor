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

import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.Layers;
import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.gui.utils.NamedAction;
import pixelitor.utils.Icons;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An {@link Action} that deletes the active layer from the active composition.
 */
public class DeleteActiveLayerAction extends NamedAction
    implements ViewActivationListener, ActiveCompositionListener {

    public static final DeleteActiveLayerAction INSTANCE = new DeleteActiveLayerAction();

    private DeleteActiveLayerAction() {
        super("Delete Layer", Icons.load("delete_layer.gif"));
        setToolTip("Deletes the active layer.");
        setEnabled(false);
        OpenImages.addActivationListener(this);
        Layers.addCompositionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var comp = OpenImages.getActiveComp();
        comp.deleteActiveLayer(true);
    }

    @Override
    public void allViewsClosed() {
        setEnabled(false);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        if (newView.getComp().getNumLayers() <= 1) { // no more deletion is possible
            setEnabled(false);
        } else {
            setEnabled(true);
        }
    }

    @Override
    public void numLayersChanged(Composition comp, int newLayerCount) {
        if (newLayerCount <= 1) {
            setEnabled(false);
        } else {
            setEnabled(true);
        }
    }

    @Override
    public void activeLayerChanged(Layer newActiveLayer) {

    }

    @Override
    public void layerOrderChanged(Composition comp) {

    }

    @Override
    public void setEnabled(boolean newValue) {
        super.setEnabled(newValue);

        assert ConsistencyChecks.layerDeleteActionEnabled();
    }
}
