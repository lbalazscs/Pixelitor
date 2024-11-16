/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
 * layer to a {@link LayerHolder}
 */
public class LayerAdder {
    private final LayerHolder holder;
    private final Composition comp;
    private String editName; // null if the add should not be added to history
    private int insertionIndex = -1;
    private boolean shouldUpdateComp = true;

    public enum Position {ABOVE_ACTIVE, BELOW_ACTIVE}

    private Position position = ABOVE_ACTIVE;
    private boolean addToUI = true;

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
     */
    public LayerAdder atPosition(Position position) {
        this.position = position;
        return this;
    }

    /**
     * Sets a specific index where the layer should be inserted.
     */
    public LayerAdder atIndex(int targetIndex) {
        this.insertionIndex = targetIndex;
        return this;
    }

    /**
     * Means that this is part of the construction,
     * the layer is not added as a result of a user interaction
     */
    public LayerAdder skipUIAdd() {
        addToUI = false;
        return this;
    }

    /**
     * Used when the composite image doesn't change.
     */
    public LayerAdder skipCompUpdate() {
        shouldUpdateComp = false;
        return this;
    }

    /**
     * Calculates the insertion index based on the relative position setting.
     */
    private void calcIndexFromPosition() {
        int activeIndex = holder.getActiveLayerIndex();
        if (activeIndex == -1) { // no active layer
            insertionIndex = switch (position) {
                case ABOVE_ACTIVE -> holder.getNumLayers(); // add to the top
                case BELOW_ACTIVE -> 0;
            };
        } else {
            insertionIndex = switch (position) {
                case ABOVE_ACTIVE -> activeIndex + 1;
                case BELOW_ACTIVE -> activeIndex;
            };
        }
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
            prevMaskViewMode = comp.getView().getMaskViewMode();
        }

        if (insertionIndex == -1) { // no index was explicitly set
            calcIndexFromPosition();
        }

        holder.addLayerToList(insertionIndex, layer);
        comp.setActiveLayer(layer);

        if (addToUI) {
            if (holder == comp) {
                comp.getView().addLayerToGUI(layer, insertionIndex);
            } else {
                ((CompositeLayer) holder).updateChildrenUI();
            }

            // mocked views will not set a UI
            assert AppMode.isUnitTesting() || layer.hasUI();

            comp.setDirty(true);
            if (shouldUpdateComp) {
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
