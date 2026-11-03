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
import pixelitor.CopyOptions;
import pixelitor.compactions.FlipDirection;
import pixelitor.compactions.Outsets;
import pixelitor.compactions.QuadrantAngle;
import pixelitor.gui.utils.TaskAction;
import pixelitor.history.*;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Thumbnails;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static pixelitor.utils.Thumbnails.createThumbnail;

/**
 * A layer group that organizes multiple layers as a single entity in the layer stack.
 */
public class LayerGroup extends CompositeLayer {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<Layer> layers;

    private transient BufferedImage thumb;
    private transient boolean needsIconUpdate = false;

    // used only for isolated (non-passthrough) groups
    private transient BufferedImage cachedImage;

    private static int groupCounter = 0;

    public LayerGroup(Composition comp, String name) {
        this(comp, name, new ArrayList<>());
    }

    public LayerGroup(Composition comp, String name, List<Layer> layers) {
        super(comp, name);
        setLayers(layers);
        blendingMode = BlendingMode.PASS_THROUGH;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        cachedImage = null;
        thumb = null;
        needsIconUpdate = false;
    }

    public static String generateName() {
        return "layer group " + (++groupCounter);
    }

    @Override
    public void afterDeserialization() {
        // the cached image of an isolated group is rendered from its children,
        // so nested composite layers (smart objects, groups) must be
        // initialized first, otherwise their images would be missing
        for (Layer layer : layers) {
            if (layer instanceof CompositeLayer composite) {
                composite.afterDeserialization();
            }
        }
        recalcCachedImage();
    }

    @Override
    protected Layer createTypeSpecificCopy(CopyOptions options, Composition newComp) {
        String copyName = options.createLayerCopyName(name);
        List<Layer> layersCopy = new ArrayList<>();
        for (Layer layer : layers) {
            layersCopy.add(layer.copy(options, newComp));
        }
        return new LayerGroup(newComp, copyName, layersCopy);
    }

    public void setLayers(List<Layer> layers) {
        this.layers = new ArrayList<>(layers);
        for (Layer layer : layers) {
            layer.setHolder(this);
        }
    }

    @Override
    public int getTx() {
        // a layer group has no translation of its own: its children
        // carry translations, and its composite cache is canvas-sized
        return 0;
    }

    @Override
    public int getTy() {
        return 0;
    }

    /**
     * In the "Pass Through" blending mode the layers within the group
     * interact with all layers below the group in the layer stack as
     * if the group didn't exist.
     * If a group is isolated (not "Pass Through"), then the layers inside
     * the group will first blend with each other, and then the resulting
     * composite will blend with the layers below using the selected blending mode.
     */
    public boolean isPassThrough() {
        return blendingMode == BlendingMode.PASS_THROUGH;
    }

    @Override
    public boolean isRasterizable() {
        return !isPassThrough();
    }

    @Override
    public boolean canExportORAImage() {
        return false;
    }

    @Override
    public boolean isConvertibleToSmartObject() {
        return !isPassThrough() && !layers.isEmpty();
    }

    @Override
    public BufferedImage render(Graphics2D g, BufferedImage currentComposite, boolean firstVisibleLayer) {
        if (isPassThrough()) {
            return renderPassThrough(g, currentComposite, firstVisibleLayer);
        }

        // TODO Currently the layer mask of isolated
        //   (non-passthrough) groups is ignored.
        setupComposite(g, firstVisibleLayer);
        g.drawImage(getCachedImage(), 0, 0, null);

        return null;
    }

    private BufferedImage renderPassThrough(Graphics2D g, BufferedImage currentComposite, boolean firstVisibleLayer) {
        // Apply the layers as if they were directly in the parent holder.
        // The algorithm is similar to ImageUtils.calcComposite(),
        // but here we have to consider the existing state of the composition.
        BufferedImage initialComposite = currentComposite;
        Graphics2D currentG = g;

        for (Layer layer : layers) {
            if (!layer.isVisible()) {
                continue;
            }
            BufferedImage result = layer.render(currentG, currentComposite, firstVisibleLayer);
            if (result != null && result != currentComposite) { // adjustment layer or watermarking text layer
                currentComposite = result;
                currentG.dispose();
                currentG = currentComposite.createGraphics();
            }
            firstVisibleLayer = false;
        }

        if (currentG != g) {
            currentG.dispose();
        }

        return currentComposite != initialComposite ? currentComposite : null;
    }

    @Override
    public void update() {
        recalcCachedImage();
        super.update();
    }

    private void recalcCachedImage() {
        clearCachedImage();
        if (isPassThrough()) {
            return;
        }

        cachedImage = ImageUtils.calcComposite(layers, comp.getCanvas());
        if (needsIconUpdate) {
            needsIconUpdate = false;
            updateIconImage();
        }
    }

