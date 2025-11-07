/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages and notifies global listeners about events related to the active composition's layers.
 */
public class LayerEvents {
    private static final List<ActiveHolderListener> holderListeners = new ArrayList<>();
    private static final List<ActiveMaskListener> maskListeners = new ArrayList<>();

    private LayerEvents() {
    }

    public static void addHolderListener(ActiveHolderListener listener) {
        holderListeners.add(listener);
    }

    public static void addMaskListener(ActiveMaskListener listener) {
        maskListeners.add(listener);
    }

    public static void fireMaskAdded(Layer layer) {
        for (var listener : maskListeners) {
            listener.maskAdded(layer);
        }
    }

    public static void fireMaskDeleted(Layer layer) {
        for (var listener : maskListeners) {
            listener.maskDeleted(layer);
        }
    }

    public static void fireLayerCountChanged(LayerHolder holder, int newLayerCount) {
        for (var listener : holderListeners) {
            listener.layerCountChanged(holder, newLayerCount);
        }
    }

    public static void fireActiveCompChanged(Composition newComp, boolean resetMaskViewMode) {
        fireLayerActivated(newComp.getActiveLayer(), resetMaskViewMode);
    }

    public static void fireLayerActivated(Layer layer, boolean resetMaskViewMode) {
        assert layer != null;
        assert layer.isActive();
        assert layer.getComp().isActive();

        for (var listener : holderListeners) {
            listener.layerActivated(layer);
        }

        View view = layer.getComp().getView();
        if (view == null) {
            // can happen when adding a new image:
            // the active layer changes, but there is no view yet
            return;
        }
        if (resetMaskViewMode) {
            view.setMaskViewMode(MaskViewMode.NORMAL, layer);
        }
    }
}
