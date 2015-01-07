/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import pixelitor.history.DeleteLayerEdit;
import pixelitor.history.DeselectEdit;
import pixelitor.history.History;
import pixelitor.history.LayerOrderChangeEdit;
import pixelitor.history.LayerSelectionChangeEdit;
import pixelitor.history.NewLayerEdit;
import pixelitor.history.NotUndoableEdit;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerButton;
import pixelitor.menus.SelectionActions;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionInteraction;
import pixelitor.selection.SelectionType;
import pixelitor.utils.HistogramsPanel;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Optional;

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

    public void addBaseLayer(BufferedImage baseLayerImage) {
//        canvas.updateSize(baseLayerImage.getWidth(), baseLayerImage.getHeight());
        ImageLayer newLayer = new ImageLayer(this, baseLayerImage, null);

        addLayerNoGUI(newLayer);
        activeLayer = newLayer;
//        imageChanged(true, true);
    }

    public void addNewEmptyLayer(String name, boolean bellowActive) {
        ImageLayer newLayer = new ImageLayer(this, name);
        addLayer(newLayer, true, false, bellowActive);
    }

    public void setActiveLayer(Layer newActiveLayer, boolean addToHistory) {
        if (activeLayer != newActiveLayer) {
            Layer oldLayer = activeLayer;
            activeLayer = newActiveLayer;

            LayerButton layerButton = activeLayer.getLayerButton();
            layerButton.setSelected(true);

            AppLogic.activeLayerChanged(newActiveLayer);

            if (addToHistory) {
                LayerSelectionChangeEdit edit = new LayerSelectionChangeEdit(this, oldLayer, newActiveLayer);
                History.addEdit(edit);
            }
        }
        assert checkInvariant();
    }

    public int getNrLayers() {
        return layerList.size();
    }

    /**
     * Adds a layer to the top without updating any GUI
     */
    public void addLayerNoGUI(Layer newLayer) {
        layerList.add(newLayer);
    }

    public void addLayer(Layer newLayer, boolean addToHistory, boolean updateHistogram, boolean bellowActive) {
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
    public void addLayer(Layer newLayer, boolean addToHistory, boolean updateHistogram, int newLayerIndex) {
        layerList.add(newLayerIndex, newLayer);
        setActiveLayer(newLayer, false);
        ic.addLayerToGUI(newLayer, newLayerIndex);

        if (addToHistory) {
            NewLayerEdit newLayerEdit = new NewLayerEdit(this, newLayer);
            History.addEdit(newLayerEdit);
        }

        imageChanged(updateHistogram, updateHistogram); // if the histogram is updated, a repaint is also necessary
        assert checkInvariant();
    }

    public void duplicateLayer() {
        Layer duplicate = activeLayer.duplicate();
        addLayer(duplicate, true, true, false);
        assert checkInvariant();
    }

    public Layer getActiveLayer() {
        return activeLayer;
    }

    public int getActiveLayerIndex() {
        return layerList.indexOf(activeLayer);
    }

    public void okPressedInDialog(String filterName) {
        ((ImageLayer) activeLayer).okPressedInDialog(filterName);
    }

    public void filterWithoutDialogFinished(BufferedImage img, ChangeReason changeReason, String opName) {
        setDirty(true);

        ((ImageLayer) activeLayer).filterWithoutDialogFinished(img, changeReason, opName);

        imageChanged(true, true);
    }

    public void changePreviewImage(BufferedImage img, String filterName) {
        ImageLayer layer = (ImageLayer) activeLayer;
        if (layer.changePreviewImage(img, filterName)) {
            imageChanged(true, true);
        }
    }

    public ImageLayer getActiveImageLayer() {
        if (activeLayer instanceof ImageLayer) {
            ImageLayer imageLayer = (ImageLayer) activeLayer;
            return imageLayer;
        }
        return null;
    }

    public BufferedImage getImageOrSubImageIfSelectedForActiveLayer(boolean copyIfFull, boolean copyAndTranslateIfSelected) {
        ImageLayer imageLayer = getActiveImageLayer();
        if (imageLayer != null) {
            return imageLayer.getImageOrSubImageIfSelected(copyIfFull, copyAndTranslateIfSelected);
        }

        return null;
    }

    public BufferedImage getFilterSource() {
        ImageLayer imageLayer = getActiveImageLayer();
        if (imageLayer != null) {
            return imageLayer.getFilterSourceImage();
        }

        return null;
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

    public void startTranslation(boolean makeDuplicateLayer) {
        if (!(activeLayer instanceof ContentLayer)) {
            return;
        }

        if (makeDuplicateLayer) {
            duplicateLayer();
        }

        ((ContentLayer) activeLayer).startTranslation();
    }

    public void endTranslation() {
        if (!(activeLayer instanceof ContentLayer)) {
            return;
        }

        ((ContentLayer) activeLayer).endTranslation();
        imageChanged(true, true);
    }

    public Layer getLayer(int i) {
        return layerList.get(i);
    }

    public void flattenImage(boolean updateGUI) {
        if (updateGUI) {
            assert isActiveComp();
        }

        if (layerList.size() < 2) {
            return;
        }

        int nrLayers = getNrLayers();
        BufferedImage bi = getCompositeImage();

        Layer flattenedLayer = new ImageLayer(this, bi, "flattened");
        addLayer(flattenedLayer, false, false, nrLayers); // add to the top

        for (int i = nrLayers - 1; i >= 0; i--) { // remove the rest
            removeLayer(i);
        }
        if (updateGUI) {
            AppLogic.activeCompLayerCountChanged(this, 1);
            History.addEdit(new NotUndoableEdit(this, "Flatten Image"));
        }
    }

    public void mergeDown() {
        assert checkInvariant();

        int activeIndex = layerList.indexOf(activeLayer);
        if (activeIndex > 0) {
            if (activeLayer.isVisible()) {
                Layer bellow = layerList.get(activeIndex - 1);
                if (bellow instanceof ImageLayer) {
                    ImageLayer imageLayerBellow = (ImageLayer) bellow;
                    if (imageLayerBellow.isVisible()) {
                        activeLayer.mergeDownOn(imageLayerBellow);
                        removeActiveLayer();

                        History.addEdit(new NotUndoableEdit(this, "Merge Down"));
                    }
                }
            }
        }
    }

    public void moveActiveLayer(boolean up) {
        assert checkInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        int newIndex = up ? oldIndex + 1 : oldIndex - 1;
        swapLayers(oldIndex, newIndex, false);
    }

    public void moveActiveLayerToTop() {
        assert checkInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        swapLayers(oldIndex, layerList.size() - 1, false);
    }

    public void moveActiveLayerToBottom() {
        assert checkInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        swapLayers(oldIndex, 0, false);
    }

    public void swapLayers(int oldIndex, int newIndex, boolean isUndoRedo) {
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

        imageChanged(true, true);

        AppLogic.layerOrderChanged(this);

        if (!isUndoRedo) {
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
        setActiveLayer(layerList.get(newIndex), true);

        assert ConsistencyChecks.fadeCheck(this);
    }


    public void moveLayerSelectionDown() {
        int oldIndex = layerList.indexOf(activeLayer);
        int newIndex = oldIndex - 1;
        if (newIndex < 0) {
            return;
        }

        setActiveLayer(layerList.get(newIndex), true);

        assert ConsistencyChecks.fadeCheck(this);
    }


    private BufferedImage calculateCompositeImage() {
        // TODO why is this not working
//        if(getNrLayers() == 1) {
//            ImageLayer layer = (ImageLayer) getLayer(0); // must be
//            return layer.getImage();
//        }

//        BufferedImage imageSoFar = ImageUtils.createCompatibleImage(getCanvasWidth(), getCanvasHeight());

        BufferedImage imageSoFar = new BufferedImage(
                canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = imageSoFar.createGraphics();

        boolean firstVisibleLayer = true;
        for (Layer layer : layerList) {
            if (layer.isVisible()) {
                BufferedImage result = layer.paintLayer(g, firstVisibleLayer, imageSoFar);
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

    }

    public void addLayersToGUI() {
        // when adding layer buttons the last layer always gets active
        // but here we don't want to change the selected layer
        Layer previousActiveLayer = activeLayer;

        for (Layer layer : layerList) {
            addLayerToGUI(layer);
        }

        setActiveLayer(previousActiveLayer, false);
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

    private void setName(String name) {
        this.name = name;
        if (ic != null) {
            ic.updateTitle();
        }
    }

    private void removeLayer(int layerIndex) {
        Layer layer = layerList.get(layerIndex);
        removeLayer(layer, false);
    }

    public void removeActiveLayer() {
        removeLayer(activeLayer, false);
    }

    public void removeLayer(Layer layerToBeRemoved, boolean isUndoRedo) {
        if (layerList.size() < 2) {
            throw new IllegalStateException("there are " + layerList.size() + " layers");
        }

        int layerIndex = layerList.indexOf(layerToBeRemoved);

        if (!isUndoRedo) {
            DeleteLayerEdit newLayerEdit = new DeleteLayerEdit(this, layerToBeRemoved, layerIndex);
            History.addEdit(newLayerEdit);
        }

        LayerButton button = layerToBeRemoved.getLayerButton();

        layerList.remove(layerToBeRemoved);

        if (layerToBeRemoved == activeLayer) {
            if (layerIndex > 0) {
                setActiveLayer(layerList.get(layerIndex - 1), false);
            } else {  // removed the fist layer, set the new first layer as active
                setActiveLayer(layerList.get(0), false);
            }
        }

        ic.deleteLayerButton(button);

        if (isActiveComp()) {
            AppLogic.activeCompLayerCountChanged(this, layerList.size());
        }

        imageChanged(true, true);
    }

    public void dispose() {
        if (selection != null) {
            // stop the timer thread
            selection.deselectAndDispose();
        }
    }

    public void addNewLayerFromComposite(String newLayerName) {
        ImageLayer newLayer = new ImageLayer(this, getCompositeImage(), newLayerName);
        addLayer(newLayer, true, false, false);
    }

    public ImageComponent getIC() {
        return (ImageComponent) ic;
    }

    public void paintSelection(Graphics2D g) {
        if (selection != null) {
            selection.paintMarchingAnts(g);
        }
    }

    public void deselect(boolean sendDeselectEdit) {
        if (selection != null) {
            if (sendDeselectEdit) {
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

    public boolean hasSelection() {
        return (selection != null);
    }

    public BufferedImage getCompositeImage() {
        if (compositeImageUpToDate) {
            return cachedCompositeImage; // this caching is useful for example when using the Color Picker Tool
        }

        cachedCompositeImage = calculateCompositeImage();

        compositeImageUpToDate = true;
        return cachedCompositeImage;
    }

    /**
     *
     */
    public void imageChanged(boolean repaint, boolean updateHistogram) {
        compositeImageUpToDate = false;

        if (repaint) {
            if (ic != null) {
                ic.repaint();
            }
        }

        if (updateHistogram) {
            HistogramsPanel.INSTANCE.updateFromCompIfShown(this);
        }
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void moveActiveContentRelative(int relativeX, int relativeY, boolean repaint) {
        if (activeLayer instanceof ContentLayer) {
            ContentLayer contentLayer = (ContentLayer) activeLayer;
            contentLayer.moveLayerRelative(relativeX, relativeY);
            imageChanged(repaint, false);
        }
    }

    public boolean isActiveLayer(Layer layer) {
        return layer == activeLayer;
    }

    public void setSelectionClipping(Graphics2D g2, AffineTransform at) {
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
        selection = new Selection(ic, selectionType, selectionInteraction);
        if (isActiveComp()) {
            SelectionActions.setEnabled(true, this);
        }
    }

    public void createSelectionFromShape(Shape selectionShape) {
        if (selection != null) {
            throw new IllegalStateException("createSelectionFromShape called while there was a selection: " + selection.toString());
        }

        selection = new Selection(selectionShape, ic);

        if (isActiveComp()) { // this could be called from the undo on a not-active component
            SelectionActions.setEnabled(true, this);
        }
    }

    public boolean isActiveComp() {
        // TODO this was a hack in order to isolate the unit tests from ImageComponents
        if (!(ic instanceof ImageComponent)) {
            return false;
        }
        return (ImageComponents.getActiveComp().get() == this);
    }

    public void layerToCanvasSize() {
        ImageLayer layer = getActiveImageLayer();
        if (layer != null) {
            layer.cropToCanvasSize();
            History.addEdit(new NotUndoableEdit(this, "Layer to Canvas Size")); // TODO ImageEdit would be better
        }
    }

    public void enlargeCanvas(int north, int east, int south, int west) {
        for (Layer layer : layerList) {
            if (layer instanceof ContentLayer) {
                ContentLayer contentLayer = (ContentLayer) layer;
                contentLayer.enlargeCanvas(north, east, south, west);
            }
        }

        canvas.updateSize(canvas.getWidth() + east + west, canvas.getHeight() + north + south);

        imageChanged(true, false);
        setDirty(true);
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
        imageChanged(true, true);
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

}
