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

import pixelitor.Composition;
import pixelitor.GUIMode;
import pixelitor.history.History;
import pixelitor.history.NewLayerEdit;

import static pixelitor.layers.LayerAdder.Position.ABOVE_ACTIVE;
import static pixelitor.layers.LayerAdder.Position.BELLOW_ACTIVE;
import static pixelitor.layers.LayerAdder.Position.TOP;

/**
 * A helper class that encapsulates the logic of adding a
 * layer to a {@link LayerHolder}
 */
public class LayerAdder {
    private final LayerHolder holder;
    private final Composition comp;
    private String editName; // null if the add should not be added to history
    private int targetLayerIndex = -1;
    private boolean update = true;

    public enum Position {TOP, ABOVE_ACTIVE, BELLOW_ACTIVE}

    private Position position = TOP;
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
        int activeLayerIndex = holder.getActiveLayerIndex();
        if (activeLayerIndex == -1) { // no active layer
            if (position == BELLOW_ACTIVE) {
                targetLayerIndex = 0;
            } else {
                targetLayerIndex = holder.getNumLayers();
            }
        } else {
            if (position == BELLOW_ACTIVE) {
                targetLayerIndex = activeLayerIndex;
            } else if (position == ABOVE_ACTIVE) {
                targetLayerIndex = activeLayerIndex + 1;
            } else {
                throw new IllegalStateException("position = " + position);
            }
        }
    }

    public LayerAdder atIndex(int targetLayerIndex) {
        this.targetLayerIndex = targetLayerIndex;
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

        if (targetLayerIndex == -1) { // no index was explicitly set
            if (position == TOP) {
                targetLayerIndex = holder.getNumLayers();
            } else {
                calcIndexBasedOnRelPosition();
            }
        }
        holder.addLayerToList(targetLayerIndex, layer);
        comp.setActiveLayer(layer);

        if (addToUI) {
            if (holder == comp) {
                comp.getView().addLayerToGUI(layer, targetLayerIndex);
            } else {
                ((CompositeLayer) holder).updateChildrenUI();
            }

            // mocked views will not set a UI
            assert GUIMode.isUnitTesting() || layer.hasUI();

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
