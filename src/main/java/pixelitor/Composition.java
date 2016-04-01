/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.history.AddToHistory;
import pixelitor.history.DeleteLayerEdit;
import pixelitor.history.DeselectEdit;
import pixelitor.history.History;
import pixelitor.history.ImageAndMaskEdit;
import pixelitor.history.ImageEdit;
import pixelitor.history.LayerOrderChangeEdit;
import pixelitor.history.LayerSelectionChangeEdit;
import pixelitor.history.LinkedEdit;
import pixelitor.history.NewLayerEdit;
import pixelitor.history.NotUndoableEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.history.TranslationEdit;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerButton;
import pixelitor.layers.LayerMask;
import pixelitor.layers.MaskViewMode;
import pixelitor.selection.IgnoreSelection;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.selection.SelectionInteraction;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.UpdateGUI;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.Composition.ImageChangeActions.INVALIDATE_CACHE;
import static pixelitor.io.FileExtensionUtils.getFileNameWOExtension;
import static pixelitor.utils.Utils.createCopyName;

/**
 * An image composition consisting of multiple layers
 */
public class Composition implements Serializable {
    // serialization is used for save in the pxc format
    private static final long serialVersionUID = 1L;

    // a counter for the names of new layers
    private int newLayerCount = 1;

    private final List<Layer> layerList = new ArrayList<>();
    private Layer activeLayer;
    private String name; // the file name or something like "Untitled 1"

    private Canvas canvas;

    //
    // the following variables are all transient, their state is not saved in PXC!
    //
    private transient File file;
    private transient boolean dirty = false;
    private transient boolean compositeImageUpToDate = false;
    private transient BufferedImage cachedCompositeImage = null;
    private transient ImageComponent ic;

    private transient Selection selection;
    private transient Selection builtSelection;

    // A Composition can be created either with one of the following static
    // factory methods or through deserialization (pxc)

    /**
     * Creates a single-layered composition from the given image
     */
    public static Composition fromImage(BufferedImage img, File file, String name) {
        assert img != null;

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
        return comp;
    }

