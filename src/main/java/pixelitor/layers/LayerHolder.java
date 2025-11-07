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
import pixelitor.Invariants;
import pixelitor.history.*;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.Debuggable;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static pixelitor.layers.LayerMoveDirection.UP;

/**
 * Interface representing any layer container that can hold multiple
 * child layers, such as compositions, layer groups, or smart objects.
 */
public interface LayerHolder extends Debuggable {
    /**
     * Returns the index of the given layer within this holder.
     */
    int indexOf(Layer layer);

    /**
     * Returns the number of layers directly contained within this holder.
     */
    int getNumLayers();

    /**
     * Returns the layer at the given index from this holder's list of direct children.
     */
    Layer getLayer(int index);

    /**
     * Checks if the given layer is a direct child of this holder.
     */
    boolean listContainsLayer(Layer layer);

    /**
     * Recursively checks if this layer holder contains
     * a layer of the given type at any nesting level.
     */
    boolean containsLayerOfType(Class<? extends Layer> type);

    /**
     * Returns a Stream of the direct child layers of this holder.
     */
    Stream<? extends Layer> directChildrenStream();

    /**
     * Returns whether this holder is allowed to have zero child layers.
     * A {@link Composition} cannot be empty, but a {@link LayerGroup} can.
     */
    boolean canBeEmpty();

    /**
     * Checks if this holder is the direct parent of the
     * currently active layer in the entire composition.
     */
    default boolean isHolderOfActiveLayer() {
        return getComp().getActiveHolder() == this;
    }

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
     * Inserts a layer at the given index.
     * The update flag controls whether this is a full-featured insertion
     * (with UI updates and history) or just a low-level list modification.
     */
    void insertLayer(Layer layer, int index, boolean update);

    default LayerAdder adder() {
        return new LayerAdder(this);
    }

    /**
     * Moves the currently active layer up or down in the layer stack.
     * Handles moving layers into or out of adjacent {@link LayerGroup}s automatically.
     */
    default void reorderActiveLayer(LayerMoveDirection direction) {
        assert isHolderOfActiveLayer();
        Layer activeLayer = getComp().getActiveLayer();
        String editName = direction.getName();

        int index = indexOf(activeLayer);
        // if the layer is already at the edge of its current holder,
        // and the holder is a group, then move it out of the group
        boolean atEdge = direction.isAtEdge(index, getNumLayers());
        if (atEdge && this instanceof LayerGroup group) {
            LayerHolder groupHolder = group.getHolder();
            int groupIndex = groupHolder.indexOf(group);

            // logic for moving out of group:
            // if moving UP, we target after the group (groupIndex + 1)
            // if moving DOWN, we target the group's index (groupIndex)
            int targetIndex = direction == UP ? groupIndex + 1 : groupIndex;
            transferLayerToHolder(activeLayer, groupHolder, targetIndex, editName);
            return;
        }

        int newIndex = direction == UP ? index + 1 : index - 1;
        if (newIndex < 0 || newIndex > getNumLayers() - 1) {
            return;
        }
        if (getLayer(newIndex) instanceof LayerGroup group) {
            // special case: move the layer into the group

            int groupIndex = direction == UP ? 0 : group.getNumLayers();
            transferLayerToHolder(activeLayer, group, groupIndex, editName);
        } else {
            reorderLayer(index, newIndex, true, editName);
        }
    }

    /**
     * Transfers the given layer from this holder to the given target
     * holder at the given index.
     */
    default void transferLayerToHolder(Layer layer, LayerHolder targetHolder, int targetIndex, String editName) {
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

        if (addToHistory) {
            History.add(new LayerOrderChangeEdit(editName, this, oldIndex, newIndex));
        }
    }

    /**
     * Updates the UI to reflect the layer order change.
     */
    void reorderLayerUI(int oldIndex, int newIndex);

    /**
     * Deletes a layer from this holder.
     */
    void deleteLayer(Layer layer, boolean addToHistory);

    /**
     * Replaces a layer with another, while keeping its position, mask, ui
     */
    void replaceLayer(Layer before, Layer after);

    /**
     * Selects the layer above the current one.
     */
    default void raiseLayerSelection() {
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

        assert Invariants.fadeWouldWorkOn(comp);
    }

    /**
     * Selects the layer below the current one.
     */
    default void lowerLayerSelection() {
        Composition comp = getComp();
        int oldIndex = indexOf(comp.getActiveLayer());
        int newIndex = oldIndex - 1;
        if (newIndex < 0) {
            return;
        }

        comp.setActiveLayer(getLayer(newIndex), true,
            LayerMoveAction.LOWER_LAYER_SELECTION);

        assert Invariants.fadeWouldWorkOn(comp);
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
     * Merges the given layer down into the layer below it.
     * This method assumes that {@link #canMergeDown(Layer)}  has previously returned true.
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
     * Signals a change in the holder's content, requiring a visual update.
     * Implementations must invalidate cached images and trigger a re-rendering of the main composition.
     */
    void update(boolean updateHistogram);

    /**
     * A convenience method that always updates the histogram as well.
     */
    void update();

    /**
     * Recursively invalidates the image caches in the direction
     * of the root of the holder tree until the composition's image
     * cache is also invalidated.
     */
    void invalidateImageCache();

    /**
     * Callback invoked when a smart object belonging to this holder has been changed.
     */
    void smartObjectChanged(boolean linked);

    /**
     * Used during export to the OpenRaster (.ora) file format.
     * It returns the opening XML tag for the layer stack represented by this holder.
     */
    String getORAStackXML();

    /**
     * Returns the root {@link Composition} object.
     */
    Composition getComp();

    String getName();


    /**
     * Converts the visible layers in this holder to a new layer group.
     */
    default void convertVisibleLayersToGroup() {
        int[] indices = directChildrenStream()
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
