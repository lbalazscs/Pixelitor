/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.Filter;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A change to a filter on an {@link AdjustmentLayer}.
 * This can be a change in the filter's parameters or a replacement of the filter itself.
 */
public class FilterChangedEdit extends PixelitorEdit {
    private final AdjustmentLayer layer;
    private Filter prevFilter;
    private String prevName;

    public FilterChangedEdit(String editName, AdjustmentLayer layer, Filter prevFilter, String prevName) {
        super(editName, layer.getComp());
        this.layer = layer;
        this.prevFilter = prevFilter;
        this.prevName = prevName;

        // a backup name is needed only if the new filter has a different type
        assert (prevName != null) ^ (prevFilter.getClass().equals(layer.getFilter().getClass()));
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        swapState();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        swapState();
    }

    private void swapState() {
        Filter currentFilter = layer.getFilter();
        layer.restoreFilter(prevFilter);
        prevFilter = currentFilter;

        // if the layer name also changed (e.g., filter replacement), swap it too
        if (prevName != null) {
            String tmpName = layer.getName();
            layer.setName(prevName, false);
            prevName = tmpName;
        }
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(prevFilter.createDebugNode("backupFilter"));

        return node;
    }
}
