/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.filters.Filter;
import pixelitor.filters.FilterUtils;
import pixelitor.filters.RepeatLast;
import pixelitor.history.AddToHistory;
import pixelitor.history.CompoundEdit;
import pixelitor.history.DeleteLayerEdit;
import pixelitor.history.DeselectEdit;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.history.LayerOrderChangeEdit;
import pixelitor.history.LayerSelectionChangeEdit;
import pixelitor.history.NewLayerEdit;
import pixelitor.history.NotUndoableEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerButton;
import pixelitor.layers.LayerMask;
import pixelitor.menus.SelectionActions;
import pixelitor.selection.IgnoreSelection;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionInteraction;
import pixelitor.selection.SelectionType;
import pixelitor.utils.Dialogs;
import pixelitor.utils.HistogramsPanel;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.UpdateGUI;
import pixelitor.utils.Utils;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.Composition.ImageChangeActions.INVALIDATE_CACHE;

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
    private transient ImageDisplay ic;
    private transient Selection selection;

    // A Composition can be created either with one of the following static
    // factory methods or through deserialization (pxc)

    /**
     * Creates a single-layered composition from the given image
     */
    public static Composition fromImage(BufferedImage img, File file, String name) {
        assert img != null;

        img = ImageUtils.toCompatibleImage(img);
        Canvas canvas = new Canvas(img.getWidth(), img.getHeight());
        Composition comp = new Composition(canvas);
        comp.addBaseLayer(img);

        if (file != null) {
            comp.setFile(file); // also sets the name based on the file name
        } else if (name != null) {
            comp.setName(name);
        } else {
            // of of the file and name arguments must be set to non-null
            throw new IllegalArgumentException("no name could be set");
        }
        return comp;
    }

    /**
     * Creates an empty composition (no layers, no name)
     */
    public static Composition empty(int width, int height) {
        Canvas canvas = new Canvas(width, height);
        Composition comp = new Composition(canvas);
        return comp;
    }

    private Composition(Canvas canvas) {
        this.canvas = canvas;
    }

    private void addBaseLayer(BufferedImage baseLayerImage) {
        ImageLayer newLayer = new ImageLayer(this, baseLayerImage, null, null);

        addLayerNoGUI(newLayer);
        activeLayer = newLayer;
    }

    public void addNewEmptyLayer(String name, boolean bellowActive) {
        ImageLayer newLayer = new ImageLayer(this, name);
        addLayer(newLayer, AddToHistory.YES, false, bellowActive);
    }

    public void setActiveLayer(Layer newActiveLayer, AddToHistory addToHistory) {
        if (activeLayer != newActiveLayer) {
            Layer oldLayer = activeLayer;
            activeLayer = newActiveLayer;

            LayerButton layerButton = activeLayer.getLayerButton();
            layerButton.setSelected(true);

            AppLogic.activeLayerChanged(newActiveLayer);

            if (addToHistory.isYes()) {
                LayerSelectionChangeEdit edit = new LayerSelectionChangeEdit(this, oldLayer, newActiveLayer);
                History.addEdit(edit);
            }
        }
        assert checkInvariant();
    }

    public int getNrLayers() {
        return layerList.size();
    }

    public int getNrImageLayers() {
        int sum = 0;
        for (Layer layer : layerList) {
            if (layer instanceof ImageLayer) {
                sum++;
            }
        }
        return sum;
    }

    /**
     * Adds a layer to the top without updating any GUI
     */
    public void addLayerNoGUI(Layer newLayer) {
        layerList.add(newLayer); // adds it to top, ignoring the active layer position
        setActiveLayer(newLayer, AddToHistory.NO);
    }

    public void addLayer(Layer newLayer, AddToHistory addToHistory, boolean updateHistogram, boolean bellowActive) {
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

        addLayer(newLayer, addToHistory, updateHistogram, newLayerIndex);
    }

    /**
     * Adds the specified layer at the specified layer position
     */
    public void addLayer(Layer newLayer, AddToHistory addToHistory, boolean updateHistogram, int newLayerIndex) {
        layerList.add(newLayerIndex, newLayer);
        setActiveLayer(newLayer, AddToHistory.NO);
        ic.addLayerToGUI(newLayer, newLayerIndex);

        if (addToHistory.isYes()) {
            NewLayerEdit newLayerEdit = new NewLayerEdit(this, newLayer);
            History.addEdit(newLayerEdit);
        }

        if (updateHistogram) {
            imageChanged(FULL); // if the histogram is updated, a repaint is also necessary
        } else {
            imageChanged(INVALIDATE_CACHE);
        }
        assert checkInvariant();
    }

    public void duplicateLayer() {
        Layer duplicate = activeLayer.duplicate();
        addLayer(duplicate, AddToHistory.YES, true, false);
        assert checkInvariant();
    }

    public Layer getActiveLayer() {
        return activeLayer;
    }

    public int getActiveLayerIndex() {
        return layerList.indexOf(activeLayer);
    }

    public void okPressedInDialog(String filterName) {
        getActiveMaskOrImageLayer().okPressedInDialog(filterName);
    }

    public void filterWithoutDialogFinished(BufferedImage img, ChangeReason changeReason, String opName) {
        setDirty(true);

        getActiveMaskOrImageLayer().filterWithoutDialogFinished(img, changeReason, opName);

        imageChanged(FULL);
    }

    public void changePreviewImage(BufferedImage img, String filterName, ChangeReason changeReason) {
        ImageLayer layer = getActiveMaskOrImageLayer();
        layer.changePreviewImage(img, filterName, changeReason);
    }

    public boolean activeIsImageLayer() {
        return (activeLayer instanceof ImageLayer) || activeLayer.isMaskEditing();
    }

    public boolean hasActiveImageLayerOrMask() {
        if (activeLayer instanceof ImageLayer) {
            return true;
        }
        return activeLayer.hasMask();
    }

    public Layer getActiveLayerOrMask() {
        if (activeLayer.isMaskEditing()) {
            return activeLayer.getMask();
        }
        return activeLayer;
    }

    public ImageLayer getAnyImageLayer() {
        for (Layer layer : layerList) {
            if (layer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                return imageLayer;
            }
        }
        return null;
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
            throw new IllegalStateException("active layer is not image layer");
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

    public BufferedImage getImageOrSubImageIfSelectedForActiveLayer(boolean copyIfFull, boolean copyAndTranslateIfSelected) {
        return getActiveMaskOrImageLayer()
                .getImageOrSubImageIfSelected(copyIfFull, copyAndTranslateIfSelected);
    }

    public BufferedImage getFilterSource() {
        return getActiveMaskOrImageLayer().getFilterSourceImage();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public boolean isEmpty() {
        return layerList.isEmpty();
    }

    public String getName() {
        return name;
    }

    public void startMovement(boolean makeDuplicateLayer) {
        if (makeDuplicateLayer) {
            duplicateLayer();
        }

        getActiveLayerOrMask().startMovement();
    }

    public void moveActiveContentRelative(int relativeX, int relativeY) {
        getActiveLayerOrMask().moveWhileDragging(relativeX, relativeY);
        imageChanged(FULL);
    }

    public void endMovement() {
        PixelitorEdit edit = getActiveLayerOrMask().endMovement();
        if (edit != null) {
            // The layer, the mask, or both moved.
            // We always should get here except if an adjustment
            // layer without a mask was moved.
            History.addEdit(edit);
            imageChanged(FULL);
        }
    }

    public Layer getLayer(int i) {
        return layerList.get(i);
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
        addLayer(flattenedLayer, AddToHistory.NO, false, nrLayers); // add to the top

        for (int i = nrLayers - 1; i >= 0; i--) { // remove the rest
            removeLayer(i);
        }
        if (updateGUI.isYes()) {
            AppLogic.activeCompLayerCountChanged(this, 1);

            // TODO should have a separate add to history argument?
            History.addEdit(new NotUndoableEdit(this, "Flatten Image"));
        }
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

                    removeActiveLayer(updateGUI);

                    PixelitorEdit edit = new CompoundEdit(this, "Merge Down",
                            new ImageEdit("", this, imageLayerBellow, backupImage, IgnoreSelection.YES, false),
                            new DeleteLayerEdit(this, mergedLayer, activeIndex)
                    );

                    History.addEdit(edit);
                }
            }
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

        if (addHistory.isYes()) {
            LayerOrderChangeEdit edit = new LayerOrderChangeEdit(this, oldIndex, newIndex);
            History.addEdit(edit);
        }
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

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    public String generateNewLayerName() {
        String retVal = "layer " + newLayerCount;
        newLayerCount++;
        return retVal;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void updateRegion(int startX, int startY, int endX, int endY, int thickness) {
        compositeImageUpToDate = false;
        ic.updateRegion(startX, startY, endX, endY, thickness);
    }

    /**
     * Called when deserialized
     */
    public void setImageComponent(ImageDisplay ic) {
        this.ic = ic;
        canvas.setIc(ic);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        for (Layer layer : layerList) {
            if (layer instanceof ImageLayer) {
                ((ImageLayer) layer).updateIconImage();
            }
        }
    }

    public void addLayersToGUI() {
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

    public Rectangle getCanvasBounds() {
        return canvas.getBounds();
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        setName(file.getName());
    }

    public void setName(String name) {
        this.name = name;
        if (ic != null) {
            ic.updateTitle();
        }
    }

    private void removeLayer(int layerIndex) {
        Layer layer = layerList.get(layerIndex);
        removeLayer(layer, AddToHistory.YES, UpdateGUI.YES);
    }

    public void removeActiveLayer(UpdateGUI updateGUI) {
        removeLayer(activeLayer, AddToHistory.YES, updateGUI);
    }

    public void removeLayer(Layer layerToBeRemoved, AddToHistory addToHistory, UpdateGUI updateGUI) {
        if (layerList.size() < 2) {
            throw new IllegalStateException("there are " + layerList.size() + " layers");
        }

        int layerIndex = layerList.indexOf(layerToBeRemoved);

        if (addToHistory.isYes()) {
            DeleteLayerEdit newLayerEdit = new DeleteLayerEdit(this, layerToBeRemoved, layerIndex);
            History.addEdit(newLayerEdit);
        }

        layerList.remove(layerToBeRemoved);

        if (layerToBeRemoved == activeLayer) {
            if (layerIndex > 0) {
                setActiveLayer(layerList.get(layerIndex - 1), AddToHistory.NO);
            } else {  // removed the fist layer, set the new first layer as active
                setActiveLayer(layerList.get(0), AddToHistory.NO);
            }
        }

        if(updateGUI.isYes()) {
            LayerButton button = layerToBeRemoved.getLayerButton();
            ic.deleteLayerButton(button);

            if (isActiveComp()) {
                AppLogic.activeCompLayerCountChanged(this, layerList.size());
            }

            imageChanged(FULL);
        }
    }

    public void dispose() {
        if (selection != null) {
            // stop the timer thread
            selection.deselectAndDispose();
        }
    }

    public void addNewLayerFromComposite(String newLayerName) {
        ImageLayer newLayer = new ImageLayer(this, getCompositeImage(), newLayerName, null);
        addLayer(newLayer, AddToHistory.YES, false, false);
    }

    public ImageComponent getIC() {
        return (ImageComponent) ic;
    }

    // we are not in a test, see method above
    public boolean hasRealIC() {
        return ic instanceof ImageComponent;
    }

    public void paintSelection(Graphics2D g) {
        if (selection != null) {
            selection.paintMarchingAnts(g);
        }
    }

    public void deselect(AddToHistory addToHistory) {
        if (selection != null) {
            if (addToHistory.isYes()) {
                Shape shape = selection.getShape();
                if (shape != null) { // for a simple click without a previous selection this is null
                    DeselectEdit edit = new DeselectEdit(this, shape, "Composition.deselect");
                    History.addEdit(edit);
                }
            }

            selection.deselectAndDispose();
            selection = null;

            if (isActiveComp()) {
                SelectionActions.setEnabled(false, this);
            } else {
                // we can get here from a DeselectEdit.redo on a non-active composition
            }
        }
    }

    public Optional<Selection> getSelection() {
        return Optional.ofNullable(selection);
    }

    public Selection getSelectionOrNull() {
        return selection;
    }

    public boolean hasSelection() {
        return (selection != null);
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


    /**
     * The contents of this composition have been changed, the cache is invalidated,
     * and additional actions might be necessary
     */
    public void imageChanged(ImageChangeActions actions) {
        compositeImageUpToDate = false;

        if (actions.isRepaint()) {
            if (ic != null) {
                ic.repaint();
            }
        }

        if (actions.isUpdateHistogram()) {
            HistogramsPanel.INSTANCE.updateFromCompIfShown(this);
        }
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isActiveLayer(Layer layer) {
        return layer == activeLayer;
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
            selection.invert(getCanvasBounds());

            SelectionChangeEdit edit = new SelectionChangeEdit(this, backupShape, "Invert Selection");
            History.addEdit(edit);
        }
    }

    public void startSelection(SelectionType selectionType, SelectionInteraction selectionInteraction) {
        setNewSelection(new Selection(ic, selectionType, selectionInteraction));
    }

    public void createSelectionFromShape(Shape selectionShape) {
        if (selection != null) {
            throw new IllegalStateException("createSelectionFromShape called while there was a selection: " + selection.toString());
        }
        setNewSelection(new Selection(selectionShape, ic));
    }

    private void setNewSelection(Selection selection) {
        this.selection = selection;
        if (isActiveComp()) {
            SelectionActions.setEnabled(true, this);
        }
    }

    private boolean isActiveComp() {
        if (!(ic instanceof ImageComponent)) {
            // we are in a unit test
            return false;
        }
        return (ImageComponents.getActiveComp().get() == this);
    }

    public void layerToCanvasSize() {
        // TODO actually this should work with any layer
        if (activeLayer instanceof ImageLayer) {
            ((ImageLayer) activeLayer).cropToCanvasSize();
            if (activeLayer.hasMask()) {
                LayerMask mask = activeLayer.getMask();
                mask.cropToCanvasSize();
            }

            // TODO A CompoundEdit from two ImageEdits (layer, mask) could be used
            History.addEdit(new NotUndoableEdit(this, "Layer to Canvas Size"));
        } else {
            Dialogs.showNotImageLayerDialog();
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

    public int getCanvasWidth() {
        return canvas.getWidth();
    }

    public int getCanvasHeight() {
        return canvas.getHeight();
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

    public int getLayerPosition(Layer layer) {
        return layerList.indexOf(layer);
    }

    /**
     * Executes the given filter with busy cursor
     */
    public void executeFilterWithBusyCursor(Filter filter, ChangeReason changeReason, Component busyCursorParent) {
        String filterMenuName = filter.getMenuName();

        try {
            long startTime = System.nanoTime();

            Runnable task = () -> filter.runit(this, changeReason);
            Utils.executeWithBusyCursor(busyCursorParent, task);

            long totalTime = (System.nanoTime() - startTime) / 1_000_000;
            String performanceMessage;
            if (totalTime < 1000) {
                performanceMessage = filterMenuName + " took " + totalTime + " ms";
            } else {
                float seconds = totalTime / 1000.0f;
                performanceMessage = String.format("%s took %.1f s", filterMenuName, seconds);
            }
            AppLogic.setStatusMessage(performanceMessage);
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        } catch (Throwable e) { // make sure AssertionErrors are caught
            if (Build.CURRENT.isRobotTest()) {
                throw e; // we can debug the exact filter parameters only in RobotTest
            }
            Dialogs.showExceptionDialog(e);
        }

        FilterUtils.setLastExecutedFilter(filter);
        RepeatLast.INSTANCE.setMenuName("Repeat " + filterMenuName);
    }

    public void setShowOriginal(boolean b) {
        getActiveMaskOrImageLayer().setShowOriginal(b);
    }

    public void cropSelection(Rectangle cropRect) {
        if (selection != null) {
            Shape currentShape = selection.getShape();
            Shape intersection = SelectionInteraction.INTERSECT.combine(currentShape, cropRect);
            if (intersection.getBounds().isEmpty()) {
                selection.deselectAndDispose();
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
        final StringBuilder sb = new StringBuilder("Composition{");
        sb.append("name='").append(name).append('\'');
        sb.append(", activeLayer=").append(activeLayer.getName());
        sb.append(", layerList=").append(layerList);
        sb.append(", canvas=").append(canvas);
        sb.append(", selection=").append(selection);
        sb.append(", dirty=").append(dirty);
        sb.append('}');
        return sb.toString();
    }
}
