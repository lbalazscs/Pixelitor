/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.CompositionView;
import pixelitor.gui.OpenComps;
import pixelitor.utils.CompActivationListener;
import pixelitor.utils.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An Action that deletes the active layer from the active composition.
 */
public class DeleteActiveLayerAction extends AbstractAction
    implements CompActivationListener, GlobalLayerChangeListener {

    public static final DeleteActiveLayerAction INSTANCE = new DeleteActiveLayerAction();

    private DeleteActiveLayerAction() {
        super("Delete Layer", Icons.load("delete_layer.gif"));
        putValue(SHORT_DESCRIPTION, "Deletes the active layer.");
        setEnabled(false);
        OpenComps.addActivationListener(this);
        Layers.addLayerChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Composition comp = OpenComps.getActiveCompOrNull();
        comp.deleteActiveLayer(true, true);
    }

    @Override
    public void allCompsClosed() {
        setEnabled(false);
    }

    @Override
    public void compActivated(CompositionView oldIC, CompositionView newIC) {
        if (newIC.getComp().getNumLayers() <= 1) { // no more deletion is possible
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
