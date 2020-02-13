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
import pixelitor.Layers;
import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.utils.Icons;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static java.awt.event.ActionEvent.CTRL_MASK;
import static pixelitor.layers.LayerMaskAddType.HIDE_ALL;
import static pixelitor.layers.LayerMaskAddType.HIDE_SELECTION;
import static pixelitor.layers.LayerMaskAddType.REVEAL_ALL;
import static pixelitor.layers.LayerMaskAddType.REVEAL_SELECTION;

/**
 * An Action that adds a new layer mask
 * to the active layer of the active composition.
 */
public class AddLayerMaskAction extends AbstractAction
        implements ViewActivationListener, GlobalLayerMaskChangeListener, GlobalLayerChangeListener {

    public static final AddLayerMaskAction INSTANCE = new AddLayerMaskAction();

    private AddLayerMaskAction() {
        super("Add Layer Mask", Icons.load("add_layer_mask.png"));
        putValue(Action.SHORT_DESCRIPTION,
                "<html>Adds a layer mask to the active layer. " +
                        "<br><b>Ctrl-click</b> to add an inverted layer mask.");
        setEnabled(false);
        OpenImages.addActivationListener(this);
        Layers.addLayerChangeListener(this);
        Layers.addLayerMaskChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var comp = OpenImages.getActiveComp();
        var layer = comp.getActiveLayer();
        assert !layer.hasMask();
        boolean ctrlPressed = false;
        if (e != null) { // could be null in tests
            ctrlPressed = (e.getModifiers() & CTRL_MASK) == CTRL_MASK;
        }

        if (comp.hasSelection()) {
            if (ctrlPressed) {
                layer.addMask(HIDE_SELECTION);
            } else {
                layer.addMask(REVEAL_SELECTION);
            }
        } else { // there is no selection
            if (ctrlPressed) {
                layer.addMask(HIDE_ALL);
            } else {
                layer.addMask(REVEAL_ALL);
            }
        }
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
    public void numLayersChanged(Composition comp, int newLayerCount) {
    }

    @Override
    public void activeLayerChanged(Layer newActiveLayer) {
        setEnabled(!newActiveLayer.hasMask());
    }

    @Override
    public void layerOrderChanged(Composition comp) {
    }
}