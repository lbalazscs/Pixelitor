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

package pixelitor;

import pixelitor.gui.View;
import pixelitor.layers.ActiveCompositionListener;
import pixelitor.layers.ActiveMaskListener;
import pixelitor.layers.Layer;
import pixelitor.layers.MaskViewMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Static methods related to layer listeners
 */
public class Layers {
    // Listeners observing (the active layer of) the active composition
    private static final List<ActiveCompositionListener> compListeners = new ArrayList<>();
    private static final List<ActiveMaskListener> maskListeners = new ArrayList<>();

    private Layers() {
    }

    public static void addCompositionListener(ActiveCompositionListener listener) {
        compListeners.add(listener);
    }

    public static void addMaskListener(ActiveMaskListener listener) {
        maskListeners.add(listener);
    }

    public static void maskAddedTo(Layer layer) {
        for (var listener : maskListeners) {
            listener.maskAddedTo(layer);
        }
    }

    public static void maskDeletedFrom(Layer layer) {
        for (var listener : maskListeners) {
            listener.maskDeletedFrom(layer);
        }
    }

    // used for GUI updates
    public static void numLayersChanged(Composition comp, int newLayerCount) {
        for (var listener : compListeners) {
            listener.numLayersChanged(comp, newLayerCount);
        }
    }

    public static void activeCompChanged(Composition newComp, boolean viewChanged) {
        layerActivated(newComp.getActiveLayer(), viewChanged);
    }

    public static void layerActivated(Layer newActiveLayer, boolean viewChanged) {
        assert newActiveLayer != null;
        assert newActiveLayer.isActive();
        assert newActiveLayer.getComp().isActive();

        for (var listener : compListeners) {
            listener.layerActivated(newActiveLayer);
        }

        View view = newActiveLayer.getComp().getView();
        if (view == null) {
            // can happen when adding a new image:
            // the active layer changes, but there is no view yet
            return;
        }
        if (!viewChanged) {
            // go to normal mask-viewing mode on the activated layer,
            // except if we got here because of a view change
            MaskViewMode.NORMAL.activate(view, newActiveLayer);
        }
    }

    public static void layerOrderChanged(Composition comp) {
        for (var listener : compListeners) {
            listener.layerOrderChanged(comp);
        }
    }
}

