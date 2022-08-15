/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
 * Static methods related to layer listeners
 */
public class Layers {
    private static final List<ActiveLayerHolderListener> lhListeners = new ArrayList<>();
    private static final List<ActiveMaskListener> maskListeners = new ArrayList<>();

    private Layers() {
    }

    public static void addLayerHolderListener(ActiveLayerHolderListener listener) {
        lhListeners.add(listener);
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
    public static void numLayersChanged(LayerHolder layerHolder, int newLayerCount) {
        for (var listener : lhListeners) {
            listener.numLayersChanged(layerHolder, newLayerCount);
        }
    }

    public static void activeCompChanged(Composition newComp, boolean viewChanged) {
        layerTargeted(newComp.getEditingTarget(), viewChanged);
    }

    public static void layerTargeted(Layer newEditingTarget, boolean viewChanged) {
        assert newEditingTarget != null;
        assert newEditingTarget.isEditingTarget();
        assert newEditingTarget.getComp().isActive();

        for (var listener : lhListeners) {
            listener.layerTargeted(newEditingTarget);
        }

        View view = newEditingTarget.getComp().getView();
        if (view == null) {
            // can happen when adding a new image:
            // the active layer changes, but there is no view yet
            return;
        }
        if (!viewChanged) {
            // go to normal mask-viewing mode on the activated layer,
            // except if we got here because of a view change
            MaskViewMode.NORMAL.activate(view, newEditingTarget);
        }
    }

    public static void layerOrderChanged(LayerHolder layerHolder) {
        for (var listener : lhListeners) {
            listener.layerOrderChanged(layerHolder);
        }
    }
}

