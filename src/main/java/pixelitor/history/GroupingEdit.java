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
import pixelitor.layers.LayerGroup;
import pixelitor.layers.LayerHolder;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * This edit is used for both grouping and ungrouping.
 */
public class GroupingEdit extends PixelitorEdit {
    private final LayerHolder holder;
    private final LayerGroup group;

    // the indices of the group's children in the holder in the ungrouped state
    private final int[] ungroupedIndices;
    // the index of the group in the holder in the grouped state
    // (can't be derived from the indices if the group is empty)
    private final int groupIndex;

    // the active layer before the original operation (can be null)
    private final Layer activeLayerBefore;
    private final boolean isGrouping; // whether this is a grouping or ungrouping

    public GroupingEdit(LayerHolder holder, LayerGroup group,
                        int[] ungroupedIndices, int groupIndex,
                        Layer activeLayerBefore, boolean isGrouping) {
        super(isGrouping ? "Grouping" : "Ungrouping", holder.getComp());

        assert ungroupedIndices != null;
        assert groupIndex >= 0;

        this.holder = holder;
        this.group = group;
        this.ungroupedIndices = ungroupedIndices;
        this.groupIndex = groupIndex;
        this.activeLayerBefore = activeLayerBefore;
        this.isGrouping = isGrouping;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        if (isGrouping) {
            ungroup();
        } else {
            group();
        }
        restoreActiveLayer();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        // the active layer is deliberately not restored here, because
        // the original operation also left the new state's layer active
        if (isGrouping) {
            group();
        } else {
            ungroup();
        }
    }

    private void ungroup() {
        group.replaceWithUngrouped(ungroupedIndices, false);
    }

    private void group() {
        holder.regroup(ungroupedIndices, groupIndex, group);
    }

    private void restoreActiveLayer() {
        if (activeLayerBefore != null && comp.contains(activeLayerBefore)) {
            comp.setActiveLayer(activeLayerBefore, false, null);
        }
    }
}
