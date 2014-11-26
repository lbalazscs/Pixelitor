/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.layers;

import pixelitor.AppLogic;
import pixelitor.Composition;
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.utils.IconUtils;
import pixelitor.utils.ImageSwitchListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An Action that moves tha active layer up in the layer stack
 */
public class LayerUpAction extends AbstractAction implements ImageSwitchListener, LayerChangeListener {
    public static final LayerUpAction INSTANCE = new LayerUpAction();

    private LayerUpAction() {
        super("Raise Layer", IconUtils.getNorthArrowIcon());
        setEnabled(false);
        ImageComponents.addImageSwitchListener(this);
        AppLogic.addLayerChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Composition comp = ImageComponents.getActiveComp();
        comp.moveActiveLayerUp();
    }

    @Override
    public void noOpenImageAnymore() {
        setEnabled(false);
    }

    @Override
    public void newImageOpened() {
        Composition comp = ImageComponents.getActiveComp();
        checkIndex(comp);
    }

    private void checkIndex(Composition comp) {
        if (comp != null) {
            int nrLayers = comp.getNrLayers();
            int activeLayerIndex = comp.getActiveLayerIndex();
            if (activeLayerIndex < nrLayers - 1) {
                setEnabled(true);
            } else {
                setEnabled(false);
            }
        }
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        checkIndex(newIC.getComp());
    }

    @Override
    public void activeCompLayerCountChanged(Composition comp, int newLayerCount) {
        checkIndex(comp);
    }

    @Override
    public void activeLayerChanged(Layer newActiveLayer) {
        Composition comp = newActiveLayer.getComposition();
        checkIndex(comp);
    }

    @Override
    public void layerOrderChanged(Composition comp) {
        checkIndex(comp);
    }
}