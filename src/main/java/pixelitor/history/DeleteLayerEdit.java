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
 * Represents the deletion of a {@link Layer} from a {@link LayerHolder}.
 */
public class DeleteLayerEdit extends PixelitorEdit {
    private final LayerHolder holder;
    private final Layer layer;
    private final int layerIndex;

    // the active layer before the deletion: the deleted layer itself,
    // a layer nested in it, or an unrelated layer
    private Layer activeLayerBefore;
    // the mask view mode that belonged to activeLayerBefore
    private MaskViewMode maskViewModeBefore;

    /**
     * Must be created before the layer is removed, because it
     * captures the active layer and its mask view mode.
     */
    public DeleteLayerEdit(LayerHolder holder, Layer layer, int layerIndex) {
        this(holder, layer, layerIndex,
            holder.getComp().getActiveLayer(),
            holder.getComp().getMaskViewMode());
    }

    /**
     * Used when the state was captured earlier than the creation of the edit.
     * The active layer is not required to be in the composition at this point
     * (for example when a group containing it was already removed).
     */
    public DeleteLayerEdit(LayerHolder holder, Layer layer, int layerIndex,
                           Layer activeLayerBefore, MaskViewMode maskViewModeBefore) {
        super("Delete " + layer.getName(), holder.getComp(), true);

        assert holder.getComp() == layer.getComp();
        assert activeLayerBefore != null;
        assert maskViewModeBefore != null;

        this.holder = holder;
        this.layer = layer;
        this.layerIndex = layerIndex;
        this.activeLayerBefore = activeLayerBefore;
        this.maskViewModeBefore = maskViewModeBefore;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        // also makes the re-inserted layer active (resetting the mask view mode)
        holder.insertLayer(layer, layerIndex);

        if (comp.contains(activeLayerBefore)) {
            comp.setActiveLayer(activeLayerBefore); // no-op if it's the re-inserted layer
            // must come after the activation, which resets the mask view mode
            maskViewModeBefore.restoreOn(activeLayerBefore);
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        // the user could have changed these after the undo
        activeLayerBefore = comp.getActiveLayer();
        maskViewModeBefore = comp.getMaskViewMode();

        holder.deleteLayer(layer, false);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addInt("layer index", layerIndex);
        node.add(layer.createDebugNode());
        node.addQuotedString("active layer before", activeLayerBefore.getName());
        node.addAsString("mask view mode before", maskViewModeBefore);

        return node;
    }
}
