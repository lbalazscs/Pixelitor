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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.history.*;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.Debuggable;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Interface representing any layer container that can hold multiple
 * child layers, such as compositions, layer groups, or smart objects.
 */
public interface LayerHolder extends Debuggable {
    /**
     * Returns the index of the currently active layer within this holder.
     */
    int getActiveLayerIndex();

    /**
     * Returns the index of the given layer within this holder.
     */
    int indexOf(Layer layer);

    /**
     * Returns the number of layers directly contained within this holder.
     */
    int getNumLayers();

    /**
     * Returns the layer at the given index within this holder.
     */
    Layer getLayer(int index);

    /**
     * Recursively checks if this layer holder contains
     * the given layer at the top nesting level.
     */
    boolean listContainsLayer(Layer layer);

    /**
     * Recursively checks if this layer holder contains
     * a layer of the given type at any nesting level.
     */
    boolean containsLayerOfType(Class<? extends Layer> type);

    /**
     * Adds a layer to this holder without adding it to the UI.
     */
    default void addLayerWithoutUI(Layer newLayer) {
        adder().skipUIAdd().add(newLayer);
    }

    /**
     * Adds a layer to this holder and also adds an undoable edit
     * to the history, using the given edit name.
     */
    default void addWithHistory(Layer newLayer, String editName) {
        adder().withHistory(editName).add(newLayer);
    }

    /**
     * Adds a layer to this holder, updating the UI, but without history.
     */
    default void add(Layer newLayer) {
        adder().add(newLayer);
    }

    /**
     * Moves the currently active layer up or down in the layer stack.
     */
    default void reorderActiveLayer(boolean up) {
        assert isHolderOfActiveLayer();
        Layer activeLayer = getComp().getActiveLayer();
        String editName = up ? LayerMoveAction.RAISE_LAYER : LayerMoveAction.LOWER_LAYER;

        int index = indexOf(activeLayer);
        // if the layer is already at the edge of its current holder,
        // and the holder is a group, then move it out of the group
        if ((up && index == getNumLayers() - 1) || (!up && index == 0)) {
            if (this instanceof LayerGroup group) {
                LayerHolder groupHolder = group.getHolder();
                int groupIndex = groupHolder.indexOf(group);
                int targetIndex = up ? groupIndex + 1 : groupIndex;
                moveLayerInto(activeLayer, groupHolder, targetIndex, editName);
                return;
            }
        }

        int newIndex = up ? index + 1 : index - 1;
        if (newIndex < 0 || newIndex > getNumLayers() - 1) {
            return;
        }
        if (getLayer(newIndex) instanceof LayerGroup group) {
            // special case: move the layer into the group

            int groupIndex = up ? 0 : group.getNumLayers();
            moveLayerInto(activeLayer, group, groupIndex, editName);
        } else {
            reorderLayer(index, newIndex, true, editName);
        }
    }

    /**
     * Transfers the given layer from this holder to the given target
     * holder at the given index.
     */
    default void moveLayerInto(Layer layer, LayerHolder targetHolder, int targetIndex, String editName) {
        assert targetHolder != this;
        assert listContainsLayer(layer);
        assert !targetHolder.listContainsLayer(layer);
        assert targetIndex >= 0 && targetIndex <= targetHolder.getNumLayers();

        if (editName != null) {
            int prevIndex = indexOf(layer);
            assert prevIndex >= 0;

            History.add(new ChangeHolderEdit(editName, layer, this, prevIndex, targetHolder, targetIndex));
        }

        deleteLayer(layer, false);
        targetHolder.adder()
            .atIndex(targetIndex)
            .add(layer);
    }

    /**
     * Moves the currently active layer to the top of the layer stack within this holder.
     */
    default void moveActiveLayerToTop() {
        assert isHolderOfActiveLayer();

        int prevIndex = indexOf(getComp().getActiveLayer());
        int newIndex = getNumLayers() - 1;
        reorderLayer(prevIndex, newIndex,
            true, LayerMoveAction.LAYER_TO_TOP);
    }

