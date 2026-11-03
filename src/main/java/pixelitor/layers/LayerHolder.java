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

import pixelitor.Composition;
import pixelitor.Invariants;
import pixelitor.history.*;
import pixelitor.tools.Tools;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.Debuggable;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
    boolean hasDirectChild(Layer layer);

    /**
     * Recursively checks if this layer holder contains
     * the given layer at any nesting level.
     * Implementations that are themselves layers also contain themselves.
     */
    default boolean contains(Layer layer) {
        for (Layer child : directChildren()) {
            if (child.contains(layer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively checks if this layer holder contains
     * the composition's active layer at any nesting level.
     */
    default boolean containsActiveLayer() {
        return contains(getComp().getActiveLayer());
    }

    /**
     * Returns the direct child layers of this holder.
     * The returned view must not be used to modify the holder.
     */
    Iterable<? extends Layer> directChildren();

    /**
     * Returns a Stream of the direct child layers of this holder.
     */
    default Stream<? extends Layer> directChildrenStream() {
        return StreamSupport.stream(directChildren().spliterator(), false);
    }

    /**
     * Applies an action to all nested layers, traversing groups and smart objects.
     * For composite layers, the children are visited before the layer
     * itself (and then its mask), so that the order is bottom-up.
     */
    default void forEachNestedLayer(Consumer<Layer> action, boolean includeMasks) {
        for (Layer child : directChildren()) {
            child.forEachNestedLayer(action, includeMasks);
        }
    }

    default void forEachNestedLayerAndMask(Consumer<Layer> action) {
        forEachNestedLayer(action, true);
    }

    /**
     * Applies an action to all nested layers of a specific type.
     */
    default <T extends Layer> void forEachNestedLayerOfType(Class<T> layerType, Consumer<T> action) {
        forEachNestedLayer(layer -> {
            if (layerType.isInstance(layer)) {
                action.accept(layerType.cast(layer));
            }
        }, false);
    }

    /**
     * Recursively searches for the first layer that satisfies the given
     * predicate, using the same traversal order as forEachNestedLayer.
     */
    default Layer findFirstLayerWhere(Predicate<Layer> predicate, boolean includeMasks) {
        for (Layer child : directChildren()) {
            Layer found = child.findFirstLayerWhere(predicate, includeMasks);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Checks that all direct children belong to the same composition
     * as this holder and that they refer back to this holder.
     * Throws an {@link AssertionError} if an inconsistency is found.
     */
    default void checkDirectChildLinks() {
        Composition comp = getComp();
        for (Layer child : directChildren()) {
            if (child.getComp() != comp) {
                throw new AssertionError("bad comp in layer '%s' (that comp=%s, this=%s)"
                    .formatted(child.getName(), child.getComp(), comp));
            }
            if (!child.isDirectChildOf(this)) {
                throw new AssertionError("bad holder in layer '%s' (that holder=%s, this=%s)"
                    .formatted(child.getName(), child.getHolder(), this));
            }
        }
    }

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
     * Inserts a layer at the given index and makes it active,
     * updating the UI, but without adding history.
     * Holders with special insertion semantics (smart objects) override this.
     */
    default void insertLayer(Layer newLayer, int index) {
        adder().atIndex(index).add(newLayer);
    }

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
        boolean up = direction == UP;

        int index = indexOf(activeLayer);
        int newIndex = direction.neighborIndex(index);

        if (newIndex < 0 || newIndex >= getNumLayers()) {
            // already at the edge of this holder: if it's a group, move the layer out
            if (this instanceof LayerGroup group) {
                LayerHolder groupHolder = group.getHolder();
                int groupIndex = groupHolder.indexOf(group);

                // above the group when moving UP, at the group's index (below it) when moving DOWN
                int targetIndex = up ? groupIndex + 1 : groupIndex;
                transferLayerToHolder(activeLayer, groupHolder, targetIndex, editName);
            }
            return;
        }

        if (getLayer(newIndex) instanceof LayerGroup group) {
            // move into the neighboring group: to its bottom when moving UP,
            // to its top when moving DOWN
            int targetIndex = up ? 0 : group.getNumLayers();
            transferLayerToHolder(activeLayer, group, targetIndex, editName);
        } else {
            reorderLayer(index, newIndex, true, editName);
        }
    }

    /**
     * Transfers the given layer from this holder to the given target
     * holder at the given index.
     */
    default void transferLayerToHolder(Layer layer, LayerHolder targetHolder,
                                       int targetIndex, String editName) {
        assert targetHolder != this;
        assert hasDirectChild(layer);
        assert !targetHolder.hasDirectChild(layer);
        assert targetIndex >= 0 && targetIndex <= targetHolder.getNumLayers();

        // the mask view mode belongs to the active layer, and the activation
        // changes caused by the removal and insertion would reset it
        MaskViewMode maskViewMode = layer.isActive()
            ? getComp().getMaskViewMode()
            : MaskViewMode.NORMAL;

        if (editName != null) {
            int prevIndex = indexOf(layer);
            assert prevIndex >= 0;

            History.add(new ChangeHolderEdit(editName, layer, this, prevIndex, targetHolder, targetIndex));
        }

        deleteLayer(layer, false);
        targetHolder.adder()
            .atIndex(targetIndex)
            .add(layer);

        if (maskViewMode != MaskViewMode.NORMAL) {
            maskViewMode.restoreOn(layer); // the adder made the layer active
        }
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
     * Changes the position of a layer within this holder, and updates the GUI.
     */
    default void reorderLayer(int oldIndex, int newIndex,
                              boolean addToHistory, String editName) {
        if (oldIndex == newIndex || newIndex < 0 || newIndex >= getNumLayers()) {
            return;
        }

        Layer layer = getLayer(oldIndex);
        removeDirectChild(layer, false);
        insertDirectChild(layer, newIndex);

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
     * Updates the UI after a layer was inserted into this holder.
     */
    void updateUIAfterLayerInsertion(Layer newLayer, int index);

    /**
     * Updates the UI after a layer was deleted from this holder
     * (and the active layer was already adjusted). The holder itself
     * is updated by the caller afterwards.
     */
    void updateUIAfterLayerDeletion(Layer deletedLayer);

    /**
     * Deletes a layer from this holder.
     */
    default void deleteLayer(Layer layer, boolean addToHistory) {
        Composition comp = getComp();
        assert layer.getComp() == comp;
        assert layer.isDirectChildOf(this);
        assert canBeEmpty() || getNumLayers() >= 2;

        int index = indexOf(layer);
        assert index != -1;

        // must be created before the removal, because it
        // captures the active layer and the mask view mode
        if (addToHistory) {
            History.add(new DeleteLayerEdit(this, layer, index));
        }

        removeDirectChild(layer, false);

        if (layer.contains(comp.getActiveLayer())) {
            // the active layer was deleted, a new one must be activated
            comp.setActiveLayer(layerToActivateAfterRemoval(index));
        }

        updateUIAfterLayerDeletion(layer);
        update(); // always, even if there is no UI to update
    }

    /**
     * Replaces a layer with another, while keeping its position, mask, and UI.
     * Also sets the holder of the new layer.
     */
    default void replaceLayer(Layer before, Layer after) {
        Composition comp = getComp();
        assert after.getComp() == comp;

        int index = indexOf(before);
        assert index != -1;

        Layer activeBefore = comp.getActiveLayer();
        boolean containedActive = before.contains(activeBefore);

        // must be set before the UI transfer
        after.setHolder(this);
        before.transferMaskAndUITo(after);

        removeDirectChild(before, false);
        insertDirectChild(after, index);

        if (containedActive) {
            if (before == activeBefore) {
                // The mask and its editing state were transferred to 'after',
                // so the mask view mode stays valid. setActiveLayer can't be
                // used here, because it would reset the mask view mode to NORMAL.
                comp.setActiveLayerRef(after);
                if (comp.isActive()) {
                    Tools.editingTargetChanged(after);
                    LayerEvents.fireLayerActivated(after, false);
                }
                after.updateUI(); // the UI was transferred while 'before' was still active
            } else {
                // a container of the active layer was replaced, so
                // this is a real change of the active layer
                comp.setActiveLayer(after);
            }
        }

        assert comp.checkInvariants();
    }

    /**
     * Activates the layer above the current one.
     */
    default void activateLayerAbove() {
        assert isHolderOfActiveLayer();
        Composition comp = getComp();
        Layer activeLayer = comp.getActiveLayer();

        int newIndex = indexOf(activeLayer) + 1;
        Layer newActive;
        if (newIndex < getNumLayers()) {
            newActive = getLayer(newIndex);
        } else if (this instanceof CompositeLayer parent) {
            // already the top layer of a nested holder: select the holder itself
            newActive = parent;
        } else {
            return; // the top layer of the composition
        }

        comp.setActiveLayer(newActive, true, LayerMoveAction.RAISE_LAYER_SELECTION);

        assert Invariants.fadeWouldWorkOn(comp);
    }

    /**
     * Activates the layer below the current one.
     */
    default void activateLayerBelow() {
        assert isHolderOfActiveLayer();
        Composition comp = getComp();

        int newIndex = indexOf(comp.getActiveLayer()) - 1;
        if (newIndex < 0) {
            return;
        }

        comp.setActiveLayer(getLayer(newIndex), true,
            LayerMoveAction.LOWER_LAYER_SELECTION);

        assert Invariants.fadeWouldWorkOn(comp);
    }

    /**
     * Checks if the given layer can be merged down into the layer below it.
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
     * This method assumes that {@link #canMergeDown(Layer)} has previously returned true.
     */
    default void mergeDown(Layer layer) {
        Composition comp = getComp();
        int layerIndex = indexOf(layer);
        var belowLayer = (ImageLayer) getLayer(layerIndex - 1);

        // captured before the deletion, which can change the active layer
        // (and reset the mask view mode, which belongs to the active layer)
        Layer activeLayerBefore = comp.getActiveLayer();
        MaskViewMode maskViewModeBefore = comp.getMaskViewMode();

        var belowImage = belowLayer.getImage();
        var imageBefore = ImageUtils.copyImage(belowImage);

        // apply the effect of the merged layer to the image of the image layer
        Graphics2D g = belowImage.createGraphics();
        g.translate(-belowLayer.getTx(), -belowLayer.getTy());
        BufferedImage result = layer.render(g, belowImage, false);
        g.dispose();

        if (result != null && result != belowImage) {  // this was an adjustment that created a new image
            belowLayer.setImage(result);
        }

        belowLayer.updateIconImage();
        belowLayer.invalidateMaskedImageCache();

        deleteLayer(layer, false);

        History.add(new MergeDownEdit(this, layer, belowLayer, imageBefore,
            activeLayerBefore, maskViewModeBefore, layerIndex));
    }

    /**
     * Signals a change in the holder's content, requiring a visual update.
     * Implementations must invalidate cached images and trigger a re-rendering of the main composition.
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
     * Returns the root {@link Composition} object.
     */
    Composition getComp();

    String getName();


    /**
     * Converts the visible layers in this holder to a new layer group.
     */
    default void convertVisibleLayersToGroup() {
        int[] indices = IntStream.range(0, getNumLayers())
            .filter(i -> getLayer(i).isVisible())
            .toArray();
        if (indices.length == 0) {
            return;
        }

        convertToGroup(indices);
    }

    /**
     * Moves the layers at the given indices into a new layer group
     * and adds a history edit.
     * The indices must be non-empty and sorted in strictly ascending order.
     * The array is retained by the history edit, so it must not be modified later.
     */
    default void convertToGroup(int[] indices) {
        assert indices.length > 0;

        Composition comp = getComp();
        Layer activeBefore = comp.getActiveLayer();
        int groupIndex = calcGroupIndex(indices);

        var newGroup = new LayerGroup(comp, LayerGroup.generateName());
        moveToGroup(indices, groupIndex, newGroup);

        // must be created after the operation (the edit checks the invariants)
        History.add(new GroupingEdit(this, newGroup, indices, groupIndex, activeBefore, true));
    }

    /**
     * Groups the layers at the given indices into an existing group
     * instance, without adding history. Used by {@link GroupingEdit}.
     * The indices can be empty (an empty group), in which
     * case the group's position is given only by groupIndex.
     */
    default void regroup(int[] indices, int groupIndex, LayerGroup group) {
        assert indices.length == 0 || groupIndex == calcGroupIndex(indices);

        moveToGroup(indices, groupIndex, group);
    }

    private void moveToGroup(int[] indices, int groupIndex, LayerGroup group) {
        assert areStrictlyAscending(indices);

        List<Layer> movedLayers = new ArrayList<>(indices.length);
        for (int index : indices) {
            movedLayers.add(getLayer(index));
        }
        for (Layer layer : movedLayers) {
            removeDirectChild(layer, true);
        }

        group.setLayers(movedLayers);
        // restores the UI-level invariants (a no-op for a new group without UI)
        group.updateChildrenUI();

        // makes the group active, as the original operation did
        adder().atIndex(groupIndex).add(group);
    }

    // the group takes the place of the topmost moved layer
    private static int calcGroupIndex(int[] indices) {
        return indices[indices.length - 1] + 1 - indices.length;
    }

    private static boolean areStrictlyAscending(int[] indices) {
        for (int i = 1; i < indices.length; i++) {
            if (indices[i] <= indices[i - 1]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the layer to activate after the layer at the given index
     * was removed from this holder: the layer below it (or the one above,
     * if it was the bottom layer), or this holder if it became empty.
     * Must be called after the removal.
     */
    default Layer layerToActivateAfterRemoval(int removedIndex) {
        assert removedIndex >= 0 && removedIndex <= getNumLayers();

        if (getNumLayers() == 0) {
            assert canBeEmpty();
            // the cast is safe because only a Composition can't be empty
            return (CompositeLayer) this;
        }
        return getLayer(Math.max(removedIndex - 1, 0));
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
    void insertDirectChild(Layer newLayer, int index);

    /**
     * Removes a layer from the layer list.
     * This form of layer deletion allows temporarily violating the
     * constraint that some holders must always contain at least one layer.
     * This is just an internal helper method.
     */
    void removeDirectChild(Layer layer, boolean removeUI);
}
