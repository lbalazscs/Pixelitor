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

package pixelitor.history;

import pixelitor.Composition.LayerAdder;
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
    private Layer activeLayerBefore;
    private Layer newLayer;
    private final int newLayerIndex;
    private final MaskViewMode viewModeBefore;

    public NewLayerEdit(String editName, LayerHolder holder,
                        Layer newLayer, Layer activeLayerBefore,
                        MaskViewMode viewModeBefore) {
        super(editName, holder.getComp());

        assert activeLayerBefore != newLayer;

        this.holder = holder;
        this.activeLayerBefore = activeLayerBefore;
        this.viewModeBefore = viewModeBefore;
        this.newLayer = newLayer;
        newLayerIndex = holder.indexOf(newLayer);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        holder.deleteLayer(newLayer, false);
        comp.setActiveLayer(activeLayerBefore);
        viewModeBefore.activate(comp, activeLayerBefore);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        new LayerAdder(holder)
            .atIndex(newLayerIndex)
            .add(newLayer);
    }

    @Override
    public void die() {
        super.die();

        newLayer = null;
        activeLayerBefore = null;
        holder = null;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(holder.createDebugNode("holder"));
        node.add(activeLayerBefore.createDebugNode("active layer before"));
        node.add(newLayer.createDebugNode("new layer"));
        node.addInt("new layer index", newLayerIndex);
        node.addAsString("view mode before", viewModeBefore);

        return node;
    }
}