    /**
     * Moves the currently active layer to the bottom of the layer stack within this holder.
     */
    default void moveActiveLayerToBottom() {
        assert isHolderOfActiveLayer();

        int prevIndex = indexOf(getComp().getActiveLayer());
        reorderLayer(prevIndex, 0,
            true, LayerMoveAction.LAYER_TO_BOTTOM);
    }

    /**
     * Changes the position of a layer within this holder, without adding the change to history.
     */
    default void reorderLayer(int oldIndex, int newIndex) {
        reorderLayer(oldIndex, newIndex, false, null);
    }

    /**
     * Changes the position of a layer within this holder.
     */
    default void reorderLayer(int oldIndex, int newIndex,
                              boolean addToHistory, String editName) {
        // Called when the layer order is changed by an action.
        // The GUI has to be updated.
        if (newIndex < 0) {
            return;
        }
        if (newIndex >= getNumLayers()) {
            return;
        }
        if (oldIndex == newIndex) {
            return;
        }

        Layer layer = getLayer(oldIndex);
        removeLayerFromList(layer);
        insertLayer(layer, newIndex, false);

        reorderLayerUI(oldIndex, newIndex);
        update();
        Layers.layersReordered(this);

        if (addToHistory) {
            History.add(new LayerOrderChangeEdit(editName, this, oldIndex, newIndex));
        }
    }

    /**
     * Updates the UI to reflect the layer order change.
     */
    void reorderLayerUI(int oldIndex, int newIndex);

    /**
     * Inserts a layer at the specified index.
     */
    void insertLayer(Layer layer, int index, boolean update);

    /**
     * Deletes a layer from this holder.
     */
    void deleteLayer(Layer layer, boolean addToHistory);

    /**
     * Returns whether this holder can be empty,
     * i.e. whether it can contain zero layers.
     */
    boolean canBeEmpty();

    /**
     * Replaces a layer with another, while keeping its position, mask, ui
     */
    void replaceLayer(Layer before, Layer after);

    /**
     * Selects the layer above the current one.
     */
    default void selectLayerAbove() {
        Composition comp = getComp();
        Layer activeLayer = comp.getActiveLayer();
        Layer newTarget;

        int prevIndex = indexOf(activeLayer);

        int newIndex = prevIndex + 1;
        if (newIndex >= getNumLayers()) {
            if (activeLayer.isTopLevel()) {
                return;
            } else {
                // if the top layer is selected, then
                // raise selection selects the holder itself
                // select the target's parent holder
                assert activeLayer.getHolder() == this;
                newTarget = (CompositeLayer) this;
            }
        } else {
            newTarget = getLayer(newIndex);
        }

        comp.setActiveLayer(newTarget, true,
            LayerMoveAction.RAISE_LAYER_SELECTION);

        assert ConsistencyChecks.fadeWouldWorkOn(comp);
    }

    /**
     * Selects the layer below the current one.
     */
    default void selectLayerBelow() {
        Composition comp = getComp();
        int oldIndex = indexOf(comp.getActiveLayer());
        int newIndex = oldIndex - 1;
        if (newIndex < 0) {
            return;
        }

        comp.setActiveLayer(getLayer(newIndex), true,
            LayerMoveAction.LOWER_LAYER_SELECTION);

        assert ConsistencyChecks.fadeWouldWorkOn(comp);
    }

    /**
     * Checks if the given layer can be merged down with the layer beneath it.
     */
    default boolean canMergeDown(Layer layer) {
        int index = indexOf(layer);
        if (index > 0 && layer.isVisible()) {
            Layer below = getLayer(index - 1);
            return below.getClass() == ImageLayer.class && below.isVisible();
        }
        return false;
    }