    @Override
    public void invalidateImageCache() {
        clearCachedImage();
        holder.invalidateImageCache();
    }

    private void clearCachedImage() {
        if (cachedImage != null) {
            cachedImage.flush();
            cachedImage = null;
        }
    }

    @Override
    public BufferedImage toImage(boolean applyMask, boolean applyOpacity) {
        // TODO This method currently ignores its arguments.
        //   (It was only implemented to support the
        //   shortcut in Composition.getCompositeImage.)
        if (isPassThrough()) {
            return ImageUtils.calcComposite(layers, comp.getCanvas());
        } else {
            return getCachedImage();
        }
    }

    private BufferedImage getCachedImage() {
        if (cachedImage == null) {
            recalcCachedImage();
        }
        return cachedImage;
    }

    @Override
    public void paint(Graphics2D g, boolean firstVisibleLayer) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected BufferedImage transformImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBlendingMode(BlendingMode newMode, boolean addToHistory, boolean update) {
        if (newMode == blendingMode) {
            return;
        }
        boolean wasPassThrough = isPassThrough();
        super.setBlendingMode(newMode, addToHistory, false);
        boolean isolationChanged = wasPassThrough != isPassThrough();

        if (isolationChanged) {
            // the cached image and the thumbnail depend on whether the group
            // is isolated, so they must be consistent even without an update
            thumb = null;
            clearCachedImage();
        }

        if (update) {
            if (isolationChanged) {
                recalcCachedImage();
                updateIconImage();
            }
            // no update(): the group's own composite doesn't
            // change, only how it's blended into its holder
            propertyChangeUpdate();
        }
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        // do nothing for the layer group itself
        // (the mask and layers are resized via forEachNestedLayer)
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle cropRect, boolean deleteCropped, boolean allowGrowing) {
        // do nothing for the group itself
    }

    @Override
    public void flip(FlipDirection direction, boolean layerTransform) {
        // do nothing for the group itself
    }

    @Override
    public void rotate(QuadrantAngle angle, boolean layerTransform) {
        // do nothing for the group itself
    }

    @Override
    public void rotate(double angleRadians, boolean layerTransform) {
        // do nothing for the group itself
    }

    @Override
    public void enlargeCanvas(Outsets out) {
        // do nothing for the group itself
    }

    @Override
    public int indexOf(Layer layer) {
        return layers.indexOf(layer);
    }

    @Override
    public int getNumLayers() {
        return layers.size();
    }

    @Override
    public Layer getLayer(int index) {
        return layers.get(index);
    }

    @Override
    public boolean hasDirectChild(Layer layer) {
        return layers.contains(layer);
    }

    @Override
    public Iterable<? extends Layer> directChildren() {
        return layers;
    }

    @Override
    public void insertDirectChild(Layer newLayer, int index) {
        layers.add(index, newLayer);
    }

    @Override
    public void removeDirectChild(Layer layer, boolean removeUI) {
        // unlike Composition.removeDirectChild, this never removes the layer's
        // UI, because a composite layer's child GUIs are handled differently
        layers.remove(layer);
    }

    @Override
    public void smartObjectChanged(boolean linked) {
        clearCachedImage();
        holder.smartObjectChanged(linked);
    }

    @Override
    public boolean canBeEmpty() {
        return true;
    }

    @Override
    public void updateIconImage() {
        if (!isPassThrough() && cachedImage == null) {
            // postponed until the cached image is calculated
            needsIconUpdate = true;
            return;
        }
        thumb = null;
        super.updateIconImage();
    }

    @Override
    public BufferedImage createIconThumbnail() {
        if (thumb != null) {
            return thumb;
        }

        if (isPassThrough()) {
            thumb = Thumbnails.createCircleThumb(new Color(0, 138, 0));
        } else if (cachedImage != null) {
            thumb = createThumbnail(cachedImage, thumbCheckerboardPainter);
        } else {
            throw new IllegalStateException(
                "isolated group '" + name + "' has no cached image");
        }

        return thumb;
    }

    @Override
    public void ungroup() {
        if (layers.isEmpty() && isTopLevel() && comp.getNumLayers() == 1) {
            String msg = "<html>The empty layer group <b>" + name + "</b> can't be ungrouped"
                + "<br>because a composition must always have at least one layer.";
            Messages.showInfo("Can't Ungroup", msg);
            return;
        }
        replaceWithUngrouped(null, true);
    }

