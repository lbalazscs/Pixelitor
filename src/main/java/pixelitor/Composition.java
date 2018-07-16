/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor;

import pixelitor.gui.HistogramsPanel;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.*;
import pixelitor.io.OutputFormat;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Drawable;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerButton;
import pixelitor.layers.LayerMask;
import pixelitor.layers.MaskViewMode;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.selection.SelectionInteraction;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.Composition.LayerAdder.Position.ABOVE_ACTIVE;
import static pixelitor.Composition.LayerAdder.Position.BELLOW_ACTIVE;
import static pixelitor.Composition.LayerAdder.Position.TOP;
import static pixelitor.io.FileExtensionUtils.stripExtension;
import static pixelitor.utils.Utils.createCopyName;

/**
 * An image composition consisting of multiple layers
 */
public class Composition implements Serializable {
    // serialization is used for saving in the pxc format
    private static final long serialVersionUID = 1L;

    // a counter for the names of new layers
    private int newLayerCount = 1;

    private final List<Layer> layerList = new ArrayList<>();
    private Layer activeLayer;
    private String name; // the file name or something like "Untitled 1"

    private final Canvas canvas;

    //
    // the following variables are all transient, their state is not saved in PXC!
    //
    private transient File file;
    private transient boolean dirty = false;
    private transient boolean compositeImageUpToDate = false;
    private transient BufferedImage cachedCompositeImage = null;
    private transient ImageComponent ic;

    private transient Selection selection;

    // a temporary, new selection which is currently built
    // by dragging with a tool, but not finalized yet
    private transient Selection builtSelection;

    // A Composition can be created either with one of the following static
    // factory methods or through deserialization (pxc)

    /**
     * Creates a single-layered composition from the given image
     */
    public static Composition fromImage(BufferedImage img, File file, String name) {
        if (img == null) {
            return null;
        }

        img = ImageUtils.toSysCompatibleImage(img);
        Canvas canvas = new Canvas(img.getWidth(), img.getHeight());
        Composition comp = new Composition(canvas);
        comp.addBaseLayer(img);

        if (file != null) {
            comp.setFile(file); // also sets the name based on the file name
        } else if (name != null) {
            comp.setName(name);
        } else {
            // one of the file and name arguments must be given
            throw new IllegalArgumentException("no name could be set");
        }
        assert comp.getName() != null;
        return comp;
    }

    /**
     * Creates an empty composition (no layers, no name)
     */
    public static Composition createEmpty(int width, int height) {
        Canvas canvas = new Canvas(width, height);
        return new Composition(canvas);
    }

    public static Composition createCopy(Composition orig, boolean forUndo) {
        Canvas canvasCopy = new Canvas(orig.getCanvas());
        Composition compCopy = new Composition(canvasCopy);

        // copy layers
        for (Layer layer : orig.layerList) {
            Layer layerCopy = layer.duplicate(true);
            layerCopy.setComp(compCopy);

            compCopy.layerList.add(layerCopy);
            if (layer == orig.activeLayer) {
                compCopy.activeLayer = layerCopy;
            }
        }

        compCopy.newLayerCount = orig.newLayerCount;

        if (orig.selection != null) {
            compCopy.selection = new Selection(orig.selection, forUndo);
        }
        if (forUndo) {
            compCopy.dirty = orig.dirty;
            compCopy.file = orig.file;
            compCopy.name = orig.name;
            compCopy.ic = orig.ic;
            compCopy.cachedCompositeImage = null;
            compCopy.compositeImageUpToDate = false;
        } else {
            compCopy.dirty = true;
            compCopy.file = null;
            compCopy.name = createCopyName(stripExtension(orig.name));
            compCopy.ic = null;
            compCopy.cachedCompositeImage = orig.cachedCompositeImage;
            compCopy.compositeImageUpToDate = orig.compositeImageUpToDate;
        }

        assert compCopy.checkInvariant();

        return compCopy;
    }

