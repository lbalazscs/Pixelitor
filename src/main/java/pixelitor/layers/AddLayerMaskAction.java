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

import pixelitor.ConsistencyChecks;
import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.gui.utils.NamedAction;
import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.utils.Icons;
import pixelitor.utils.Messages;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static java.awt.event.ActionEvent.CTRL_MASK;

/**
 * An {@link Action} that adds a new layer mask
 * to the active layer of the active composition.
 */
public class AddLayerMaskAction extends NamedAction
    implements ViewActivationListener, ActiveMaskListener, ActiveHolderListener {

    public static final AddLayerMaskAction INSTANCE = new AddLayerMaskAction();

    private AddLayerMaskAction() {
        super("Add Layer Mask", Icons.loadThemed("add_layer_mask.png", ThemedImageIcon.GREEN));
        setToolTip("<html>Adds a layer mask to the active layer. " +
                   "<br><b>Ctrl-click</b> to add an inverted layer mask.");
        setEnabled(false);
        Views.addActivationListener(this);
        Layers.addHolderListener(this);
        Layers.addMaskListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean ctrlPressed = false;
        if (e != null) { // could be null in tests
            ctrlPressed = (e.getModifiers() & CTRL_MASK) == CTRL_MASK;
        }

        try {
            onClick(ctrlPressed);
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private void onClick(boolean ctrlPressed) {
        var comp = Views.getActiveComp();
        var layer = comp.getActiveLayer();
        assert !layer.hasMask();

        layer.addMask(ctrlPressed);
    }

    @Override
    public void allViewsClosed() {
        setEnabled(false);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        boolean hasMask = newView.getComp().getActiveLayer().hasMask();
        setEnabled(!hasMask);
    }

    @Override
    public void maskAddedTo(Layer layer) {
        assert layer.hasMask();
        setEnabled(false);
    }

    @Override
    public void maskDeletedFrom(Layer layer) {
        assert !layer.hasMask();
        setEnabled(true);
    }

    @Override
    public void numLayersChanged(LayerHolder layerHolder, int newLayerCount) {
    }

    @Override
    public void layerActivated(Layer newActiveLayer) {
        setEnabled(!newActiveLayer.hasMask());
    }

    @Override
    public void layerOrderChanged(LayerHolder layerHolder) {
    }

    @Override
    public void setEnabled(boolean newValue) {
        super.setEnabled(newValue);

        assert ConsistencyChecks.addMaskActionEnabled();
    }
}