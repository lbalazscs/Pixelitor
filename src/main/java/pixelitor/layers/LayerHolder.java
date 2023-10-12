/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import static pixelitor.layers.LayerAdder.Position.ABOVE_ACTIVE;

/**
 * Something that contains a list of layers, like a
 * composition, layer group or smart object.
 */
public interface LayerHolder extends Debuggable {
    int getActiveLayerIndex();

    int indexOf(Layer layer);

    int getNumLayers();

    Layer getLayer(int index);

    void addLayerToList(int index, Layer newLayer);

    boolean containsLayer(Layer layer);

    boolean containsLayerWithClass(Class<? extends Layer> clazz);

    default void addLayerNoUI(Layer newLayer) {
        adder().noUI().add(newLayer);
    }

    default void moveActiveLayer(boolean up) {
        assert isHolderOfActiveLayer();
        Layer activeLayer = getComp().getActiveLayer();
        String editName = up ? LayerMoveAction.RAISE_LAYER : LayerMoveAction.LOWER_LAYER;

        int index = indexOf(activeLayer);
        // if the layer is already at the edge of its current holder,
        // and that holder is a group, then get it out of the group
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
            changeLayerOrder(index, newIndex, true, editName);
        }
    }

    // moves the given layer from this holder into the given holder
    default void moveLayerInto(Layer layer, LayerHolder targetHolder, int targetIndex, String editName) {
        assert targetHolder != this;
        assert containsLayer(layer);
        assert !targetHolder.containsLayer(layer);
        assert targetIndex >= 0;

        if (editName != null) {
            int oldIndex = indexOf(layer);
            assert oldIndex >= 0;

            History.add(new ChangeHolderEdit(editName, layer, this, oldIndex, targetHolder, targetIndex));
        }

        deleteLayer(layer, false);
        targetHolder.adder()
            .atIndex(targetIndex)
            .add(layer);
    }

    default void moveActiveLayerToTop() {
        assert isHolderOfActiveLayer();

        int oldIndex = indexOf(getComp().getActiveLayer());
        int newIndex = getNumLayers() - 1;
        changeLayerOrder(oldIndex, newIndex,
            true, LayerMoveAction.LAYER_TO_TOP);
    }

    default void moveActiveLayerToBottom() {
        assert isHolderOfActiveLayer();

        int oldIndex = indexOf(getComp().getActiveLayer());
        changeLayerOrder(oldIndex, 0,
            true, LayerMoveAction.LAYER_TO_BOTTOM);
    }

    default void changeLayerOrder(int oldIndex, int newIndex) {
        changeLayerOrder(oldIndex, newIndex, false, null);
    }

    // Called when the layer order is changed by an action.
    // The GUI has to be updated.
    default void changeLayerOrder(int oldIndex, int newIndex,
                                  boolean addToHistory, String editName) {
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

        changeLayerGUIOrder(oldIndex, newIndex);
        update();
        Layers.layersReordered(this);

        if (addToHistory) {
            History.add(new LayerOrderChangeEdit(editName, this, oldIndex, newIndex));
        }
    }

    void changeLayerGUIOrder(int oldIndex, int newIndex);

    void insertLayer(Layer layer, int index, boolean update);

    void removeLayerFromList(Layer layer);

    void deleteLayer(Layer layer, boolean addToHistory);

    /**
     * This form of layer deletion allows to temporarily violate the
     * constraint that some holders must always contain at least one layer.
     */
    void deleteTemporarily(Layer layer);

    boolean allowsZeroLayers();

    /**
     * Replaces a layer with another, while keeping its position, mask, ui
     */
    void replaceLayer(Layer before, Layer after);

    default void raiseLayerSelection() {
        Composition comp = getComp();
        Layer activeLayer = comp.getActiveLayer();
        Layer newTarget;

        int oldIndex = indexOf(activeLayer);

        int newIndex = oldIndex + 1;
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

    default void lowerLayerSelection() {
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

    default boolean canMergeDown(Layer layer) {
        int index = indexOf(layer);
        if (index > 0 && layer.isVisible()) {
            Layer bellow = getLayer(index - 1);
            return bellow.getClass() == ImageLayer.class && bellow.isVisible();
        }
        return false;
    }

    // this method assumes that canMergeDown() previously returned true
    default void mergeDown(Layer layer) {
        int layerIndex = indexOf(layer);
        var bellowLayer = (ImageLayer) getLayer(layerIndex - 1);

        var bellowImage = bellowLayer.getImage();
        var maskViewModeBefore = getComp().getView().getMaskViewMode();
        var imageBefore = ImageUtils.copyImage(bellowImage);

        // apply the effect of the merged layer to the image of the image layer
        Graphics2D g = bellowImage.createGraphics();
        g.translate(-bellowLayer.getTx(), -bellowLayer.getTy());
        BufferedImage result = layer.applyLayer(g, bellowImage, false);
        if (result != null) {  // this was an adjustment
            bellowLayer.setImage(result);
        }
        g.dispose();

        bellowLayer.updateIconImage();

        deleteLayer(layer, false);

        History.add(new MergeDownEdit(this, layer,
            bellowLayer, imageBefore, maskViewModeBefore, layerIndex));
    }

    default boolean isHolderOfActiveLayer() {
        return getComp().getActiveHolder() == this;
    }

    void update(boolean updateHistogram);

    void update();

    void smartObjectChanged(boolean linked);

    String getORAStackXML();

    Composition getComp();

    String getName();

    void invalidateImageCache();

    /**
     * Return a Stream of layers at this level.
     */
    Stream<? extends Layer> levelStream();

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

    default void convertToGroup(int[] indices, LayerGroup target, boolean addHistory) {
        List<Layer> movedLayers = new ArrayList<>(indices.length);
        for (int index : indices) {
            movedLayers.add(getLayer(index));
        }
        for (Layer layer : movedLayers) {
            deleteTemporarily(layer);
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
            newGroup = new LayerGroup(getComp(), LayerGroup.createName(), movedLayers);
        }

        int lastMovedIndex = indices[indices.length - 1];
        int newIndex = lastMovedIndex + 1 - indices.length;
        adder().atIndex(newIndex).add(newGroup);

        if (addHistory) {
            History.add(new GroupingEdit(this, newGroup, indices, true));
        }
    }

    default void addEmptyGroup() {
        LayerGroup group = new LayerGroup(getComp(), LayerGroup.createName());
        new LayerAdder(this)
            .atPosition(ABOVE_ACTIVE)
            .withHistory("New Layer Group")
            .add(group);
    }

    default LayerAdder adder() {
        return new LayerAdder(this);
    }
}
