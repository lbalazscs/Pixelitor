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

package pixelitor.layers;

import pixelitor.AppLogic;
import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.utils.IconUtils;
import pixelitor.utils.ImageSwitchListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An Action that adds a new layer mask.
 */
public class AddLayerMaskAction extends AbstractAction implements ImageSwitchListener, GlobalLayerMaskChangeListener, GlobalLayerChangeListener {
    public static final AddLayerMaskAction INSTANCE = new AddLayerMaskAction();

    private AddLayerMaskAction() {
        super("Add Layer Mask", IconUtils.loadIcon("add_layer_mask.png"));
        putValue(Action.SHORT_DESCRIPTION, "<html>Adds a layer mask to the active layer. <br>Ctrl-click to add an inverted layer mask.");
        setEnabled(false);
        ImageComponents.addImageSwitchListener(this);
        AppLogic.addLayerChangeListener(this);
        AppLogic.addLayerMaskChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Composition comp = ImageComponents.getActiveCompOrNull();
        Layer layer = comp.getActiveLayer();
        assert !layer.hasMask();
        boolean ctrlPressed = false;
        if(e != null) { // could be null in tests
            ctrlPressed = ((e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK);
        }

        if (comp.hasSelection()) {
            if (ctrlPressed) {
                layer.addMask(LayerMaskAddType.HIDE_SELECTION);
            } else {
                layer.addMask(LayerMaskAddType.REVEAL_SELECTION);
            }
        } else { // there is no selection
            if (ctrlPressed) {
                layer.addMask(LayerMaskAddType.HIDE_ALL);
            } else {
                layer.addMask(LayerMaskAddType.REVEAL_ALL);
            }
        }
    }

    @Override
    public void noOpenImageAnymore() {
        setEnabled(false);
    }

    @Override
    public void newImageOpened(Composition comp) {
        setEnabled(!comp.getActiveLayer().hasMask());
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        boolean hasMask = newIC.getComp().getActiveLayer().hasMask();
        setEnabled(!hasMask);
    }

    @Override
    public void maskAddedOrDeleted(Layer affectedLayer) {
        setEnabled(!affectedLayer.hasMask());
    }

    @Override
    public void activeCompLayerCountChanged(Composition comp, int newLayerCount) {
    }

    @Override
    public void activeLayerChanged(Layer newActiveLayer) {
        setEnabled(!newActiveLayer.hasMask());
    }

    @Override
    public void layerOrderChanged(Composition comp) {
    }
}