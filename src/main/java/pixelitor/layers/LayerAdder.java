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

package pixelitor.layers;

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.history.History;
import pixelitor.history.NewLayerEdit;

import static pixelitor.layers.LayerAdder.Position.ABOVE_ACTIVE;

/**
 * A helper class that encapsulates the logic of adding a
 * layer to a {@link LayerHolder}.
 */
public class LayerAdder {
    private final LayerHolder holder;
    private final Composition comp;
    private String editName; // null if the addition should not be recorded in history
    private int insertionIndex = -1;
    private Position position = ABOVE_ACTIVE;
    private boolean addToUI = true;
    private boolean updateComp = true;

    public enum Position {ABOVE_ACTIVE, BELOW_ACTIVE}

    public LayerAdder(LayerHolder holder) {
        this.comp = holder.getComp();
        this.holder = holder;
    }

    public LayerAdder withHistory(String editName) {
        this.editName = editName;
        return this;
    }

    /**
     * Sets the relative position where the layer should be inserted.
     * Ignored if a specific index is set with {@link #atIndex(int)}.
     */
    public LayerAdder atPosition(Position position) {
        this.position = position;
        return this;
    }

    /**
     * Sets a specific index where the layer should be inserted.
     * Takes precedence over {@link #atPosition(Position)}.
     */
    public LayerAdder atIndex(int targetIndex) {
        assert targetIndex >= 0;
        this.insertionIndex = targetIndex;
        return this;
    }

    /**
     * Skips adding the layer's UI (because the layer is being added
     * programmatically rather than as a direct user action).
     * <p>
     * Note: skipping UI add also skips marking the composition as dirty
     * ({@link Composition#setDirty(boolean)}) and updating the layer holder
     * ({@link LayerHolder#update()}).
     */
    public LayerAdder skipUIAdd() {
        addToUI = false;
        return this;
    }

    /**
     * Used when the composite image doesn't change.
     */
    public LayerAdder skipCompUpdate() {
        updateComp = false;
        return this;
    }

    /**
     * Calculates the insertion index based on the relative position setting.
     */
    private int calcIndexFromPosition() {
        int activeIndex = holder.indexOf(comp.getActiveLayer());
        boolean above = position == ABOVE_ACTIVE;

        if (activeIndex == -1) {
            // the active layer is not a direct child of the target holder:
            // default to adding at the top or bottom of the holder
            return above ? holder.getNumLayers() : 0;
        }

        // the active layer is a direct child, so insert relative to it
        return above ? activeIndex + 1 : activeIndex;
    }

    /**
     * The final operation, which actually adds the layer.
     */
    public void add(Layer layer) {
        layer.setHolder(holder);
        Layer prevActiveLayer = null;
        MaskViewMode prevMaskViewMode = null;

        // capture state for history if needed
        if (isHistoryEnabled()) {
            prevActiveLayer = comp.getActiveLayer();
            prevMaskViewMode = comp.getMaskViewMode();
        }

        int index = insertionIndex != -1 ? insertionIndex : calcIndexFromPosition();

        holder.insertDirectChild(layer, index);
        comp.setActiveLayer(layer);

        if (addToUI) {
            if (holder == comp) {
                comp.getView().addLayerUI(layer, index);
                comp.fireLayerUICountChanged();
            } else {
                ((CompositeLayer) holder).updateChildrenUI();
            }

            // mocked views will not set a UI
            assert AppMode.isUnitTesting() || layer.hasUI();

            comp.setDirty(true);
            if (updateComp) {
                holder.update();
            }
        }

        if (isHistoryEnabled()) {
            History.add(new NewLayerEdit(editName,
                layer, prevActiveLayer, prevMaskViewMode));
        }
        assert comp.checkInvariants();
    }

    private boolean isHistoryEnabled() {
        return editName != null;
    }
}