    /**
     * Replaces this group with its children in the holder.
     *
     * @param ungroupedIndices the (ascending) indices where the children are inserted
     *                         (used by undo/redo), or null to insert them at the group's position
     * @param addToHistory     false if called from undo/redo
     */
    public void replaceWithUngrouped(int[] ungroupedIndices, boolean addToHistory) {
        assert ungroupedIndices == null || ungroupedIndices.length == layers.size();

        Layer activeBefore = comp.getActiveLayer();
        boolean activeWasThis = activeBefore == this;
        boolean activeWasInside = !activeWasThis && contains(activeBefore);

        int groupIndex = holder.indexOf(this);
        assert groupIndex != -1;
        holder.removeDirectChild(this, true);

        int numLayers = layers.size();
        int[] insertIndices = new int[numLayers];
        for (int i = 0; i < numLayers; i++) {
            Layer layer = layers.get(i);
            if (layer.hasUI()) {
                layer.getUI().detach();
            }

            insertIndices[i] = ungroupedIndices != null
                ? ungroupedIndices[i]
                : groupIndex + i;
            holder.adder()
                .atIndex(insertIndices[i])
                .skipCompUpdate() // the holder is updated once, below
                .add(layer);
        }

        if (activeWasInside) {
            // inserting the layers made the last one active
            activeBefore.activate();
        } else if (activeWasThis && numLayers == 0) {
            // the active layer disappeared together with the empty group
            comp.setActiveLayer(holder.layerToActivateAfterRemoval(groupIndex));
        }

        if (numLayers == 0) {
            // no insertion happened, so nothing else refreshed the holder's UI
            refreshHolderUI();
        }
        holder.update();

        assert holder.getNumLayers() > 0 || holder.canBeEmpty();
        assert comp.getActiveLayer().getTopLevelLayer() != this;
        assert comp.checkInvariants(); // also catches a dangling active layer in nested groups

        if (addToHistory) { // wasn't called from undo/redo
            History.add(new GroupingEdit(holder, this,
                insertIndices, groupIndex, activeBefore, false));
        }
    }

    // refreshes what LayerAdder would have refreshed if a child had been inserted
    private void refreshHolderUI() {
        if (isTopLevel()) {
            // removeDirectChild has already removed the group's UI from the view
            if (comp.isOpen()) {
                comp.fireLayerUICountChanged();
            }
        } else {
            // the removed group's UI is still in the parent's children panel
            ((CompositeLayer) holder).updateChildrenUI();
        }
    }

    @Override
    public Rectangle getContentBounds(boolean includeTransparent) {
        return Utils.calcCombinedBounds(layers, includeTransparent);
    }

    @Override
    public int getPixelAtPoint(Point p) {
        if (isPassThrough()) {
            throw new IllegalStateException(
                "pass-through group '" + name + "' must be hit-tested recursively");
        }
        // for an isolated group, get the pixel from its cached composite image
        return HitTesting.getPixelAt(this, getCachedImage(), p);
    }

    @Override
    public void prepareMovement() {
        super.prepareMovement();

        for (Layer layer : layers) {
            layer.prepareMovement();
        }
    }

    @Override
    public void moveWhileDragging(double imDx, double imDy) {
        super.moveWhileDragging(imDx, imDy);

        for (Layer layer : layers) {
            layer.moveWhileDragging(imDx, imDy);
        }

        invalidateImageCache();
    }

    @Override
    public PixelitorEdit finalizeMovement() {
        resetDragOffsets();

        List<PixelitorEdit> edits = new ArrayList<>(layers.size() + 1);
        edits.add(createLinkedMovementEdit()); // the mask edit, can be null
        for (Layer layer : layers) {
            edits.add(layer.finalizeMovement());
        }

        invalidateImageCache();

        return MultiEdit.combineNonNull(ContentLayerMoveEdit.NAME, comp, edits);
    }

    @Override
    PixelitorEdit createMovementEdit(int prevTx, int prevTy) {
        return null; // the group has no content of its own
    }

    @Override
    public LayerHolder getHolderForNewLayers() {
        return this;
    }

    @Override
    public JPopupMenu createLayerIconPopupMenu() {
        JPopupMenu popup = super.createLayerIconPopupMenu();

        popup.add(new TaskAction("Ungroup", this::ungroup));

        return popup;
    }

    @Override
    public boolean checkInvariants() {
        if (!super.checkInvariants()) {
            return false;
        }
        checkDirectChildLinks();
        // the children's own invariants are checked by the traversal in
        // Composition.checkInvariants(), so there is no recursion here
        if (isPassThrough() && cachedImage != null) {
            throw new AssertionError("pass-through group '" + name + "' has a cached image");
        }
        return true;
    }

    @Override
    public String getTypeString() {
        return "Layer Group";
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addBoolean("has cached image", cachedImage != null);
        node.addBoolean("has thumb", thumb != null);
        for (Layer layer : layers) {
            node.add(layer.createDebugNode());
        }

        return node;
    }
}
