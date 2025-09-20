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
import pixelitor.CopyType;
import pixelitor.compactions.FlipDirection;
import pixelitor.compactions.Outsets;
import pixelitor.compactions.QuadrantAngle;
import pixelitor.gui.utils.TaskAction;
import pixelitor.history.*;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static pixelitor.Views.thumbSize;
import static pixelitor.utils.ImageUtils.createThumbnail;

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
        recalcCachedImage();
    }

    @Override
    protected Layer createTypeSpecificCopy(CopyType copyType, Composition newComp) {
        String copyName = copyType.createLayerCopyName(name);
        List<Layer> layersCopy = new ArrayList<>();
        for (Layer layer : layers) {
            layersCopy.add(layer.copy(copyType, true, newComp));
        }
        return new LayerGroup(comp, copyName, layersCopy);
    }

    public void setLayers(List<Layer> layers) {
        this.layers = new ArrayList<>(layers);
        for (Layer layer : layers) {
            layer.setHolder(this);
        }
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
        g.setComposite(blendingMode.getComposite(getOpacity()));
        g.drawImage(getCachedImage(), 0, 0, null);

        return currentComposite;
    }

    private BufferedImage renderPassThrough(Graphics2D g, BufferedImage currentComposite, boolean firstVisibleLayer) {
        // Apply the layers as if they were directly in the parent holder.
        // The algorithm is similar to ImageUtils.calcComposite(),
        // but here we have to consider the existing state of the composition.
        for (Layer layer : layers) {
            if (!layer.isVisible()) {
                continue;
            }
            BufferedImage result = layer.render(g, currentComposite, firstVisibleLayer);
            if (result != null) { // adjustment layer or watermarking text layer
                currentComposite = result;
                g.dispose();
                g = currentComposite.createGraphics();
            }
            firstVisibleLayer = false;
        }
        return currentComposite;
    }

    @Override
    public void update(boolean updateHistogram) {
        recalcCachedImage();
        holder.update(updateHistogram);
    }

    private void recalcCachedImage() {
        if (isPassThrough()) {
            cachedImage = null;
        } else {
            cachedImage = ImageUtils.calcComposite(layers, comp.getCanvas());
            if (needsIconUpdate) {
                updateIconImage();
                needsIconUpdate = false;
            }
        }
    }

    @Override
    public void invalidateImageCache() {
        if (cachedImage != null) {
            cachedImage.flush();
            cachedImage = null;
        }
        holder.invalidateImageCache();
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

        if (update) {
            if (wasPassThrough != isPassThrough()) {
                recalcCachedImage();
                thumb = null;
                updateIconImage();
            }
            update();
        }
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        // do nothing for the layer group itself
        // (the mask and layers are resized via forEachNestedLayer)
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCropped, boolean allowGrowing) {
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
    public void enlargeCanvas(Outsets out) {
        // do nothing for the group itself
    }

    @Override
    public void forEachNestedLayer(Consumer<Layer> action, boolean includeMasks) {
        for (Layer layer : layers) {
            layer.forEachNestedLayer(action, includeMasks);
        }

        // run on itself only after the childern
        // (this ordering is needed for initialization)
        action.accept(this);
        if (includeMasks && hasMask()) {
            action.accept(getMask());
        }
    }

    @Override
    public Layer findFirstLayerWhere(Predicate<Layer> predicate, boolean includeMasks) {
        // search through all the child layers first to be consistent with forEachNestedLayer
        for (Layer layer : layers) {
            Layer foundLayer = layer.findFirstLayerWhere(predicate, includeMasks);
            if (foundLayer != null) {
                return foundLayer;
            }
        }

        if (predicate.test(this)) {
            return this;
        }

        if (includeMasks && hasMask() && predicate.test(getMask())) {
            return getMask();
        }

        return null;
    }

    @Override
    public boolean contains(Layer target) {
        if (target == this) {
            return true;
        }
        for (Layer layer : layers) {
            if (layer.contains(target)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsLayerOfType(Class<? extends Layer> type) {
        if (type.isInstance(this)) {
            return true;
        }
        for (Layer layer : layers) {
            if (layer.containsLayerOfType(type)) {
                return true;
            }
        }
        return false;
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
    public boolean listContainsLayer(Layer layer) {
        return layers.contains(layer);
    }

    @Override
    public Stream<? extends Layer> directChildrenStream() {
        return layers.stream();
    }

    @Override
    public void addLayerToList(Layer newLayer, int index) {
        layers.add(index, newLayer);
    }

    @Override
    public void reorderLayerUI(int oldIndex, int newIndex) {
        updateChildrenUI();
    }

    @Override
    public void removeLayerFromList(Layer layer) {
        layers.remove(layer);
    }

    @Override
    public void deleteLayer(Layer layer, boolean addToHistory) {
        assert layer.getComp() == comp;
        assert layer.getHolder() == this;

        int layerIndex = layers.indexOf(layer);

        if (addToHistory) {
            History.add(new DeleteLayerEdit(this, layer, layerIndex));
        }

        layers.remove(layer);

        if (layer.isActive()) {
            if (layers.isEmpty()) {
                comp.setActiveLayer(this);
            } else if (layerIndex > 0) {
                comp.setActiveLayer(layers.get(layerIndex - 1));
            } else {  // deleted the fist layer, set the new first layer as active
                comp.setActiveLayer(layers.getFirst());
            }
        }

        updateChildrenUI();
        update();
    }

    @Override
    public void deleteInternal(Layer layer) {
        layers.remove(layer);
    }

    @Override
    public void insertLayer(Layer layer, int index, boolean update) {
        if (update) {
            new LayerAdder(this).atIndex(index).add(layer);
        } else {
            layers.add(index, layer);
        }
    }

    @Override
    public void smartObjectChanged(boolean linked) {
        cachedImage = null;
        holder.smartObjectChanged(linked);
    }

    @Override
    public boolean canBeEmpty() {
        return true;
    }

    @Override
    public void replaceLayer(Layer before, Layer after) {
        boolean containedActive = before.contains(comp.getActiveLayer());

        before.transferMaskAndUITo(after);

        int layerIndex = indexOf(before);
        assert layerIndex != -1;
        layers.set(layerIndex, after);

        if (containedActive) {
            // TODO see comments in Composition.replaceLayer
            comp.setActiveLayer(after);
        }
        comp.checkInvariants();
    }

    @Override
    public void updateIconImage() {
        if (!isPassThrough()) {
            if (cachedImage == null) {
                needsIconUpdate = true; // postpone
                return;
            }
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
            thumb = ImageUtils.createCircleThumb(new Color(0, 138, 0));
        } else if (cachedImage != null) {
            thumb = createThumbnail(cachedImage, thumbSize, thumbCheckerBoardPainter);
        } else {
            // isolated groups should always have a cached image
            throw new IllegalStateException();
//            thumb = ImageUtils.createCircleThumb(new Color(0, 0, 203));
        }

        return thumb;
    }

    @Override
    public void unGroup() {
        if (layers.isEmpty() && isTopLevel() && comp.getNumLayers() == 1) {
            String msg = "<html>The empty layer group <b>" + name + "</b> can't be ungrouped"
                + "<br>because a composition must always have at least one layer.";
            Messages.showInfo("Can't Ungroup", msg);
            return;
        }
        replaceWithUnGrouped(null, true);
    }

    public void replaceWithUnGrouped(int[] prevIndices, boolean addToHistory) {
        Layer activeBefore = comp.getActiveLayer();
        boolean activeWasThis = this == activeBefore;
        boolean activeWasInside = !activeWasThis && contains(activeBefore);

        int indexInHolder = holder.indexOf(this);
        holder.deleteInternal(this);

        int numLayers = layers.size();
        int[] insertIndices = new int[numLayers];
        for (int i = 0; i < numLayers; i++) {
            Layer layer = layers.get(i);

            layer.getUI().detach();

            int insertIndex = prevIndices != null ? prevIndices[i] : indexInHolder;
            holder.insertLayer(layer, insertIndex, true);
            insertIndices[i] = insertIndex;
            indexInHolder++;
        }

        if (activeWasInside) {
            // inserting the layers into the parent will make
            // the last inserted layer active
            activeBefore.activate();
        }

        assert comp.getActiveTopLevelLayer() != this;

        if (addToHistory) { // wasn't called from undo
            History.add(new GroupingEdit(holder, this, insertIndices, activeBefore, false));
        }
    }

    @Override
    public Rectangle getContentBounds(boolean includeTransparent) {
        return Utils.calcCombinedBounds(layers, includeTransparent);
    }

    @Override
    public int getPixelAtPoint(Point p) {
        if (isPassThrough()) {
            // should be handled recursively
            throw new IllegalStateException();
        } else {
            // for an isolated group, get the pixel from its cached composite image
            return ImageUtils.getPixelAt(this, getCachedImage(), p);
        }
    }

    @Override
    public ContentLayer findOpaqueLayerAtPoint(Point p) {
        // a small opacity makes the layer effectively invisible for hit-testing.
        if (!isVisible() || getOpacity() < 0.05f) {
            return null;
        }

        if (isPassThrough()) {
            // iterate in reverse order to search layers from top to bottom
            return ContentLayer.findOpaqueInList(layers, p);
        } else {
            // for an isolated group, treat it as a single entity
            return super.findOpaqueLayerAtPoint(p);
        }
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
        MultiEdit fullEdit = new MultiEdit(ContentLayerMoveEdit.NAME, comp);
        PixelitorEdit maskEdit = createLinkedMovementEdit();
        if (maskEdit != null) {
            fullEdit.add(maskEdit);
        }
        for (Layer layer : layers) {
            PixelitorEdit layerEdit = layer.finalizeMovement();
            if (layerEdit != null) {
                fullEdit.add(layerEdit);
            }
        }

        invalidateImageCache();

        if (fullEdit.isEmpty()) {
            return null;
        }
        return fullEdit;
    }

    @Override
    PixelitorEdit createMovementEdit(int prevTx, int prevTy) {
        return null; // the group has no content of its own
    }

    @Override
    public void setComp(Composition comp) {
        super.setComp(comp);

        for (Layer layer : layers) {
            layer.setComp(comp);
        }
    }

    @Override
    public LayerHolder getHolderForNewLayers() {
        return this;
    }

    @Override
    public JPopupMenu createLayerIconPopupMenu() {
        JPopupMenu popup = super.createLayerIconPopupMenu();
        if (popup == null) {
            popup = new JPopupMenu();
        }

        popup.add(new TaskAction("Ungroup", () ->
            replaceWithUnGrouped(null, true)));

        return popup;
    }

    @Override
    public boolean checkInvariants() {
        if (!super.checkInvariants()) {
            return false;
        }
        for (Layer layer : layers) {
            if (layer.getComp() != comp) {
                throw new AssertionError("bad comp in layer '%s' (that comp='%s', this='%s')".formatted(
                    layer.getName(), layer.getComp().getDebugName(), comp.getDebugName()));
            }
            if (layer.getHolder() != this) {
                throw new AssertionError("bad holder in layer '%s' (that holder='%s', this='%s')".formatted(
                    layer.getName(), layer.getHolder().getName(), getName()));
            }
            assert layer.checkInvariants();
        }
        return true;
    }

    @Override
    public String getORAStackXML() {
        return "<stack composite-op=\"%s\" name=\"%s\" opacity=\"%f\" visibility=\"%s\" isolation=\"%s\">\n".formatted(
            blendingMode.toSVGName(), getName(), getOpacity(), getVisibilityAsORAString(),
            blendingMode == BlendingMode.PASS_THROUGH ? "auto" : "isolate");
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

