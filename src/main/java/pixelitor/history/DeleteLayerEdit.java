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
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the deletion of a layer
 */
public class DeleteLayerEdit extends PixelitorEdit {
    private final LayerHolder holder;
    private final Layer layer;
    private final int layerIndex;

    public DeleteLayerEdit(LayerHolder holder, Layer layer, int layerIndex) {
        super("Delete " + layer.getName(), holder.getComp(), true);

        assert holder.getComp() == layer.getComp();

        this.holder = holder;
        this.layer = layer;
        this.layerIndex = layerIndex;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        holder.insertLayer(layer, layerIndex, true);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        holder.deleteLayer(layer, false);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addInt("layer index", layerIndex);
        node.add(layer.createDebugNode());

        return node;
    }
}