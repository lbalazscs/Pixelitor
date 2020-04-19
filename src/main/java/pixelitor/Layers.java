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

package pixelitor;

import pixelitor.gui.View;
import pixelitor.layers.GlobalLayerChangeListener;
import pixelitor.layers.GlobalLayerMaskChangeListener;
import pixelitor.layers.Layer;
import pixelitor.layers.MaskViewMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Static methods related to layer listeners
 */
public class Layers {
    /**
     * Global listeners which always act on the active layer of the active composition
     */
    private static final List<GlobalLayerChangeListener> layerChangeListeners = new ArrayList<>();
    private static final List<GlobalLayerMaskChangeListener> layerMaskChangeListeners = new ArrayList<>();

    private Layers() {
    }

    public static void addLayerChangeListener(GlobalLayerChangeListener listener) {
        layerChangeListeners.add(listener);
    }

    public static void addLayerMaskChangeListener(GlobalLayerMaskChangeListener listener) {
        layerMaskChangeListeners.add(listener);
    }

    public static void maskAddedTo(Layer layer) {
        for (var listener : layerMaskChangeListeners) {
            listener.maskAddedTo(layer);
        }
    }

    public static void maskDeletedFrom(Layer layer) {
        for (var listener : layerMaskChangeListeners) {
            listener.maskDeletedFrom(layer);
        }
    }

    // used for GUI updates
    public static void numLayersChanged(Composition comp, int newLayerCount) {
        for (var listener : layerChangeListeners) {
            listener.numLayersChanged(comp, newLayerCount);
        }
    }

    public static void activeLayerChanged(Layer newActiveLayer, boolean viewChanged) {
        assert newActiveLayer != null;
        for (var listener : layerChangeListeners) {
            listener.activeLayerChanged(newActiveLayer);
        }

        View view = newActiveLayer.getComp().getView();
        if (view == null) {
            // can happen when adding a new image:
            // the active layer changes, but there is no view yet
            return;
        }
        if(!viewChanged) {
            // go to normal mask-viewing mode on the activated layer,
            // except if we got here because of a view change
            MaskViewMode.NORMAL.activate(view, newActiveLayer, "active layer changed");
        }
    }

    public static void layerOrderChanged(Composition comp) {
        for (var listener : layerChangeListeners) {
            listener.layerOrderChanged(comp);
        }
    }
}

