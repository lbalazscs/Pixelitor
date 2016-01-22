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

package pixelitor.utils.test;

import pixelitor.AppLogic;
import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.layers.GlobalLayerChangeListener;
import pixelitor.layers.GlobalLayerMaskChangeListener;
import pixelitor.layers.Layer;
import pixelitor.utils.ImageSwitchListener;

/**
 * Listens to changes and generates events
 */
public class PixelitorEventListener implements GlobalLayerChangeListener,
        GlobalLayerMaskChangeListener, ImageSwitchListener {

    public PixelitorEventListener() {
        if (Build.CURRENT == Build.FINAL) {
            throw new IllegalStateException("this should be used for debugging");
        }
    }

    public void register() {
        AppLogic.addLayerChangeListener(this);
        AppLogic.addLayerMaskChangeListener(this);
        ImageComponents.addImageSwitchListener(this);
    }

    @Override
    public void activeCompLayerCountChanged(Composition comp, int newLayerCount) {
        Events.postListenerEvent("activeCompLayerCountChanged, newCount = " + newLayerCount, comp, null);
    }

    @Override
    public void activeLayerChanged(Layer newActiveLayer) {
        Events.postListenerEvent("activeLayerChanged", newActiveLayer.getComp(), newActiveLayer);
    }

    @Override
    public void layerOrderChanged(Composition comp) {
        Events.postListenerEvent("layerOrderChanged", comp, null);
    }

    @Override
    public void maskAddedOrDeleted(Layer affectedLayer) {
        Events.postListenerEvent("maskAddedOrDeleted", affectedLayer.getComp(), affectedLayer);
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
        Events.postListenerEvent("activeImageHasChanged", newIC.getComp(), null);
    }
}