    /**
     * Merges the specified layer down into the layer below it.
     * This method assumes that canMergeDown()  has previously returned true.
     */
    default void mergeDown(Layer layer) {
        int layerIndex = indexOf(layer);
        var belowLayer = (ImageLayer) getLayer(layerIndex - 1);

        var belowImage = belowLayer.getImage();
        var maskViewModeBefore = getComp().getView().getMaskViewMode();
        var imageBefore = ImageUtils.copyImage(belowImage);

        // apply the effect of the merged layer to the image of the image layer
        Graphics2D g = belowImage.createGraphics();
        g.translate(-belowLayer.getTx(), -belowLayer.getTy());
        BufferedImage result = layer.render(g, belowImage, false);
        if (result != null) {  // this was an adjustment
            belowLayer.setImage(result);
        }
        g.dispose();

        belowLayer.updateIconImage();

        deleteLayer(layer, false);

        History.add(new MergeDownEdit(this, layer,
            belowLayer, imageBefore, maskViewModeBefore, layerIndex));
    }

    /**
     * Checks if the current active layer belongs to this holder.
     */
    default boolean isHolderOfActiveLayer() {
        return getComp().getActiveHolder() == this;
    }

    void update(boolean updateHistogram);

    void update();

    /**
     * Callback invoked when a smart object belonging to this holder has been changed.
     */
    void smartObjectChanged(boolean linked);

    String getORAStackXML();

    Composition getComp();

    String getName();

    /**
     * Recursively invalidate the image caches in the direction
     * of the root of the holder tree until the composition's image
     * cache is also invalidated.
     */
    void invalidateImageCache();

    /**
     * Return a Stream of layers at this level.
     */
    Stream<? extends Layer> levelStream();

    /**
     * Converts the visible layers in this holder to a new layer group.
     */
    default void convertVisibleLayersToGroup() {
        int[] indices = levelStream()
            .filter(Layer::isVisible)
            .mapToInt(this::indexOf)
            .toArray();
        if (indices.length == 0) {
            return;
        }

        convertToGroup(indices, null, true);
    }

    /**
     * Converts the layers at the given indices to a group,
     * optionally using an existing group as the target.
     */
    default void convertToGroup(int[] indices, LayerGroup target, boolean addHistory) {
        List<Layer> movedLayers = new ArrayList<>(indices.length);
        for (int index : indices) {
            movedLayers.add(getLayer(index));
        }
        for (Layer layer : movedLayers) {
            deleteInternal(layer);
        }

        LayerGroup newGroup;
        if (target != null) {
            // history edits must use a specific instance for consistency
            assert !addHistory;
            newGroup = target;
            newGroup.setLayers(movedLayers);
            // restore the UI-level invariants
            newGroup.updateChildrenUI();
        } else {
            assert addHistory;
            newGroup = new LayerGroup(getComp(), LayerGroup.generateName(), movedLayers);
        }

        int lastMovedIndex = indices[indices.length - 1];
        int newIndex = lastMovedIndex + 1 - indices.length;
        adder().atIndex(newIndex).add(newGroup);

        if (addHistory) {
            History.add(new GroupingEdit(this, newGroup, indices, null, true));
        }
    }

    /**
     * Adds a new empty group to this holder.
     */
    default void addEmptyGroup() {
        LayerGroup group = new LayerGroup(getComp(), LayerGroup.generateName());
        addWithHistory(group, "New Layer Group");
    }

    default LayerAdder adder() {
        return new LayerAdder(this);
    }

    /**
     * Adds a layer (only) to the list of layers maintained by this holder, at a specific index.
     * This is just an internal helper method.
     */
    void addLayerToList(Layer newLayer, int index);

    /**
     * Removes a layer (only) from the layer list.
     * This is just an internal helper method.
     */
    void removeLayerFromList(Layer layer);

    /**
     * This form of layer deletion allows to temporarily violate the
     * constraint that some holders must always contain at least one layer.
     * This is just an internal helper method.
     */
    void deleteInternal(Layer layer);
}
