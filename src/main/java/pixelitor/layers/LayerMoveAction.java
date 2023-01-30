/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.OpenViewEnabledAction;
import pixelitor.utils.Icons;

import javax.swing.*;

import static pixelitor.utils.Texts.i18n;

/**
 * An {@link Action} that moves the active layer of the active composition
 * up or down in the layer stack
 */
public class LayerMoveAction extends OpenViewEnabledAction.Checked
    implements ActiveHolderListener {

    // menu and history names (also for selection movements)
    public static final String RAISE_LAYER = i18n("raise_layer");
    public static final String LOWER_LAYER = i18n("lower_layer");
    public static final String LAYER_TO_TOP = i18n("layer_to_top");
    public static final String LAYER_TO_BOTTOM = i18n("layer_to_bottom");
    public static final String LOWER_LAYER_SELECTION = i18n("lower_layer_selection");
    public static final String RAISE_LAYER_SELECTION = i18n("raise_layer_selection");

    public static final LayerMoveAction MOVE_LAYER_UP = new LayerMoveAction(true);
    public static final LayerMoveAction MOVE_LAYER_DOWN = new LayerMoveAction(false);

    private final boolean up;

    private LayerMoveAction(boolean up) {
        super(getName(up), getIcon(up));
        this.up = up;
        setToolTip(up ? i18n("raise_layer_tt") : i18n("lower_layer_tt"));
        setEnabled(false);
        Layers.addHolderListener(this);
    }

    @Override
    protected void onClick(Composition comp) {
        LayerHolder holder = comp.getActiveHolder();
        if (up) {
            holder.moveActiveLayerUp();
        } else {
            holder.moveActiveLayerDown();
        }
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        enableDisable(newView.getComp().getActiveHolder());
    }

    public void enableDisable(LayerHolder holder) {
        if (holder != null) {
            int activeLayerIndex = holder.getActiveLayerIndex();
            if (up) {
                int numLayers = holder.getNumLayers();
                setEnabled(activeLayerIndex < numLayers - 1);
            } else {
                setEnabled(activeLayerIndex > 0);
            }
        }
    }

    @Override
    public void numLayersChanged(LayerHolder holder, int newLayerCount) {
        enableDisable(holder);
    }

    @Override
    public void layerActivated(Layer layer) {
        enableDisable(layer.getHolder());
    }

    @Override
    public void layersReordered(LayerHolder holder) {
        enableDisable(holder);
    }

    private static Icon getIcon(boolean up) {
        return up ? Icons.getNorthArrowIcon() : Icons.getSouthArrowIcon();
    }

    private static String getName(boolean up) {
        return up ? RAISE_LAYER : LOWER_LAYER;
    }
}
