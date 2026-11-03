/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.history;

import pixelitor.layers.Layer;
import pixelitor.layers.LayerHolder;
import pixelitor.layers.MaskViewMode;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Represents the addition of a new {@link Layer} to a {@link LayerHolder}.
 */
public class NewLayerEdit extends PixelitorEdit {
    private final LayerHolder holder;
    private final Layer prevActiveLayer;
    private final Layer layer;
    private final int layerIndex;
    private final MaskViewMode prevMaskViewMode;

    public NewLayerEdit(String name,
                        Layer layer, Layer prevActiveLayer,
                        MaskViewMode prevMaskViewMode) {
        super(name, layer.getComp());

        assert prevActiveLayer != null;
        assert prevActiveLayer != layer;

        this.holder = layer.getHolder();
        this.prevActiveLayer = prevActiveLayer;
        this.prevMaskViewMode = prevMaskViewMode;
        this.layer = layer;
        layerIndex = this.holder.indexOf(layer);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        // Activate first, so that deleteLayer doesn't have to pick
        // (and fire events for) an arbitrary neighbor. This is safe,
        // because prevActiveLayer is never nested in the new layer,
        // so deleteLayer won't change the active layer afterwards.
        comp.setActiveLayer(prevActiveLayer);
        holder.deleteLayer(layer, false);
        // the activation above reset the mask view mode, so restore it last
        comp.setMaskViewMode(prevMaskViewMode, prevActiveLayer);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        holder.adder()
            .atIndex(layerIndex)
            .add(layer);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(holder.createDebugNode("holder"));
        node.add(prevActiveLayer.createDebugNode("previous active layer"));
        node.add(layer.createDebugNode("layer"));
        node.addInt("layer index", layerIndex);
        node.addAsString("previous mask view mode", prevMaskViewMode);

        return node;
    }
}
