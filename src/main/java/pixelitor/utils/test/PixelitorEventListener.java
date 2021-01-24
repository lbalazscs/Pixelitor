/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.Layers;
import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.layers.ActiveCompositionListener;
import pixelitor.layers.ActiveMaskListener;
import pixelitor.layers.Layer;
import pixelitor.utils.ViewActivationListener;

import static java.lang.String.format;

/**
 * Used for tracking what happens in long-running automatic tests.
 * Listens to changes and generates events.
 */
public class PixelitorEventListener implements ActiveCompositionListener,
    ActiveMaskListener, ViewActivationListener {

    public PixelitorEventListener() {
        if (AppContext.isFinal()) {
            throw new IllegalStateException("should be used only for debugging");
        }
    }

    public void register() {
        Layers.addCompositionListener(this);
        Layers.addMaskListener(this);
        OpenImages.addActivationListener(this);
    }

    @Override
    public void numLayersChanged(Composition comp, int newLayerCount) {
        String type = "#layers changed, newCount = " + newLayerCount;
        Events.postListenerEvent(type, comp, null);
    }

    @Override
    public void layerActivated(Layer newActiveLayer) {
        String type = "layer activated: " + newActiveLayer.getName();
        Events.postListenerEvent(type, newActiveLayer.getComp(), newActiveLayer);
    }

    @Override
    public void layerOrderChanged(Composition comp) {
        Events.postListenerEvent("layer order changed", comp, null);
    }

    @Override
    public void maskAddedTo(Layer layer) {
        Events.postListenerEvent("mask added", layer.getComp(), layer);
    }

    @Override
    public void maskDeletedFrom(Layer layer) {
        Events.postListenerEvent("mask deleted", layer.getComp(), layer);
    }

    @Override
    public void allViewsClosed() {
        Events.postListenerEvent("all views closed", null, null);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        String oldCVName = oldView == null ? "null" : oldView.getName();
        String type = format("view activated %s => %s", oldCVName, newView.getName());
        Events.postListenerEvent(type, newView.getComp(), null);
    }
}
