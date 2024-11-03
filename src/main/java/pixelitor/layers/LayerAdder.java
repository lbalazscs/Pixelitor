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
import static pixelitor.layers.LayerAdder.Position.BELLOW_ACTIVE;

/**
 * A helper class that encapsulates the logic of adding a
 * layer to a {@link LayerHolder}
 */
public class LayerAdder {
    private final LayerHolder holder;
    private final Composition comp;
    private String editName; // null if the add should not be added to history
    private int targetIndex = -1;
    private boolean update = true;

    public enum Position {ABOVE_ACTIVE, BELLOW_ACTIVE}

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

    public LayerAdder atPosition(Position position) {
        this.position = position;
        return this;
    }

    /**
     * Means that this is part of the construction,
     * the layer is not added as a result of a user interaction
     */
    public LayerAdder noUI() {
        addToUI = false;
        return this;
    }

    /**
     * Used when the composite image doesn't change.
     */
    public LayerAdder noUpdate() {
        update = false;
        return this;
    }

    private void calcIndexBasedOnRelPosition() {
        int activeIndex = holder.getActiveLayerIndex();
        if (activeIndex == -1) { // no active layer
            if (position == BELLOW_ACTIVE) {
                targetIndex = 0;
            } else {
                targetIndex = holder.getNumLayers(); // add to the top
            }
        } else {
            if (position == ABOVE_ACTIVE) {
                targetIndex = activeIndex + 1;
            } else if (position == BELLOW_ACTIVE) {
                targetIndex = activeIndex;
            } else {
                throw new IllegalStateException("position = " + position);
            }
        }
    }

    public LayerAdder atIndex(int targetIndex) {
        this.targetIndex = targetIndex;
        return this;
    }

    /**
     * The final operation, which actually adds the layer.
     */
    public void add(Layer layer) {
        layer.setHolder(holder);

        Layer previousActiveLayer = null;
        MaskViewMode previousMaskViewMode = null;
        if (needsHistory()) {
            previousActiveLayer = comp.getActiveLayer();
            previousMaskViewMode = comp.getView().getMaskViewMode();
        }

        if (targetIndex == -1) { // no index was explicitly set
            calcIndexBasedOnRelPosition();
        }
        holder.addLayerToList(targetIndex, layer);
        comp.setActiveLayer(layer);

        if (addToUI) {
            if (holder == comp) {
                comp.getView().addLayerToGUI(layer, targetIndex);
            } else {
                ((CompositeLayer) holder).updateChildrenUI();
            }

            // mocked views will not set a UI
            assert AppMode.isUnitTesting() || layer.hasUI();

            comp.setDirty(true);
            if (update) {
                holder.update();
            }
        }

        if (needsHistory()) {
            History.add(new NewLayerEdit(editName,
                layer, previousActiveLayer, previousMaskViewMode));
        }
        assert comp.checkInvariants();
    }

    private boolean needsHistory() {
        return editName != null;
    }
}
