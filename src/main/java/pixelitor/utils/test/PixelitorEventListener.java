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

package pixelitor.utils.test;

import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.Layers;
import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.layers.GlobalLayerChangeListener;
import pixelitor.layers.GlobalLayerMaskChangeListener;
import pixelitor.layers.Layer;
import pixelitor.utils.CompActivationListener;

import static java.lang.String.format;

/**
 * Used for tracking what happens in long-running automatic tests.
 * Listens to changes and generates events.
 */
public class PixelitorEventListener implements GlobalLayerChangeListener,
    GlobalLayerMaskChangeListener, CompActivationListener {

    public PixelitorEventListener() {
        if (Build.isFinal()) {
            throw new IllegalStateException("this should be only used for debugging");
        }
    }

    public void register() {
        Layers.addLayerChangeListener(this);
        Layers.addLayerMaskChangeListener(this);
        OpenImages.addActivationListener(this);
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
    public void allCompsClosed() {
        Events.postListenerEvent("allCompsClosed", null, null);
    }

    @Override
    public void compActivated(View oldView, View newView) {
        String oldCVName = oldView == null ? "null" : oldView.getName();
        Events.postListenerEvent(format("compActivated %s => %s",
            oldCVName, newView.getName()),
            newView.getComp(), null);
    }
}
