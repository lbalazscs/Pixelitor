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

package pixelitor.history;

import pixelitor.Composition;
import pixelitor.Composition.LayerAdder;
import pixelitor.layers.Layer;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.LayerNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the deletion of a layer
 */
public class DeleteLayerEdit extends PixelitorEdit {
    private Layer layer;
    private final int layerIndex;

    public DeleteLayerEdit(Composition comp, Layer layer, int layerIndex) {
        super("Delete Layer", comp, true);

        this.layer = layer;
        this.layerIndex = layerIndex;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        new LayerAdder(comp)
            .atIndex(layerIndex)
            .add(layer);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        comp.deleteLayer(layer, false);
    }

    @Override
    public void die() {
        super.die();

        layer = null;
    }

    @Override
    public DebugNode createDebugNode() {
        DebugNode node = super.createDebugNode();

        node.addInt("layer index", layerIndex);
        node.add(new LayerNode(layer));

        return node;
    }
}