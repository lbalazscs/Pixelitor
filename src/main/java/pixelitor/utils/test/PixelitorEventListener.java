/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils.test;

import pixelitor.AppLogic;
import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.layers.GlobalLayerChangeListener;
import pixelitor.layers.GlobalLayerMaskChangeListener;
import pixelitor.layers.Layer;
import pixelitor.utils.ActiveImageChangeListener;

/**
 * This class is used for tracking what happens
 * in long-running automatic tests.
 * It listens to changes and generates events
 */
public class PixelitorEventListener implements GlobalLayerChangeListener,
        GlobalLayerMaskChangeListener, ActiveImageChangeListener {

    public PixelitorEventListener() {
        if (Build.CURRENT.isFinal()) {
            throw new IllegalStateException("this should be only used for debugging");
        }
    }

    public void register() {
        AppLogic.addLayerChangeListener(this);
        AppLogic.addLayerMaskChangeListener(this);
        ImageComponents.addActiveImageChangeListener(this);
    }

    @Override
    public void numLayersChanged(Composition comp, int newLayerCount) {
        Events.postListenerEvent("activeCompLayerCountChanged, newCount = " + newLayerCount, comp, null);
    }

    @Override
    public void activeLayerChanged(Layer newActiveLayer) {
        Events.postListenerEvent("activeLayerChanged to "
                        + newActiveLayer.getName(),
                newActiveLayer.getComp(), newActiveLayer);
    }

    @Override
    public void layerOrderChanged(Composition comp) {
        Events.postListenerEvent("layerOrderChanged", comp, null);
    }

    @Override
    public void maskAddedTo(Layer layer) {
        Events.postListenerEvent("maskAdded", layer.getComp(), layer);
    }

    @Override
    public void maskDeletedFrom(Layer layer) {
        Events.postListenerEvent("maskDeleted", layer.getComp(), layer);
    }

    @Override
    public void noOpenImageAnymore() {
        Events.postListenerEvent("noOpenImageAnymore", null, null);
    }

    @Override
    public void newImageOpened(Composition comp) {
        Events.postListenerEvent("newImageOpened", comp, null);
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        Events.postListenerEvent(String.format("activeImageHasChanged %s => %s",
                        oldIC.getName(), newIC.getName()),
                newIC.getComp(), null);
    }
}
