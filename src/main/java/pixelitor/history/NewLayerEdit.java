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

package pixelitor.history;

import pixelitor.layers.Layer;
import pixelitor.layers.LayerHolder;
import pixelitor.layers.MaskViewMode;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the creation of a new layer
 */
public class NewLayerEdit extends PixelitorEdit {
    private LayerHolder holder;
    private Layer previousActiveLayer;
    private Layer layer;
    private final int layerIndex;
    private final MaskViewMode previousMaskViewMode;

    public NewLayerEdit(String editName,
                        Layer layer, Layer previousActiveLayer,
                        MaskViewMode previousMaskViewMode) {
        super(editName, layer.getComp());

        assert previousActiveLayer != layer;

        this.holder = layer.getHolder();
        this.previousActiveLayer = previousActiveLayer;
        this.previousMaskViewMode = previousMaskViewMode;
        this.layer = layer;
        layerIndex = this.holder.indexOf(layer);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        holder.deleteLayer(layer, false);
        comp.setActiveLayer(previousActiveLayer);
        previousMaskViewMode.activate(comp, previousActiveLayer);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        holder.adder()
            .atIndex(layerIndex)
            .add(layer);
    }

    @Override
    public void die() {
        super.die();

        layer = null;
        previousActiveLayer = null;
        holder = null;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(holder.createDebugNode("holder"));
        node.add(previousActiveLayer.createDebugNode("previous active layer"));
        node.add(layer.createDebugNode("layer"));
        node.addInt("layer index", layerIndex);
        node.addAsString("previous mask view mode", previousMaskViewMode);

        return node;
    }
}