    private Composition(Canvas canvas) {
        this.canvas = canvas;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        for (Layer layer : layerList) {
            if (layer instanceof ImageLayer) {
                ((ImageLayer) layer).updateIconImage();
            }
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                mask.getUI().addMaskIconLabel();
                mask.updateIconImage();
            }
        }
    }

    public void setIC(ImageComponent ic) {
        this.ic = ic;
        canvas.setIC(ic);

        if (selection != null) { // can happen when duplicating
            selection.setIC(ic);
        }
    }

    public boolean isEmpty() {
        return layerList.isEmpty();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public Rectangle getCanvasImBounds() {
        return canvas.getImBounds();
    }

    public int getCanvasImWidth() {
        return canvas.getImWidth();
    }

    public int getCanvasImHeight() {
        return canvas.getImHeight();
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return dirty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (ic != null) {
            ic.updateTitle();
        }
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        setName(file.getName());
    }

    private void addBaseLayer(BufferedImage baseLayerImage) {
        ImageLayer newLayer = new ImageLayer(this, baseLayerImage, null, null);

        addLayerInInitMode(newLayer);
    }

    /**
     * "Init mode" means that this is part of the composition construction,
     * the layer is not added as a result of a user interaction
     */
    public void addLayerInInitMode(Layer newLayer) {
        new LayerAdder(this).compInitMode().add(newLayer);
    }

    public ImageLayer addNewEmptyLayer(String name, boolean bellowActive) {
        ImageLayer newLayer = new ImageLayer(this, name);
        new LayerAdder(this)
                .withHistory("New Empty Layer")
                .atPosition(bellowActive ? BELLOW_ACTIVE : ABOVE_ACTIVE)
                .noRefresh()
                .add(newLayer);

        newLayer.updateIconImage();
        return newLayer;
    }

    public void addAllLayersToGUI() {
        assert checkInvariant();

        // when adding layer buttons the last layer always gets active
        // but here we don't want to change the selected layer
        Layer previousActiveLayer = activeLayer;

        layerList.forEach(this::addLayerToGUI);

        setActiveLayer(previousActiveLayer, false);
    }

    private void addLayerToGUI(Layer layer) {
        int layerIndex = layerList.indexOf(layer);
        ic.addLayerToGUI(layer, layerIndex);
    }

    public void duplicateActiveLayer() {
        Layer duplicate = activeLayer.duplicate(false);
        new LayerAdder(this)
                .withHistory("Duplicate Layer")
                .add(duplicate);
        assert checkInvariant();
    }

    public void mergeActiveLayerDown(boolean updateGUI) {
        assert checkInvariant();

        int activeIndex = layerList.indexOf(activeLayer);
        if (activeIndex > 0 && activeLayer.isVisible()) {
            Layer bellow = layerList.get(activeIndex - 1);
            if (bellow instanceof ImageLayer && bellow.isVisible()) {
                mergeActiveLayerToImageLayer(activeIndex, (ImageLayer) bellow, updateGUI);
            }
        }
    }

    private void mergeActiveLayerToImageLayer(int activeIndex,
                                              ImageLayer imageLayer,
                                              boolean updateGUI) {
        MaskViewMode maskViewModeBefore = ic.getMaskViewMode();
        BufferedImage imageBefore = ImageUtils.copyImage(imageLayer.getImage());

        // apply the effect of the merged layer to the image of the image layer
        BufferedImage bellowImage = imageLayer.getImage();
        Graphics2D g = bellowImage.createGraphics();
        g.translate(-imageLayer.getTX(), -imageLayer.getTY());
        BufferedImage result = activeLayer.applyLayer(g, bellowImage, false);
        if (result != null) {  // this was an adjustment
            imageLayer.setImage(result);
        }
        g.dispose();

        imageLayer.updateIconImage();

        // keep the reference because after deleting the
        // active merged layer, another layer will become active
        Layer mergedLayer = activeLayer;

        deleteActiveLayer(updateGUI, false);

        History.addEdit(new MergeDownEdit(this, mergedLayer,
                imageLayer, imageBefore, maskViewModeBefore, activeIndex));
    }

    private void deleteLayer(int layerIndex, boolean addToHistory) {
        Layer layer = layerList.get(layerIndex);
        deleteLayer(layer, addToHistory, true);
    }

    public void deleteActiveLayer(boolean updateGUI, boolean addToHistory) {
        deleteLayer(activeLayer, addToHistory, updateGUI);
    }

    public void deleteLayer(Layer layerToBeDeleted, boolean addToHistory, boolean updateGUI) {
        if (layerList.size() < 2) {
            throw new IllegalStateException("there are " + layerList.size() + " layers");
        }

        int layerIndex = layerList.indexOf(layerToBeDeleted);

        History.addEdit(addToHistory, () -> new DeleteLayerEdit(this, layerToBeDeleted, layerIndex));

        layerList.remove(layerToBeDeleted);

        if (layerToBeDeleted == activeLayer) {
            if (layerIndex > 0) {
                setActiveLayer(layerList.get(layerIndex - 1), false);
            } else {  // deleted the fist layer, set the new first layer as active
                setActiveLayer(layerList.get(0), false);
            }
        }

        if (updateGUI) {
            LayerButton button = layerToBeDeleted.getUI();
            ic.deleteLayerButton(button);

            if (isActiveComp()) {
                Layers.numLayersChanged(this, layerList.size());
            }

            imageChanged();
        }
    }

    public void setActiveLayer(Layer newActiveLayer, boolean addToHistory) {
        if (activeLayer == newActiveLayer) {
            return;
        }
        assert layerList.contains(newActiveLayer)
                : String.format("new active layer '%s' (%s) not in the layer list of '%s'",
                newActiveLayer.getName(), System.identityHashCode(newActiveLayer), getName());

        Layer oldLayer = activeLayer;
        activeLayer = newActiveLayer;

        // notify UI
        activeLayer.activateUI();
        Layers.activeLayerChanged(newActiveLayer);

        History.addEdit(addToHistory, () -> new LayerSelectionChangeEdit(this, oldLayer, newActiveLayer));

        assert checkInvariant();
    }

    public boolean isActive(Layer layer) {
        return layer == activeLayer;
    }

    public Layer getActiveLayer() {
        return activeLayer;
    }

    public int getActiveLayerIndex() {
        return layerList.indexOf(activeLayer);
    }

    public int getLayerIndex(Layer layer) {
        return layerList.indexOf(layer);
    }

    public Layer getLayer(int i) {
        return layerList.get(i);
    }

    public int getNumLayers() {
        return layerList.size();
    }

    public void forEachLayer(Consumer<Layer> action) {
        layerList.forEach(action);
    }

    public void forEachContentLayer(Consumer<ContentLayer> action) {
        for (Layer layer : layerList) {
            if (layer instanceof ContentLayer) {
                ContentLayer contentLayer = (ContentLayer) layer;
                action.accept(contentLayer);
            }
        }
    }

    public void forEachDrawable(Consumer<Drawable> action) {
        for (Layer layer : layerList) {
            if (layer instanceof ImageLayer) {
                action.accept((ImageLayer) layer);
            }
            if (layer.hasMask()) {
                action.accept(layer.getMask());
            }
        }
    }

    public void updateAllIconImages() {
        forEachDrawable(Drawable::updateIconImage);
    }

    public boolean activeIsDrawable() {
        if (activeLayer instanceof ImageLayer) {
            return true;
        }
        if (activeLayer.isMaskEditing()) {
            return true;
        }

        return false;
    }

    private Layer getActiveMaskOrLayer() {
        if (activeLayer.isMaskEditing()) {
            return activeLayer.getMask();
        }
        return activeLayer;
    }

    public ContentLayer getAnyContentLayer() {
        return (ContentLayer) layerList.stream()
                .filter(layer -> layer instanceof ContentLayer)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the active mask or image layer or null
     */
    public Drawable getActiveDrawableOrNull() {
        assert checkInvariant();
        if (activeLayer.isMaskEditing()) {
            return activeLayer.getMask();
        }
        if (activeLayer instanceof ImageLayer) {
            return (ImageLayer) activeLayer;
        }
        return null;
    }

    public Optional<Drawable> getActiveDrawable() {
        return Optional.ofNullable(getActiveDrawableOrNull());
    }

    /**
     * Returns the active mask or image layer.
     * Calling this method assumes that the active layer is a Drawable.
     */
    public Drawable getActiveDrawableOrThrow() {
        Drawable dr = getActiveDrawableOrNull();
        if (dr == null) {
            throw new IllegalStateException("The active layer is not an image layer or a mask, it is "
                    + activeLayer.getClass().getSimpleName());
        }
        return dr;
    }

    public void startMovement(boolean duplicateLayer) {
        if (duplicateLayer) {
            duplicateActiveLayer();
        }

        Layer layer = getActiveMaskOrLayer();
        layer.startMovement();
    }

    public void moveActiveContentRelative(double relX, double relY) {
        Layer layer = getActiveMaskOrLayer();
        layer.moveWhileDragging(relX, relY);
        imageChanged();
    }

    public void endMovement() {
        Layer layer = getActiveMaskOrLayer();
        PixelitorEdit edit = layer.endMovement();
        if (edit != null) {
            // The layer, the mask, or both moved.
            // We always should get here except if an adjustment
            // layer without a mask was moved.
            History.addEdit(edit);
            imageChanged();
        }
    }

    public void flattenImage(boolean updateGUI, boolean addToHistory) {
        if (updateGUI) {
            assert isActiveComp();
        }

        if (layerList.size() < 2) {
            return;
        }

        int numLayers = getNumLayers();
        BufferedImage bi = getCompositeImage();

        Layer flattenedLayer = new ImageLayer(this, bi, "flattened", null);
        new LayerAdder(this)
                .atIndex(numLayers) // add to the top
                .noRefresh()
                .add(flattenedLayer);

        for (int i = numLayers - 1; i >= 0; i--) { // delete the rest
            deleteLayer(i, false);
        }
        if (updateGUI) {
            Layers.numLayersChanged(this, 1);
        }
        if (addToHistory) {
            History.addEdit(new NotUndoableEdit("Flatten Image", this));
        }
    }

    public void moveActiveLayerUp() {
        assert checkInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        changeLayerOrder(oldIndex, oldIndex + 1, true);
    }

    public void moveActiveLayerDown() {
        assert checkInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        changeLayerOrder(oldIndex, oldIndex - 1, true);
    }

    public void moveActiveLayerToTop() {
        assert checkInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        int newIndex = layerList.size() - 1;
        changeLayerOrder(oldIndex, newIndex, true);
    }

    public void moveActiveLayerToBottom() {
        assert checkInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        changeLayerOrder(oldIndex, 0, true);
    }

    public void changeLayerOrder(int oldIndex, int newIndex, boolean addToHistory) {
        if (newIndex < 0) {
            return;
        }
        if (newIndex >= layerList.size()) {
            return;
        }
        if (oldIndex == newIndex) {
            return;
        }

        Layer layer = layerList.get(oldIndex);
        layerList.remove(oldIndex);
        layerList.add(newIndex, layer);

        ic.changeLayerButtonOrder(oldIndex, newIndex);
        imageChanged();
        Layers.layerOrderChanged(this);

        History.addEdit(addToHistory, () -> new LayerOrderChangeEdit(this, oldIndex, newIndex));
    }

    public void moveLayerSelectionUp() {
        int oldIndex = layerList.indexOf(activeLayer);

        int newIndex = oldIndex + 1;
        if (newIndex >= layerList.size()) {
            return;
        }
        setActiveLayer(layerList.get(newIndex), true);

        assert ConsistencyChecks.fadeWouldWorkOn(this);
    }

    public void moveLayerSelectionDown() {
        int oldIndex = layerList.indexOf(activeLayer);
        int newIndex = oldIndex - 1;
        if (newIndex < 0) {
            return;
        }

        setActiveLayer(layerList.get(newIndex), true);

        assert ConsistencyChecks.fadeWouldWorkOn(this);
    }

    public BufferedImage calculateCompositeImage() {
        // TODO why is this not working
//        if(layerList.size() == 1) {
//            Layer firstLayer = layerList.get(0);
//            if(firstLayer instanceof ImageLayer) {
//                ImageLayer layer = (ImageLayer) firstLayer;
//                if(!layer.isBigLayer()) {
//                    return layer.getImage();
//                }
//                return layer.getCanvasSizedSubImage();
//            }
//        }

//        BufferedImage imageSoFar = ImageUtils.createCompatibleImage(getCanvasWidth(), getCanvasHeight());

        BufferedImage imageSoFar = new BufferedImage(
                canvas.getImWidth(), canvas.getImHeight(), TYPE_INT_ARGB_PRE);
        Graphics2D g = imageSoFar.createGraphics();

        boolean firstVisibleLayer = true;
        for (Layer layer : layerList) {
            if (layer.isVisible()) {
                BufferedImage result = layer.applyLayer(g, imageSoFar, firstVisibleLayer);
                if (result != null) { // adjustment layer or watermarking text layer
                    imageSoFar = result;
                    if (g != null) {
                        g.dispose();
                    }
                    g = imageSoFar.createGraphics();
                }
                firstVisibleLayer = false;
            }
        }

        g.dispose();

        return imageSoFar;
    }

    public String generateNewLayerName() {
        String retVal = "layer " + newLayerCount;
        newLayerCount++;
        return retVal;
    }

    public void updateRegion(PPoint start, PPoint end, int thickness) {
        compositeImageUpToDate = false;
        if (ic != null) { // during reload image it can be null
            ic.updateRegion(start, end, thickness);
            ic.updateNavigator(false);
        }
    }

    public void dispose() {
        if (selection != null) {
            // stop the timer thread
            selection.die();
        }
    }

    public void addNewLayerFromComposite(String newLayerName) {
        ImageLayer newLayer = new ImageLayer(this, getCompositeImage(), newLayerName, null);

        new LayerAdder(this)
                .withHistory("New Layer from Composite")
                .noRefresh()
                .atIndex(layerList.size())
                .add(newLayer);
    }

    public ImageComponent getIC() {
        return ic;
    }

    public void paintSelection(Graphics2D g) {
        boolean ruby = false;
        if (ruby) {
            Shape allSelection = null;
            if (builtSelection != null) {
                allSelection = builtSelection.getShape();
            }
            if (selection != null) {
                if (allSelection == null) {
                    allSelection = selection.getShape();
                } else {
                    allSelection = SelectionInteraction.ADD.combine(allSelection, selection.getShape());
                }
            }
            if (allSelection != null) {
                Shape inverted = canvas.invertShape(allSelection);
                g.setComposite(AlphaComposite.SrcOver.derive(0.5f));
                g.setColor(Color.RED);
                g.fill(inverted);
            }
        } else {
            if (builtSelection != null) {
                builtSelection.paintMarchingAnts(g);
            }
            if (selection != null) {
                selection.paintMarchingAnts(g);
            }
        }
    }

    public void deselect(boolean addToHistory) {
        if (selection != null) {
            if (addToHistory) {
                DeselectEdit edit = createDeselectEdit();
                if (edit != null) {
                    History.addEdit(edit);
                }
            }

            boolean wasHidden = selection.isHidden();
            selection.die();
            selection = null;

            if (isActiveComp()) {
                if (wasHidden) {
                    SelectionActions.getShowHide()
                            .setHideName();
                }
                if (!ic.isMock()) {
                    SelectionActions.setEnabled(false, this);
                }
            } else {
                // we can get here from a DeselectEdit.redo on a non-active composition
            }
        }
    }

    public Shape clipShapeToCanvasSize(Shape shape) {
        return canvas.clipShapeToBounds(shape);
    }

    public DeselectEdit createDeselectEdit() {
        DeselectEdit edit = null;
        Shape shape = selection.getShape();
        if (shape != null) { // for a simple click without a previous selection this is null
            edit = new DeselectEdit(this, shape, "Composition.deselect");
        }
        return edit;
    }

    public void onSelection(Consumer<Selection> action) {
        if (selection != null) {
            action.accept(selection);
        }
    }

    public void transformSelection(Supplier<AffineTransform> at) {
        if (selection != null) {
            selection.transform(at.get());
        }
    }

    public Selection getSelection() {
        return selection;
    }

    public Shape getSelectionShape() {
        if (selection != null) {
            return selection.getShape();
        }
        return null;
    }

    public Selection getBuiltSelection() {
        return builtSelection;
    }

    public void setBuiltSelection(Selection selection) {
        this.builtSelection = selection;
    }

    // this is the "complete" method for setting a selection
    // from shape in the sense that it handles
    // everything: existing selections, history management
    public void setSelectionFromShapeComplete(Shape shape) {
        PixelitorEdit edit;
        shape = canvas.clipShapeToBounds(shape);
        if (shape.getBounds().isEmpty()) {
            // the new selection was outside the canvas
            return;
        }

        if (selection != null) {
            Shape backupSelectionShape = selection.getShape();
            selection.setShape(shape);
            edit = new SelectionChangeEdit("Selection Change", this, backupSelectionShape);
        } else {
            selection = createSelectionFromShape(shape);
            edit = new NewSelectionEdit(this, selection.getShape());
        }
        History.addEdit(edit);
    }

    public void setSelection(Selection selection) {
        this.selection = selection;
    }

    public void promoteSelection() {
        assert selection == null || !selection.isAlive() : "selection = " + selection;
        setNewSelection(builtSelection);
        setBuiltSelection(null);
    }

    public boolean hasSelection() {
        return (selection != null);
    }

    /**
     * Returns true if visually there are marching ants,
     * even if the selection is not yet finished
     */
    public boolean showsSelection() {
        return (selection != null || builtSelection != null);
    }

    /**
     * Sets the clip area on the given graphics according to the selection.
     * It is assumed that the graphics is relative to the canvas:
     * if it is coming from the image of an ImageLayer, then it must be
     * translated before calling this.
     */
    public void applySelectionClipping(Graphics2D g2) {
        if (selection != null) {
            Shape shape = selection.getShape();
            g2.setClip(shape);
        }
    }

    public void invertSelection() {
        if (selection != null) {
            Shape backupShape = selection.getShape();
            Shape inverted = canvas.invertShape(backupShape);
            selection.setShape(inverted);
            SelectionChangeEdit edit = new SelectionChangeEdit("Invert Selection", this, backupShape);
            History.addEdit(edit);
        }
    }

    public Selection createSelectionFromShape(Shape selectionShape) {
        if (selection != null) {
            throw new IllegalStateException("createSelectionFromShape called while there was a selection: " + selection
                    .toString());
        }
        setNewSelection(new Selection(selectionShape, ic));
        return selection;
    }

    public void setNewSelection(Selection selection) {
        assert selection != null;
        this.selection = selection;
        if (isActiveComp() && !ic.isMock()) {
            SelectionActions.setEnabled(true, this);
        }
    }

    /**
     * Returns the composite image which jas the same dimensions as the canvas.
     */
    public BufferedImage getCompositeImage() {
        if (compositeImageUpToDate) {
            return cachedCompositeImage; // this caching is useful for example when using the Color Picker Tool
        }

        cachedCompositeImage = calculateCompositeImage();

        compositeImageUpToDate = true;
        return cachedCompositeImage;
    }

    public void imageChanged() {
        imageChanged(FULL);
    }

    public void imageChanged(ImageChangeActions actions) {
        imageChanged(actions, false);
    }

    /**
     * The contents of this composition have been changed, the cache is invalidated,
     * and additional actions might be necessary
     */
    public void imageChanged(ImageChangeActions actions, boolean sizeChanged) {
        compositeImageUpToDate = false;

        if (actions.repaintNeeded()) {
            if (ic != null) {
                ic.repaint();
                ic.updateNavigator(sizeChanged);
            }
        }

        if (actions.histogramChanged()) {
            HistogramsPanel.INSTANCE.updateFromCompIfShown(this);
        }
    }

    private boolean isActiveComp() {
        return (ImageComponents.getActiveCompOrNull() == this);
    }

    public void activeLayerToCanvasSize() {
        if (!(activeLayer instanceof ImageLayer)) {
            Messages.showNotImageLayerError();
            return;
        }

        ImageLayer layer = (ImageLayer) this.activeLayer;
        BufferedImage backupImage = layer.getImage();

        TranslationEdit translationEdit = new TranslationEdit(this, layer, true);
        boolean changed = layer.cropToCanvasSize();

        if (changed) {
            ImageEdit imageEdit;
            String editName = "Layer to Canvas Size";

            boolean maskChanged = false;
            BufferedImage maskBackupImage = null;
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                maskBackupImage = mask.getImage();
                maskChanged = mask.cropToCanvasSize();
            }
            if (maskChanged) {
                imageEdit = new ImageAndMaskEdit(editName, this, layer, backupImage, maskBackupImage, false);
            } else {
                // no mask or no mask change, a simple ImageEdit will do
                imageEdit = new ImageEdit(editName, this, layer, backupImage, true, false);
                imageEdit.setFadeable(false);
            }
            History.addEdit(new LinkedEdit(editName, this, translationEdit, imageEdit));
        }
    }

    /**
     * The user reordered the layers by dragging
     */
    public void dragFinished(Layer layer, int newIndex) {
        layerList.remove(layer);
        layerList.add(newIndex, layer);
        imageChanged();
    }

    public void repaint() {
        ic.repaint();
    }

    /**
     * Applies the cropping to the selection
     */
    public void cropSelection(Rectangle2D cropRect) {
        if (selection != null) {
            Shape currentShape = selection.getShape();
            Shape intersection = SelectionInteraction.INTERSECT.combine(currentShape, cropRect);
            if (intersection.getBounds().isEmpty()) {
                selection.die();
                selection = null;
            } else {
                // the intersection has to be translated
                // into the coordinate system of the new, cropped image
                double txx = -cropRect.getX();
                double txy = -cropRect.getY();
                AffineTransform tx = AffineTransform.getTranslateInstance(txx, txy);
                selection.setShape(tx.createTransformedShape(intersection));
            }
        }
    }

    // called from assertions and unit tests
    @SuppressWarnings("SameReturnValue")
    public boolean checkInvariant() {
        if (layerList.isEmpty()) {
            throw new IllegalStateException("no layer in " + getName());
        }
        if (activeLayer == null) {
            throw new IllegalStateException("no active layer in " + getName());
        }
        if (!layerList.contains(activeLayer)) {
            throw new IllegalStateException("active layer (" + activeLayer.getName() + ") not in list (" + layerList
                    .toString() + ")");
        }
        return true;
    }

    public int getNumImageLayers() {
        int count = 0;
        for (Layer layer : layerList) {
            if (layer instanceof ImageLayer) {
                count++;
            }
        }
        return count;
    }

    public int calcNumImages() {
        int count = 0;
        for (Layer layer : layerList) {
            if (layer instanceof ImageLayer) {
                count++;
            }
            if (layer.hasMask()) {
                count++;
            }
        }
        return count;
    }

    public CompletableFuture<Void> saveAsync(File file,
                                             OutputFormat outputFormat,
                                             boolean addToRecentMenus) {
        System.out.println("Composition::saveAsync: CALLED, file = " + file.getAbsolutePath());

        Runnable saveTask = outputFormat.getSaveTask(this, file);
        return saveAsync(saveTask, file, addToRecentMenus);
    }

    public CompletableFuture<Void> saveAsync(Runnable saveTask,
                                             File file,
                                             boolean addToRecentMenus) {
        return CompletableFuture
                .runAsync(saveTask,
                        ThreadPool.getExecutor())
                .thenAcceptAsync(v -> afterSaveActions(file, addToRecentMenus),
                        EventQueue::invokeLater)
                .exceptionally(Messages::showExceptionOnEDT);
    }

    private void afterSaveActions(File file, boolean addToRecentMenus) {
        assert EventQueue.isDispatchThread();

        // perhaps for a multilayered image this should be
        // set only if it was saved in a layered format
        setDirty(false);

        setFile(file);
        if (addToRecentMenus) {
            RecentFilesMenu.getInstance().addFile(file);
        }
        Messages.showFileSavedMessage(file);
    }

    public enum ImageChangeActions {
        INVALIDATE_CACHE(false, false) {
        }, REPAINT(true, false) {
        }, HISTOGRAM(false, true) {
        }, FULL(true, true) {
        };

        private final boolean repaint;
        private final boolean updateHistogram;

        ImageChangeActions(boolean repaint, boolean updateHistogram) {
            this.repaint = repaint;
            this.updateHistogram = updateHistogram;
        }

        private boolean repaintNeeded() {
            return repaint;
        }

        private boolean histogramChanged() {
            return updateHistogram;
        }
    }

    @Override
    public String toString() {
        return "Composition{name='" + name + '\''
                + ", activeLayer=" + (activeLayer == null ? "null" : activeLayer.getName())
                + ", layerList=" + layerList
                + ", canvas=" + canvas
                + ", selection=" + selection
                + ", dirty=" + dirty + '}';
    }

    public static class LayerAdder {
        private final Composition comp;
        private String historyName; // null if the add should not be added to history
        private int newLayerIndex = -1;
        private boolean refresh = true;

        public enum Position {TOP, ABOVE_ACTIVE, BELLOW_ACTIVE}

        private Position position = TOP;
        private boolean compInit = false;

        public LayerAdder(Composition comp) {
            this.comp = comp;
        }

        public LayerAdder withHistory(String historyName) {
            this.historyName = historyName;
            return this;
        }

        public LayerAdder atPosition(Position position) {
            this.position = position;
            return this;
        }

        /**
         * Used during composition initialization
         */
        public LayerAdder compInitMode() {
            compInit = true;
            return this;
        }

        /**
         * Used when the composite image does not change
         */
        public LayerAdder noRefresh() {
            refresh = false;
            return this;
        }

        private void calcIndexBasedOnRelPosition() {
            int activeLayerIndex = comp.getActiveLayerIndex();
            if (activeLayerIndex == -1) { // no active layer yet
                assert comp.layerList.isEmpty();
                newLayerIndex = 0;
            } else {
                if (position == BELLOW_ACTIVE) {
                    newLayerIndex = activeLayerIndex;
                } else if (position == ABOVE_ACTIVE) {
                    newLayerIndex = activeLayerIndex + 1;
                } else {
                    throw new IllegalStateException("position = " + position);
                }
            }
        }

        public LayerAdder atIndex(int newLayerIndex) {
            this.newLayerIndex = newLayerIndex;
            return this;
        }

        public void add(Layer newLayer) {
            Layer activeLayerBefore = null;
            MaskViewMode oldViewMode = null;
            if (historyName != null) {
                activeLayerBefore = comp.activeLayer;
                oldViewMode = comp.ic.getMaskViewMode();
            }

            if (newLayerIndex == -1) { // no index was explicitly set
                if (position == TOP) {
                    newLayerIndex = comp.layerList.size();
                } else {
                    calcIndexBasedOnRelPosition();
                }
            }
            comp.layerList.add(newLayerIndex, newLayer);
            comp.setActiveLayer(newLayer, false);
            if (!compInit) {
                comp.setDirty(true);
                comp.ic.addLayerToGUI(newLayer, newLayerIndex);

                if (refresh) {
                    comp.imageChanged();
                }
            }
            if (historyName != null) {
                History.addEdit(new NewLayerEdit(historyName,
                        comp, newLayer, activeLayerBefore, oldViewMode));
            }
            assert comp.checkInvariant();
        }
    }
}
