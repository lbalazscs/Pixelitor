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
import pixelitor.ConsistencyChecks;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.utils.IconUtils;
import pixelitor.utils.ImageSwitchListener;
import pixelitor.utils.UpdateGUI;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An Action that deletes the active layer
 */
public class DeleteActiveLayerAction extends AbstractAction implements ImageSwitchListener, GlobalLayerChangeListener {
    public static final DeleteActiveLayerAction INSTANCE = new DeleteActiveLayerAction();

    private DeleteActiveLayerAction() {
        super("Delete Layer", IconUtils.loadIcon("delete_layer.gif"));
        putValue(Action.SHORT_DESCRIPTION, "Deletes the active layer.");
        setEnabled(false);
        ImageComponents.addImageSwitchListener(this);
        AppLogic.addLayerChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Composition comp = ImageComponents.getActiveCompOrNull();
        comp.deleteActiveLayer(UpdateGUI.YES, AddToHistory.YES);
    }

    @Override
    public void noOpenImageAnymore() {
        setEnabled(false);
    }

    @Override
    public void newImageOpened(Composition comp) {
        int nrLayers = comp.getNrLayers();
        if (nrLayers <= 1) {
            setEnabled(false);
        } else {
            setEnabled(true);
        }
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        if (newIC.getComp().getNrLayers() <= 1) { // no more deletion is possible
            setEnabled(false);
        } else {
            setEnabled(true);
        }
    }

    @Override
    public void activeCompLayerCountChanged(Composition comp, int newLayerCount) {
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

        assert ConsistencyChecks.layerDeleteActionEnabledCheck();
    }
}