    /**
     * Creates an empty composition (no layers, no name)
     */
    public static Composition createEmpty(int width, int height) {
        Canvas canvas = new Canvas(width, height);
        Composition comp = new Composition(canvas);
        return comp;
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
            compCopy.name = createCopyName(getFileNameWOExtension(orig.name));
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

        //noinspection Convert2streamapi
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

    /**
     * Called when deserialized and when duplicated
     */
    public void setIC(ImageComponent ic) {
        this.ic = ic;
        canvas.setIc(ic);

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

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    public Rectangle getCanvasBounds() {
        return canvas.getBounds();
    }

    public int getCanvasWidth() {
        return canvas.getWidth();
    }

    public int getCanvasHeight() {
        return canvas.getHeight();
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

        addLayerNoGUI(newLayer);
        activeLayer = newLayer;
    }

    /**
     * Adds a layer to the top without updating any GUI
     */
    public void addLayerNoGUI(Layer newLayer) {
        layerList.add(newLayer); // adds it to top, ignoring the active layer position
        setActiveLayer(newLayer, AddToHistory.NO);

        // doesn't set the dirty flag because
        // this method is used when adding the base layer
    }

    public ImageLayer addNewEmptyLayer(String name, boolean bellowActive) {
        ImageLayer newLayer = new ImageLayer(this, name);
        addLayer(newLayer, AddToHistory.YES, "New Empty Layer", false, bellowActive);
        newLayer.updateIconImage();
        return newLayer;
    }

    public void addLayer(Layer newLayer, AddToHistory addToHistory, String historyName,
                         boolean updateHistogram, boolean bellowActive) {
        int activeLayerIndex = layerList.indexOf(activeLayer);
        int newLayerIndex;

        if (activeLayerIndex == -1) { // no active layer yet
            assert layerList.isEmpty();
            newLayerIndex = 0;
        } else {
            if (bellowActive) {
                newLayerIndex = activeLayerIndex;
            } else {
                newLayerIndex = activeLayerIndex + 1;
            }
        }

        addLayer(newLayer, addToHistory, historyName, updateHistogram, newLayerIndex);
    }

    /**
     * Adds the specified layer at the specified layer position
     */
    public void addLayer(Layer newLayer, AddToHistory addToHistory, String historyName, boolean updateHistogram, int newLayerIndex) {
        Layer activeLayerBefore = activeLayer;
        MaskViewMode oldViewMode = ic.getMaskViewMode();

        layerList.add(newLayerIndex, newLayer);
        setActiveLayer(newLayer, AddToHistory.NO);
        ic.addLayerToGUI(newLayer, newLayerIndex);

        History.addEdit(addToHistory, () -> new NewLayerEdit(this, newLayer, activeLayerBefore, historyName, oldViewMode));

        if (updateHistogram) {
            imageChanged(FULL); // if the histogram is updated, a repaint is also necessary
        } else {
            imageChanged(INVALIDATE_CACHE);
        }
        assert checkInvariant();
    }

    public void addLayersToGUI() {
        assert checkInvariant();

        // when adding layer buttons the last layer always gets active
        // but here we don't want to change the selected layer
        Layer previousActiveLayer = activeLayer;

        layerList.forEach(this::addLayerToGUI);

        setActiveLayer(previousActiveLayer, AddToHistory.NO);
    }

    private void addLayerToGUI(Layer layer) {
        int layerIndex = layerList.indexOf(layer);
//        setActiveLayer(layer, false);
        ic.addLayerToGUI(layer, layerIndex);
    }

    public void duplicateActiveLayer() {
        Layer duplicate = activeLayer.duplicate(false);
        addLayer(duplicate, AddToHistory.YES, "Duplicate Layer", true, false);
        assert checkInvariant();
    }

    public void mergeDown(UpdateGUI updateGUI) {
        assert checkInvariant();

        int activeIndex = layerList.indexOf(activeLayer);
        if (activeIndex > 0 && activeLayer.isVisible()) {
            Layer bellow = layerList.get(activeIndex - 1);
            if (bellow instanceof ImageLayer) {
                ImageLayer imageLayerBellow = (ImageLayer) bellow;
                if (imageLayerBellow.isVisible()) {
                    // TODO for adjustment effects it is not necessary to copy
                    BufferedImage backupImage = ImageUtils.copyImage(imageLayerBellow.getImage());

                    activeLayer.mergeDownOn(imageLayerBellow);
                    imageLayerBellow.updateIconImage();
                    Layer mergedLayer = activeLayer;

                    deleteActiveLayer(updateGUI, AddToHistory.NO);

                    PixelitorEdit edit = new LinkedEdit(this, "Merge Down",
                            new ImageEdit(this, "", imageLayerBellow, backupImage, IgnoreSelection.YES, false),
                            new DeleteLayerEdit(this, mergedLayer, activeIndex)
                    );

                    History.addEdit(edit);
                }
            }
        }
    }

    private void deleteLayer(int layerIndex) {
        Layer layer = layerList.get(layerIndex);
        deleteLayer(layer, AddToHistory.YES, UpdateGUI.YES);
    }

    public void deleteActiveLayer(UpdateGUI updateGUI, AddToHistory addToHistory) {
        deleteLayer(activeLayer, addToHistory, updateGUI);
    }

    public void deleteLayer(Layer layerToBeDeleted, AddToHistory addToHistory, UpdateGUI updateGUI) {
        if (layerList.size() < 2) {
            throw new IllegalStateException("there are " + layerList.size() + " layers");
        }

        int layerIndex = layerList.indexOf(layerToBeDeleted);

        History.addEdit(addToHistory, () -> new DeleteLayerEdit(this, layerToBeDeleted, layerIndex));

        layerList.remove(layerToBeDeleted);

        if (layerToBeDeleted == activeLayer) {
            if (layerIndex > 0) {
                setActiveLayer(layerList.get(layerIndex - 1), AddToHistory.NO);
            } else {  // deleted the fist layer, set the new first layer as active
                setActiveLayer(layerList.get(0), AddToHistory.NO);
            }
        }

        if (updateGUI.isYes()) {
            LayerButton button = layerToBeDeleted.getUI().getLayerButton();
            ic.deleteLayerButton(button);

            if (isActiveComp()) {
                AppLogic.activeCompLayerCountChanged(this, layerList.size());
            }

            imageChanged(FULL);
        }
    }

    public void setActiveLayer(Layer newActiveLayer, AddToHistory addToHistory) {
        if (activeLayer != newActiveLayer) {
            assert layerList.contains(newActiveLayer)
                    : String.format("new active layer '%s' (%s) not in the layer list of '%s'",
                    newActiveLayer.getName(), System.identityHashCode(newActiveLayer), getName());

            Layer oldLayer = activeLayer;
            activeLayer = newActiveLayer;

            // notify UI
            activeLayer.activateUI();
            AppLogic.activeLayerChanged(newActiveLayer);

            // notify history
            History.addEdit(addToHistory, () -> new LayerSelectionChangeEdit(this, oldLayer, newActiveLayer));
            assert checkInvariant();
        }
    }

    public boolean isActiveLayer(Layer layer) {
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

    public int getNrLayers() {
        return layerList.size();
    }

    public void updateAllLayerIconImages() {
        for (Layer layer : layerList) {
            if (layer instanceof ImageLayer) {
                ((ImageLayer) layer).updateIconImage();
            }
            if (layer.hasMask()) {
                layer.getMask().updateIconImage();
            }
        }
    }

    public boolean activeIsImageLayerOrMask() {
        if (activeLayer instanceof ImageLayer) {
            return true;
        }
        if (activeLayer.isMaskEditing()) {
            return true;
        }

        return false;
    }

    public Layer getActiveMaskOrLayer() {
        if (activeLayer.isMaskEditing()) {
            return activeLayer.getMask();
        }
        return activeLayer;
    }

    public ContentLayer getAnyContentLayer() {
        for (Layer layer : layerList) {
            if (layer instanceof ContentLayer) {
                ContentLayer contentLayer = (ContentLayer) layer;
                return contentLayer;
            }
        }
        return null;
    }

    public ImageLayer getActiveMaskOrImageLayerOrNull() {
        assert checkInvariant();
        if (activeLayer.isMaskEditing()) {
            return activeLayer.getMask();
        }
        if (activeLayer instanceof ImageLayer) {
            ImageLayer imageLayer = (ImageLayer) activeLayer;
            return imageLayer;
        }
        return null;
    }

    /**
     * This method assumes that the active layer is an image layer
     */
    public ImageLayer getActiveMaskOrImageLayer() {
        ImageLayer layer = getActiveMaskOrImageLayerOrNull();
        if (layer == null) {
            throw new IllegalStateException("active layer is not image layer or mask, it is "
                    + activeLayer.getClass().getSimpleName());
        }
        return layer;
    }

    /**
     * This should be called if the active layer might not be an image layer
     */
    public Optional<ImageLayer> getActiveMaskOrImageLayerOpt() {
        ImageLayer layer = getActiveMaskOrImageLayerOrNull();
        if (layer == null) {
            return Optional.empty();
        }
        return Optional.of(layer);
    }

    public void startMovement(boolean duplicateLayer) {
        if (duplicateLayer) {
            duplicateActiveLayer();
        }

        Layer layer = getActiveMaskOrLayer();
        layer.startMovement();
    }

    public void moveActiveContentRelative(double relativeX, double relativeY) {
        Layer layer = getActiveMaskOrLayer();
        layer.moveWhileDragging(relativeX, relativeY);
        imageChanged(FULL);
    }

    public void endMovement() {
        Layer layer = getActiveMaskOrLayer();
        PixelitorEdit edit = layer.endMovement();
        if (edit != null) {
            // The layer, the mask, or both moved.
            // We always should get here except if an adjustment
            // layer without a mask was moved.
            History.addEdit(edit);
            imageChanged(FULL);
        }
    }

    public void flattenImage(UpdateGUI updateGUI) {
        if (updateGUI.isYes()) {
            assert isActiveComp();
        }

        if (layerList.size() < 2) {
            return;
        }

        int nrLayers = getNrLayers();
        BufferedImage bi = getCompositeImage();

        Layer flattenedLayer = new ImageLayer(this, bi, "flattened", null);
        addLayer(flattenedLayer, AddToHistory.NO, null, false, nrLayers); // add to the top

        for (int i = nrLayers - 1; i >= 0; i--) { // delete the rest
            deleteLayer(i);
        }
        if (updateGUI.isYes()) {
            AppLogic.activeCompLayerCountChanged(this, 1);

            // TODO should have a separate add to history argument?
            History.addEdit(new NotUndoableEdit(this, "Flatten Image"));
        }
    }

    public void moveActiveLayerUp() {
        assert checkInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        swapLayers(oldIndex, oldIndex + 1, AddToHistory.YES);
    }

    public void moveActiveLayerDown() {
        assert checkInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        swapLayers(oldIndex, oldIndex - 1, AddToHistory.YES);
    }

    public void moveActiveLayerToTop() {
        assert checkInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        int newIndex = layerList.size() - 1;
        swapLayers(oldIndex, newIndex, AddToHistory.YES);
    }

    public void moveActiveLayerToBottom() {
        assert checkInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        swapLayers(oldIndex, 0, AddToHistory.YES);
    }

    public void swapLayers(int oldIndex, int newIndex, AddToHistory addHistory) {
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
        layerList.remove(layer);
        layerList.add(newIndex, layer);

        ic.changeLayerOrderInTheGUI(oldIndex, newIndex);
        imageChanged(FULL);
        AppLogic.layerOrderChanged(this);

        History.addEdit(addHistory, () -> new LayerOrderChangeEdit(this, oldIndex, newIndex));
    }

    public void moveLayerSelectionUp() {
        int oldIndex = layerList.indexOf(activeLayer);

        int newIndex = oldIndex + 1;
        if (newIndex >= layerList.size()) {
            return;
        }
        setActiveLayer(layerList.get(newIndex), AddToHistory.YES);

        assert ConsistencyChecks.fadeCheck(this);
    }

    public void moveLayerSelectionDown() {
        int oldIndex = layerList.indexOf(activeLayer);
        int newIndex = oldIndex - 1;
        if (newIndex < 0) {
            return;
        }

        setActiveLayer(layerList.get(newIndex), AddToHistory.YES);

        assert ConsistencyChecks.fadeCheck(this);
    }

    public BufferedImage calculateCompositeImage() {
        // TODO why is this not working
//        if(getNrLayers() == 1) {
//            ImageLayer layer = (ImageLayer) getLayer(0); // must be
//            return layer.getImage();
//        }

//        BufferedImage imageSoFar = ImageUtils.createCompatibleImage(getCanvasWidth(), getCanvasHeight());

        BufferedImage imageSoFar = new BufferedImage(
                canvas.getWidth(), canvas.getHeight(), TYPE_INT_ARGB_PRE);
        Graphics2D g = imageSoFar.createGraphics();

        boolean firstVisibleLayer = true;
        for (Layer layer : layerList) {
            if (layer.isVisible()) {
                BufferedImage result = layer.applyLayer(g, firstVisibleLayer, imageSoFar);
                if (result != null) { // this was an adjustment layer
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

    public void updateRegion(double startX, double startY, double endX, double endY, int thickness) {
        compositeImageUpToDate = false;
        ic.updateRegion(startX, startY, endX, endY, thickness);
        ic.updateNavigator(false);
    }

    public void dispose() {
        if (selection != null) {
            // stop the timer thread
            selection.die();
        }
    }

    public void addNewLayerFromComposite(String newLayerName) {
        ImageLayer newLayer = new ImageLayer(this, getCompositeImage(), newLayerName, null);
        addLayer(newLayer, AddToHistory.YES, "New Layer from Composite", false, false);
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

    public void deselect(AddToHistory addToHistory) {
        if (selection != null) {
            if (addToHistory.isYes()) {
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
                    SelectionActions.getShowHideSelectionAction().setHideName();
                }
                SelectionActions.setEnabled(false, this);
            } else {
                // we can get here from a DeselectEdit.redo on a non-active composition
            }
        }
    }

    public Shape clipShapeToCanvasSize(Shape shape) {
        assert shape != null;

        Area compBounds = new Area(new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight()));
        Area result = new Area(shape);
        result.intersect(compBounds);

        return result;
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

    public void setSelection(Selection selection) {
        this.selection = selection;
    }

    public void promoteSelection() {
        assert selection == null || !selection.isAlive() : "selection = " + selection;
        setNewSelection(builtSelection);
        builtSelection = null;
    }

    public boolean hasSelection() {
        return (selection != null);
    }

    public void applySelectionClipping(Graphics2D g2, AffineTransform at) {
        if (selection != null) {
            Shape shape;
            if (at != null) {
                Path2D.Float pathShape = new Path2D.Float(selection.getShape());
                pathShape.transform(at);
                shape = pathShape;
            } else { // relative to the composition
                shape = selection.getShape();
            }
            g2.setClip(shape);
        }
    }

    public void invertSelection() {
        if (selection != null) {
            Shape backupShape = selection.getShape();
            Shape inverted = canvas.invertShape(backupShape);
            selection.setShape(inverted);
            SelectionChangeEdit edit = new SelectionChangeEdit(this, backupShape, "Invert Selection");
            History.addEdit(edit);
        }
    }

    public void createSelectionFromShape(Shape selectionShape) {
        if (selection != null) {
            throw new IllegalStateException("createSelectionFromShape called while there was a selection: " + selection.toString());
        }
        setNewSelection(new Selection(selectionShape, ic));
    }

    public void setNewSelection(Selection selection) {
        assert selection != null;
        this.selection = selection;
        if (isActiveComp()) {
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

    public void imageChanged(ImageChangeActions actions) {
        imageChanged(actions, false);
    }

    /**
     * The contents of this composition have been changed, the cache is invalidated,
     * and additional actions might be necessary
     */
    public void imageChanged(ImageChangeActions actions, boolean sizeChanged) {
        compositeImageUpToDate = false;

        if (actions.isRepaint()) {
            if (ic != null) {
                ic.repaint();
                ic.updateNavigator(sizeChanged);
            }
        }

        if (actions.isUpdateHistogram()) {
            HistogramsPanel.INSTANCE.updateFromCompIfShown(this);
        }
    }

    private boolean isActiveComp() {
        if (ic.isMock()) {
            // we are in a unit test
            // TODO hack
            return false;
        }
        return (ImageComponents.getActiveCompOrNull() == this);
    }

    public void activeLayerToCanvasSize() {
        // TODO actually this should work with any layer
        if (activeLayer instanceof ImageLayer) {

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
                    imageEdit = new ImageAndMaskEdit(this, editName, layer, backupImage, maskBackupImage, false);
                } else {
                    // no mask or no mask change, a simple ImageEdit will do
                    imageEdit = new ImageEdit(this, editName, layer, backupImage, IgnoreSelection.YES, false);
                    imageEdit.setFadeable(false);
                }
                History.addEdit(new LinkedEdit(this, editName, translationEdit, imageEdit));
            }
        } else {
            Messages.showNotImageLayerError();
        }
    }

    public void updateAllIconImages() {
        for (Layer layer : layerList) {
            if (layer instanceof ImageLayer) {
                ((ImageLayer) layer).updateIconImage();
            }
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                mask.updateIconImage();
            }
        }
    }

    /**
     * The user reordered the layers by dragging
     */
    public void dragFinished(Layer layer, int newIndex) {
        layerList.remove(layer);
        layerList.add(newIndex, layer);
        imageChanged(FULL);
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
                // the intersection needs to be translated
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
            throw new IllegalStateException("active layer (" + activeLayer.getName() + ") not in list (" + layerList.toString() + ")");
        }
        return true;
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

        private boolean isRepaint() {
            return repaint;
        }

        private boolean isUpdateHistogram() {
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

    /**
     * Includes only the layer names and which layer is active
     */
    public String toLayerNamesString() {
        return layerList.stream()
                .map((layer) -> layer == activeLayer ? "ACTIVE " + layer.getName() : layer.getName())
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
