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

package pixelitor;

import pixelitor.compactions.EnlargeCanvas;
import pixelitor.compactions.Outsets;
import pixelitor.gui.*;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.ImagePreviewPanel;
import pixelitor.guides.Guides;
import pixelitor.guides.GuidesChangeEdit;
import pixelitor.history.*;
import pixelitor.io.FileFormat;
import pixelitor.io.FileUtils;
import pixelitor.io.IOTasks;
import pixelitor.io.SaveSettings;
import pixelitor.layers.*;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.selection.ShapeCombinator;
import pixelitor.tools.Tools;
import pixelitor.tools.move.MoveMode;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.Paths;
import pixelitor.tools.pen.history.ConvertSelectionToPathEdit;
import pixelitor.tools.util.PPoint;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.String.format;
import static pixelitor.io.FileUtils.removeExtension;
import static pixelitor.layers.LayerAdder.Position.ABOVE_ACTIVE;
import static pixelitor.layers.LayerAdder.Position.BELOW_ACTIVE;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;
import static pixelitor.utils.Threads.threadInfo;
import static pixelitor.utils.Utils.createCopyName;
import static pixelitor.utils.debug.DebugNodes.createBufferedImageNode;

/**
 * An image composition containing multiple layers, paths, guides, and overall state.
 */
public class Composition implements Serializable, ImageSource, LayerHolder {
    // serialization is used for saving in the pxc format
    @Serial
    private static final long serialVersionUID = 1L;

    private static long debugIdCounter = 0;

    private String name;

    private final List<Layer> layerList = new ArrayList<>();

    // The currently selected layer, potentially nested within groups or smart objects.
    private Layer activeLayer;

    // The top-level ancestor layer of the activeLayer.
    // If the active layer is top-level, then it's the same as the active layer.
    private transient Layer activeTopLevelLayer;

    // a counter for the names of new layers
    private int newLayerCount = 1;

    private final Canvas canvas;
    private Paths paths;
    private Guides guides;
    private ImageMode mode;

    //
    // transient variables from here
    //

    // List of smart objects that use this composition as their content.
    // Multiple smart objects can reference the same content due to shallow cloning.
    // Marked transient to prevent serialization of parent compositions.
    private transient List<SmartObject> owners;

    // useful for distinguishing between versions with the same name
    private transient String debugName;

    // the file associated with the composition, if loaded from/saved to one
    private transient File file;
    // the last modified time of the file in millis since the epoch
    private transient long fileTimestamp;

    // whether the composition has been modified since the last save
    private transient boolean dirty = false;

    // cached rendering of all visible layers combined
    private transient BufferedImage compositeImage;

    // the View that shows this composition, if any
    private transient View view;

    private transient Selection selection;

    // A temporary selection that is currently being created
    // by dragging, but it's not finalized yet.
    private transient Selection draftSelection;

    /**
     * Private constructor. Use static factory methods
     * or deserialization to create instances.
     */
    private Composition(Canvas canvas, ImageMode mode) {
        assert canvas != null;
        this.canvas = canvas;
        this.mode = mode;
    }

    /**
     * Creates a single-layered composition from the given image.
     */
    public static Composition fromImage(BufferedImage img, File file, String name) {
        assert img != null;
        Canvas canvas = new Canvas(img.getWidth(), img.getHeight());

        ImageMode mode;
        if (Features.enableImageMode && img.getColorModel() instanceof IndexColorModel) {
            mode = ImageMode.INDEXED;
        } else {
            mode = ImageMode.RGB;
            img = ImageUtils.toSysCompatibleImage(img);
        }

        var comp = new Composition(canvas, mode);
        comp.addBaseLayer(img);

        if (file != null) {
            comp.setFile(file); // also sets the name based on the file name
        } else if (name != null) {
            comp.setName(name);
        } else {
            throw new IllegalArgumentException("must be given a file or a name");
        }
        comp.createDebugName();
        assert comp.getName() != null;
        return comp;
    }

    /**
     * Creates an empty composition (no layers, no name).
     */
    public static Composition createEmpty(int width, int height, ImageMode mode) {
        Canvas canvas = new Canvas(width, height);
        return new Composition(canvas, mode);
    }

    /**
     * Creates a composition with a single, transparent image layer.
     */
    public static Composition createTransparent(int width, int height) {
        BufferedImage img = ImageUtils.createSysCompatibleImage(width, height);
        return fromImage(img, null, "transparent");
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Initialize transient variables
        compositeImage = null; // will be set when needed
        file = null; // will be set later
        fileTimestamp = 0;
        debugName = null; // will be set later
        dirty = false;
        view = null; // will be set later
        selection = null; // the selection isn't saved
        draftSelection = null;
        owners = null; // will be set from the owners when they are deserialized

        in.defaultReadObject();

        activeTopLevelLayer = activeLayer.getTopLevelLayer();

        // Perform actions that need a full canvas and also
        // (re)load the contents of linked smart objects
        forEachNestedLayerOfType(CompositeLayer.class, CompositeLayer::afterDeserialization);

        createDebugName();
        assert checkAllSOInvariants();
    }

    /**
     * Creates and returns a deep copy of this composition.
     */
    public Composition copy(CopyType copyType, boolean copySelection) {
        assert checkInvariants();
        var compCopy = new Composition(canvas.copy(), mode);

        // copy layers recursively
        for (Layer layer : layerList) {
            // Layer.copy handles recursion
            var layerCopy = layer.copy(copyType, true, compCopy);

            // fully setup only the top-level stuff here
            layerCopy.setHolder(compCopy);
            compCopy.layerList.add(layerCopy);
            if (layer == activeTopLevelLayer) {
                compCopy.activeTopLevelLayer = layerCopy;
            }
        }

        compCopy.newLayerCount = newLayerCount;
        compCopy.mode = mode;

        if (copySelection && selection != null) {
            compCopy.setSelection(new Selection(selection, copyType == CopyType.UNDO));
        }
        if (paths != null) {
            compCopy.paths = paths.deepCopy(compCopy);
        }

        // In the case of undo, the view will be transferred later.
        // At no time should two comps point to the same view.
        compCopy.view = null;

        if (copyType == CopyType.UNDO) {
            compCopy.dirty = dirty;
            compCopy.file = file;
            compCopy.fileTimestamp = fileTimestamp;
            compCopy.name = name;
            // the new guides are set in the action that needed undo
        } else {
            compCopy.dirty = false;
            compCopy.file = null;
            compCopy.fileTimestamp = 0;
            compCopy.name = createCopyName(removeExtension(name));
            if (guides != null) {
                compCopy.guides = guides.copyIdentical(view);
            }
        }
        compCopy.createDebugName();

        assert checkInvariants();
        assert compCopy.checkInvariants();

        // if it's an undo, then the active layer names must match
        assert copyType != CopyType.UNDO || compCopy.activeLayer.getName().equals(activeLayer.getName())
            : "copyType = " + copyType + ", compCopy.activeLayer.getName() = " + compCopy.activeLayer.getName() + ", activeLayer.getName() = " + activeLayer.getName();

        return compCopy;
    }

    @Override
    public Composition getComp() {
        return this;
    }

    /**
     * Returns the holder of the active layer.
     */
    public LayerHolder getActiveHolder() {
        return activeLayer.getHolder();
    }

    public LayerHolder getHolderForGrouping() {
        LayerHolder holder = getActiveHolder();
        if (holder instanceof SmartObject so) {
            return so.getHolder();
        }
        return holder;
    }

    /**
     * Determines the holder where newly created layers should be added.
     */
    public LayerHolder getHolderForNewLayers() {
        if (activeLayer == null) { // can happen during initialization
            return this;
        }
        // delegate to the active layer to decide (for example
        // a layer group wants layers added inside itself)
        return activeLayer.getHolderForNewLayers();
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;

        if (view != null) {
            canvas.recalcCoSize(view, true);

            if (selection != null) {
                selection.setView(view);
            }
            if (paths != null) {
                paths.setView(view);
            }
        } else { // the new view can be null when closing or reloading
            if (selection != null) {
                disposeSelection();
            }
            if (paths != null) {
                paths.setView(null);
            }
        }
    }

    /**
     * Returns the parent component for displaying dialogs related to this composition.
     */
    public Component getDialogParent() {
        if (isOpen()) {
            return view.getDialogParent();
        }
        return PixelitorWindow.get();
    }

    /**
     * Called when this composition's view is deactivated
     * (because another view becomes active).
     */
    public void deactivated() {
        if (isSmartObjectContent()) {
            // lazily update all containing smart objects
            for (SmartObject owner : owners) {
                owner.propagateContentChanges(this, false);
            }
        }
    }

    public List<SmartObject> getOwners() {
        return owners;
    }

    public void addOwner(SmartObject newOwner) {
        assert newOwner != null;
        if (owners == null) {
            owners = new ArrayList<>(1);
            owners.add(newOwner);
        } else if (!owners.contains(newOwner)) { // avoid duplicates
            owners.add(newOwner);
        }
    }

    /**
     * Checks if this {@link Composition} is the content of any {@link SmartObject}.
     */
    public boolean isSmartObjectContent() {
        // If a content file is opened independently of its parent,
        // then this will return false, even for PXC files!
        return owners != null;
    }

    public Canvas getCanvas() {
        return canvas;
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

    public Shape clipToCanvasBounds(Shape shape) {
        return canvas.clip(shape);
    }

    public PPoint genRandomPointInCanvas() {
        return canvas.genRandomPoint(view);
    }

    public boolean isDirty() {
        return dirty;
    }

    /**
     * Returns true if this composition has unsaved changes that need to be saved.
     */
    public boolean hasUnsavedChanges() {
        if (!dirty) {
            // not modified, definitely doesn't need saving
            return false;
        }

        // If this is the content of a smart object, the dirty flag
        // is relevant only if the content is linked (not embedded).
        // Otherwise, the parent composition should save it.
        if (isSmartObjectContent()) {
            for (SmartObject owner : owners) {
                if (owner.isContentEmbedded()) {
                    return false;
                }
            }
        }

        return true; // modified and there's no embedding parent to save it
    }

    public void setDirty(boolean dirty) {
        if (this.dirty == dirty) {
            return;
        }

        this.dirty = dirty;
        if (isActive() && !AppMode.isUnitTesting()) {
            PixelitorWindow.get().updateTitle(this);
        }
    }

    private void clearDirtyFlagsRecursively() {
        setDirty(false);

        forEachNestedSmartObject(so -> {
            if (so.findSavingComp() == this) {
                so.getContent().setDirty(false);
            }
        });
    }

    /**
     * Checks if this composition is currently open in a view.
     */
    public boolean isOpen() {
        return view != null;
    }

    private void closeView() {
        if (isOpen()) {
            view.close();
        }
    }

    /**
     * Called when this composition is no longer needed because it was closed or replaced.
     */
    public void dispose() {
        if (selection != null) {
            disposeSelection();
        }
        removeAllLayerUIs();
        setView(null);
    }

    /**
     * Finds the first open view in the parent hierarchy.
     */
    public View findParentView() {
        if (isOpen()) {
            return view;
        }
        if (isSmartObjectContent()) {
            // Recursively search in the hierarchy of parents.
            // It checks only the first owner, because it assumes
            // that all owners are in the same composition.
            return owners.getFirst().findParentView();
        }

        throw new IllegalStateException("no view for top-level comp " + getDebugName());
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        assert name != null;
        this.name = name;

        // TODO isActive() should imply isOpen(), but apparently in a
        //  unit test a composition can be active vithout having a view
        if (isOpen() && isActive()) {
            view.updateTitle();
            PixelitorWindow.get().updateTitle(this);
        }
    }

    /**
     * Create a file name that will be suggested as the default
     * file name in the save dialog.
     */
    public String suggestFileName(String extension) {
        if (file == null) {
            return name + "." + extension;
        } else {
            return FileUtils.replaceExtension(file.getName(), extension);
        }
    }

    /**
     * Calculates the title shown in the in window title bar.
     */
    public String calcWindowTitle() {
        String baseTitle = dirty ? "* " + name : name;
        return baseTitle + " (" + canvas.getSizeString() + ")";
    }

    /**
     * Opens a dialog to rename this composition interactively.
     */
    public void renameInteractively(TabViewContainer owner) {
        String currentName = getName();
        String chosenName = JOptionPane.showInputDialog(owner,
            "New Name:", currentName);
        rename(currentName, chosenName);
    }

    /**
     * Renames the composition and adds a history entry.
     */
    public void rename(String oldName, String newName) {
        if (newName.equals(oldName)) {
            return;
        }
        setName(newName);
        History.add(new CompositionRenamedEdit(this, oldName, newName));
    }

    public String getDebugName() {
        return debugName;
    }

    public void createDebugName() {
        assert name != null;
        assert debugName == null;

        this.debugName = name + " " + debugIdCounter++;
    }

    /**
     * Generates a unique name for a new layer.
     */
    public String generateNewLayerName() {
        return "layer " + newLayerCount++;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        if (file == null) {
            return;
        }
        this.fileTimestamp = file.lastModified();
        setName(file.getName());
    }

    public boolean hasSameFileAs(Composition other, boolean allowBothNull) {
        if (file == null) {
            if (allowBothNull) {
                return other.file == null;
            } else {
                return false;
            }
        } else {
            return file.equals(other.file);
        }
    }

    public boolean hasNoLayers() {
        return layerList.isEmpty();
    }

    /**
     * Adds the very first layer to the composition.
     */
    private void addBaseLayer(BufferedImage baseLayerImage) {
        assert hasNoLayers();

        addLayerWithoutUI(new ImageLayer(this,
            baseLayerImage, generateNewLayerName()));
    }

    /**
     * Creates a new empty (transparent) image layer and adds it to the composition.
     */
    public void addNewEmptyImageLayer(String name, boolean belowActive) {
        var newLayer = ImageLayer.createEmpty(this, name);
        getHolderForNewLayers().adder()
            .withHistory("New Empty Layer")
            .atPosition(belowActive ? BELOW_ACTIVE : ABOVE_ACTIVE)
            .skipCompUpdate()
            .add(newLayer);
    }

    public void addExternalImageAsNewLayer(BufferedImage image, String layerName, String editName) {
        Layer newLayer = ImageLayer.fromExternalImage(image, this, layerName);
        addWithHistory(newLayer, editName);
    }

    /**
     * Creates a new layer by merging all currently visible layers.
     */
    public void addNewLayerFromVisible() {
        var newLayer = new ImageLayer(this,
            getCompositeImage(), "Composite");

        new LayerAdder(this)
            .withHistory("New Layer from Visible")
            .skipCompUpdate()
            .atIndex(layerList.size())
            .add(newLayer);
    }

    /**
     * Duplicates the currently active layer and adds it above the original.
     */
    public void duplicateActiveLayer() {
        Layer duplicate = activeLayer.copy(CopyType.DUPLICATE_LAYER, true, this);
        if (duplicate == null) {
            // there was an out of memory error
            return;
        }
        getActiveHolder().addWithHistory(duplicate, "Duplicate Layer");
        assert checkInvariants();
    }

    /**
     * Flattens all visible layers into a single image layer.
     */
    public void flattenImage() {
        assert isActive();

        if (layerList.size() < 2) { // already flat
            return;
        }

        BufferedImage flattenedImg = getCompositeImage();
        Layer flattenedLayer = new ImageLayer(this, flattenedImg, "flattened");

        // add the flattened layer on top
        int numLayers = getNumLayers();
        adder()
            .atIndex(numLayers)
            .skipCompUpdate()
            .add(flattenedLayer);

        // remove all other layers
        for (int i = numLayers - 1; i >= 0; i--) {
            deleteLayer(layerList.get(i), false);
        }

        Layers.numLayersChanged(this, 1);
        History.add(new NotUndoableEdit("Flatten Image", this));
    }

    /**
     * Adds all layers in this composition to the UI.
     */
    public void addLayersToUI() {
        assert checkInvariants();

        Layer origActiveTopLevelLayer = activeTopLevelLayer;

        // this shouldn't change the active layer here,
        // but sets the last button to selected
        forEachTopLevelLayer(this::addLayerToGUI);
        assert activeTopLevelLayer == origActiveTopLevelLayer;

        // correct the selection
        LayerGUI ui = (LayerGUI) activeTopLevelLayer.getUI();
        if (!ui.isSelected()) {
            ui.setSelected(true);
        }
    }

    private void addLayerToGUI(Layer layer) {
        int layerIndex = layerList.indexOf(layer);
        view.addLayerToGUI(layer, layerIndex);
    }

    private void removeAllLayerUIs() {
        for (Layer layer : layerList) {
            LayerUI ui = layer.getUI();
            if (ui != null) {
                view.removeLayerUI(ui);
            }
        }
    }

    /**
     * Merges the active layer with the layer below it within the same holder.
     */
    public void mergeActiveLayerDown() {
        assert checkInvariants();

        LayerHolder holder = getActiveHolder();

        if (holder.canMergeDown(activeLayer)) {
            holder.mergeDown(activeLayer);
        }
    }

    @Override
    public void insertLayer(Layer layer, int index, boolean update) {
        if (update) {
            new LayerAdder(this).atIndex(index).add(layer);
        } else {
            layerList.add(index, layer);
        }
    }

    public void deleteActiveLayer(boolean addToHistory) {
        getActiveHolder().deleteLayer(activeLayer, addToHistory);
    }

    @Override
    public void deleteLayer(Layer layer, boolean addToHistory) {
        deleteLayer(layer, addToHistory, true);
    }

    public void deleteLayer(Layer layer, boolean addToHistory, boolean updateUI) {
        assert layer.getComp() == this;
        assert layer.getHolder() == this;
        assert layerList.size() >= 2;

        int deletedIndex = layerList.indexOf(layer);
        if (addToHistory) {
            History.add(new DeleteLayerEdit(this, layer, deletedIndex));
        }

        layerList.remove(layer);

        if (layer == activeLayer) {
            // the active layer was deleted, a new one must be selected
            Layer newActiveLayer = deletedIndex > 0
                ? layerList.get(deletedIndex - 1)
                : layerList.getFirst();
            setActiveLayer(newActiveLayer);
        }

        if (updateUI) {
            updateUIAfterLayerDeletion(layer);
        }
    }

    private void updateUIAfterLayerDeletion(Layer deletedLayer) {
        LayerUI ui = deletedLayer.getUI();
        if (ui == null) { // can be null if part of layer rasterization
            return;
        }

        view.removeLayerUI(ui);
        if (isActive()) {
            Layers.numLayersChanged(this, layerList.size());
        }
        update();
    }

    @Override
    public void deleteInternal(Layer layer) {
        layerList.remove(layer);
        if (layer.hasUI()) {
            view.removeLayerUI(layer.getUI());
        }
    }

    @Override
    public void reorderLayerUI(int oldIndex, int newIndex) {
        view.reorderLayerInUI(oldIndex, newIndex);
    }

    @Override
    public void removeLayerFromList(Layer layer) {
        layerList.remove(layer);
    }

    @Override
    public boolean canBeEmpty() {
        // unlike other layer holders, a composition can't be empty
        return false;
    }

    @Override
    public void replaceLayer(Layer before, Layer after) {
        boolean wasActiveTopLevel = before == activeTopLevelLayer;
        boolean containedActive = before.contains(activeLayer);

        before.transferMaskAndUITo(after);

        int layerIndex = layerList.indexOf(before);
        assert layerIndex != -1;
        layerList.set(layerIndex, after);

        if (wasActiveTopLevel) {
            activeTopLevelLayer = after;
        }
        if (containedActive) {
            // Avoids calling setActiveLayer, because it would set the mask view mode to NORMAL.

            // This means that after a rasterization undo, the active layer status
            // won't be restored correctly if a container of the active layer was
            // rasterized. However, normally rasterization is called on the active layer.
            activeLayer = after;
        }

        assert checkInvariants();
    }

    private void setActiveTopLevelLayer(Layer layer) {
        if (activeTopLevelLayer == layer) {
            return;
        }
        assert layerList.contains(layer)
            : format("new active top-level layer '%s' (%s, %s) not in the layer list of '%s'",
            layer.getName(), layer.getClass().getSimpleName(),
            System.identityHashCode(layer), getDebugName());

        activeTopLevelLayer = layer;

        if (activeTopLevelLayer.hasUI()) {
            activeTopLevelLayer.activateUI();
        }
    }

    // only sets the active layer reference directly
    public void setActiveLayerRef(Layer activeLayer) {
        this.activeLayer = activeLayer;
    }

    public void setActiveLayer(Layer activeLayer) {
        setActiveLayer(activeLayer, false, null);
    }

    /**
     * Sets the active layer, updating the UI and optionally adding history.
     */
    public void setActiveLayer(Layer layer, boolean addToHistory, String editName) {
        assert layer.getComp() == this;
        Layer prevActiveLayer = this.activeLayer;
        this.activeLayer = layer;

        // After ungrouping a group with a single active layer,
        // the active layer could change without a change in the target.
        setActiveTopLevelLayer(layer.getTopLevelLayer());

        if (this.activeLayer == prevActiveLayer) {
            return;
        }

        if (isActive()) {
            Tools.editingTargetChanged(layer);
            Layers.layerActivated(layer, true);

            if (prevActiveLayer != null) {
                prevActiveLayer.updateUI();
            }
            layer.updateUI();
        }

        if (addToHistory) {
            History.add(new LayerSelectionChangeEdit(editName, this, prevActiveLayer, layer));
        }
    }

    public boolean isActiveLayer(Layer layer) {
        return activeLayer == layer;
    }

    public Layer getActiveLayer() {
        return activeLayer;
    }

    public boolean isActiveTopLevelLayer(Layer layer) {
        return layer == activeTopLevelLayer;
    }

    public Layer getActiveTopLevelLayer() {
        return activeTopLevelLayer;
    }

    @Override
    public int getActiveLayerIndex() {
        return layerList.indexOf(activeTopLevelLayer);
    }

    @Override
    public int indexOf(Layer layer) {
        return layerList.indexOf(layer);
    }

    @Override
    public Layer getLayer(int i) {
        return layerList.get(i);
    }

    @Override
    public boolean listContainsLayer(Layer layer) {
        return layerList.contains(layer);
    }

    @Override
    public Stream<? extends Layer> levelStream() {
        return layerList.stream();
    }

    @Override
    public void addLayerToList(Layer newLayer, int index) {
        layerList.add(index, newLayer);
    }

    @Override
    public int getNumLayers() {
        return layerList.size();
    }

    /**
     * Returns the number of images exportable in OpenRaster format.
     */
    public int getNumORAExportableImages() {
        int[] count = {0};
        forEachNestedLayer(layer -> {
            if (layer.canExportORAImage()) {
                count[0]++;
            }
        }, false);
        return count[0];
    }

    /**
     * Calculates the total number of images in this composition including any mask images.
     */
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

    @Override
    public String getORAStackXML() {
        return "<stack>\n";
    }

    public void isolateActiveTopLevelLayer() {
        isolateLayer(activeTopLevelLayer, true);
    }

    /**
     * Shows only the given layer and hides all others.
     */
    public void isolateLayer(Layer layer, boolean addHistory) {
        if (addHistory) { // not undoing an "isolate"
            // check if we should undo the last isolation of the same layer
            if (History.getEditToBeUndone() instanceof IsolateEdit isolateEdit) {
                if (isolateEdit.getLayer() == layer) {
                    History.undo();
                    return;
                }
            }

            int numLayers = layerList.size();
            boolean[] backupVisibility = new boolean[numLayers];
            for (int i = 0; i < numLayers; i++) {
                backupVisibility[i] = layerList.get(i).isVisible();
            }
            History.add(new IsolateEdit(this, layer, backupVisibility));

            Messages.showStatusMessage("Layer <b>" + layer.getName() + "</b> was isolated.");
        }

        for (Layer other : layerList) {
            other.setVisible(other == layer);
        }
        update();
    }

    public void forEachTopLevelLayer(Consumer<Layer> action) {
        layerList.forEach(action);
    }

    public void forEachNestedLayerAndMask(Consumer<Layer> action) {
        forEachNestedLayer(action, true);
    }

    /**
     * Applies an action to all nested layers.
     * Traverses both layer group hierarchy and smart object content hierarchy.
     */
    public void forEachNestedLayer(Consumer<Layer> action, boolean includeMasks) {
        for (Layer layer : layerList) {
            layer.forEachNestedLayer(action, includeMasks);
        }
    }

    /**
     * Applies an action to all nested layers of a specific type.
     */
    @SuppressWarnings("unchecked")
    public <T extends Layer> void forEachNestedLayerOfType(Class<T> layerType, Consumer<T> action) {
        forEachNestedLayer(layer -> {
            if (layerType.isAssignableFrom(layer.getClass())) {
                action.accept((T) layer);
            }
        }, false);
    }

    /**
     * Applies the given action to all nested smart objects recursively.
     */
    public void forEachNestedSmartObject(Consumer<SmartObject> action) {
        forEachNestedLayerOfType(SmartObject.class, so -> so.forEachNestedSmartObject(action));
    }

    /**
     * Finds the topmost layer that is opaque at a given point on the canvas.
     */
    public Layer findLayerAtPoint(Point2D p) {
        // In mask editing mode never auto-select another layer
        if (getView().getMaskViewMode().showMask()) {
            return getActiveTopLevelLayer();
        }

        Point pixelLoc = new Point((int) p.getX(), (int) p.getY());

        // iterate in reverse order (we need to search layers from top to bottom)
        ListIterator<Layer> li = layerList.listIterator(layerList.size());
        while (li.hasPrevious()) {
            Layer layer = li.previous();
            if (!(layer instanceof ContentLayer contentLayer)) {
                continue;
            }
            if (!layer.isVisible() || layer.getOpacity() < 0.05f) {
                continue;
            }

            int pixel = contentLayer.getPixelAtPoint(pixelLoc);

            int pixelAlphaThreshold = 30;
            if (((pixel >> 24) & 0xFF) > pixelAlphaThreshold) {
                return layer;
            }
        }

        return null;
    }

    public Rectangle2D calcContentBounds(boolean includeTransparent) {
        return Utils.calcCombinedBounds(layerList, includeTransparent);
    }

    public void updateAllIconImages() {
        forEachNestedLayerAndMask(Layer::updateIconImage);
    }

    /**
     * Checks if the currently active target (layer or mask) can be drawn on.
     */
    public boolean canDrawOnActiveTarget() {
        return activeLayer instanceof Drawable || activeLayer.isMaskEditing();
    }

    private Layer getActiveTarget() {
        if (activeLayer.isMaskEditing()) {
            return activeLayer.getMask();
        }
        return activeLayer;
    }

    /**
     * Returns the active mask or image layer or null
     */
    public Drawable getActiveDrawable() {
        assert checkInvariants();
        if (activeLayer.isMaskEditing()) {
            return activeLayer.getMask();
        } else if (activeLayer instanceof Drawable dr) {
            return dr;
        } else {
            return null;
        }
    }

    public Filterable getActiveFilterable() {
        assert checkInvariants();
        if (activeLayer.isMaskEditing()) {
            return activeLayer.getMask();
        }
        if (activeLayer instanceof Filterable) {
            return (Filterable) activeLayer;
        }
        return null;
    }

    /**
     * Returns the active mask or image layer.
     * Calling this method assumes that the active layer is a {@link Drawable}.
     */
    public Drawable getActiveDrawableOrThrow() {
        Drawable dr = getActiveDrawable();
        if (dr == null) {
            throw new IllegalStateException("not drawable in '" + getName() + "': "
                + activeLayer.getClass().getSimpleName());
        }
        return dr;
    }

    /**
     * Called when the contents of one of the smart objects
     * belonging to this composition have changed.
     */
    @Override
    public void smartObjectChanged(boolean linked) {
        // invalidate cache as the visual representation will change
        invalidateImageCache();

        // mark this composition as dirty only if the changed content was
        // embedded within it => this composition is responsible for saving
        if (!linked) {
            setDirty(true);
        }

        // if this composition is itself content for other
        // smart objects, propagate the change upwards.
        if (isSmartObjectContent()) {
            for (SmartObject owner : owners) {
                owner.invalidateImageCache();

                Composition parent = owner.getComp();
                parent.smartObjectChanged(owner.isContentLinked());
            }
        }
    }

    /**
     * Prepares for moving the active layer/mask and/or the selection.
     */
    public void prepareMovement(MoveMode mode, boolean duplicateLayer) {
        if (mode.movesLayer()) {
            if (duplicateLayer) {
                duplicateActiveLayer();
            }

            getActiveTarget().prepareMovement();
        }
        if (mode.movesSelection()) {
            if (selection != null) {
                selection.prepareMovement();
            }
        }
    }

    /**
     * Updates the position of content/selection selection during a drag operation.
     */
    public void moveActiveContent(MoveMode mode, double imDx, double imDy) {
        if (mode.movesLayer()) {
            Layer target = getActiveTarget();
            target.moveWhileDragging(imDx, imDy);
            target.getHolder().invalidateImageCache();
        }
        if (mode.movesSelection() && selection != null) {
            selection.moveWhileDragging(imDx, imDy);
        }
        update();
    }

    /**
     * Finalizes a movement operation, adding history if changes occurred.
     */
    public void finalizeMovement(MoveMode mode) {
        PixelitorEdit layerEdit = null;
        if (mode.movesLayer()) {
            Layer target = getActiveTarget();
            // The target edit will be null if an adjustment
            // layer without a mask was moved.
            layerEdit = target.finalizeMovement();
        }

        PixelitorEdit selectionEdit = null;
        if (mode.movesSelection()) {
            if (selection != null) {
                selectionEdit = selection.finalizeMovement(false);
            }
        }

        var combinedEdit = MultiEdit.combine(
            layerEdit, selectionEdit, MoveMode.MOVE_BOTH.getEditName());
        if (combinedEdit != null) {
            History.add(combinedEdit);
            update();
        }
    }

    /**
     * Draws visual feedback (the bounding box) for the content being moved.
     */
    public void drawMovementContours(Graphics2D g, MoveMode mode) {
        if (mode.movesLayer()) {
            Layer target = getActiveTarget();
            if (target instanceof ContentLayer contentLayer) {
                Rectangle imBounds = contentLayer.getContentBounds();
                if (imBounds != null) {
                    Shapes.drawVisibly(g, view.imageToComponentSpace(imBounds));
                }
            }
        }
    }

    /**
     * Changes the position of a top-level layer in the layer stack.
     * <p>
     * The GUI doesn't have to be updated because this method is
     * called after a drag-and-drop reorder in the UI is completed.
     */
    public void changeStackIndex(Layer layer, int newIndex) {
        int oldIndex = layerList.indexOf(layer);
        assert oldIndex != -1;
        assert newIndex < layerList.size() : "oldIndex = " + oldIndex + ", newIndex = " + newIndex;

        if (oldIndex == newIndex) {
            return;
        }

        layerList.remove(layer);
        layerList.add(newIndex, layer);
        update();

        History.add(new LayerOrderChangeEdit(
            "Layer Reordering", this, oldIndex, newIndex));
    }

    public void createLayerUIs() {
        for (Layer layer : layerList) {
            layer.createUI();
        }
    }

    public void repaint() {
        view.repaint();
    }

    public void repaintRegion(PPoint start, PPoint end, double thickness) {
        invalidateImageCache();
        if (view != null) { // it might not be opened during image reloading
            view.repaintRegion(start, end, thickness);
            view.repaintNavigator(false);
        }
    }

    public void repaintRegion(PRectangle area) {
        invalidateImageCache();
        if (view != null) { // it might not be opened during image reloading
            view.repaintRegion(area);
            view.repaintNavigator(false);
        }
    }

    public void paintSelection(Graphics2D g) {
        if (draftSelection != null) {
            draftSelection.paintMarchingAnts(g);
        }
        if (selection != null) {
            selection.paintMarchingAnts(g);
        }
    }

    /**
     * Removes the current selection, optionally adding an undo/redo edit to the
     * history. The edit is always returned, allowing callers to embed it into
     * a composite edit. If there was no selection, then null is returned.
     */
    public DeselectEdit deselect(boolean addToHistory) {
        if (draftSelection != null) {
            draftSelection.dispose();
            draftSelection = null;
        }

        if (selection == null) {
            return null;
        }

        DeselectEdit edit = null;

        Shape shape = selection.getShape();
        boolean wasHidden = selection.isHidden();

        disposeSelection();

        if (shape != null) { // null for a simple click without a previous selection
            edit = new DeselectEdit(this, shape);
        }
        if (addToHistory && edit != null) {
            History.add(edit);
        }

        if (wasHidden && isActive()) {
            SelectionActions.getShowHide().setHideText();
        }
        return edit;
    }

    private void disposeSelection() {
        selection.dispose();
        setSelection(null);
    }

    public Selection getSelection() {
        return selection;
    }

    public boolean hasSelection() {
        return selection != null;
    }

    public Shape getSelectionShape() {
        return (selection != null) ? selection.getShape() : null;
    }

    public Selection getDraftSelection() {
        return draftSelection;
    }

    public boolean hasDraftSelection() {
        return draftSelection != null;
    }

    public void setDraftSelection(Selection selection) {
        draftSelection = selection;
    }

    /**
     * Changes the main selection based on a new shape, potentially combining
     * with the existing selection via user interaction (dialog).
     * Returns the undo edit for the change, or null if cancelled or no change occurred.
     */
    public PixelitorEdit changeSelection(Shape newShape) {
        newShape = clipToCanvasBounds(newShape);
        if (newShape.getBounds().isEmpty()) {
            // the new selection would be outside the canvas bounds
            return null;
        }

        if (selection == null) { // no existing selection
            // create a new selection
            setSelection(new Selection(newShape, view));
            return new NewSelectionEdit(this, selection.getShape());
        }

        // modify existing selection
        ShapeCombinator combinator = showShapeCombinatorDialog(this);
        if (combinator == null) {
            return null; // canceled
        }
        Shape origShape = selection.getShape();
        selection.setShape(combinator.combine(origShape, newShape));
        selection.setHidden(false, false);

        return new SelectionShapeChangeEdit("Selection Change", this, origShape);
    }

    private static ShapeCombinator showShapeCombinatorDialog(Composition comp) {
        String[] options = {"Replace", "Add", "Subtract", "Intersect", GUIText.CANCEL};
        String msg = "<html>There is already a selection on " + comp.getName() +
            ".<br>How do you want to combine new selection with the existing one?";

        int userChoice = Dialogs.showManyOptionsDialog(
            comp.getDialogParent(),
            "Existing Selection",
            msg,
            options,
            JOptionPane.QUESTION_MESSAGE);

        return switch (userChoice) {
            case 0 -> ShapeCombinator.REPLACE;
            case 1 -> ShapeCombinator.ADD;
            case 2 -> ShapeCombinator.SUBTRACT;
            case 3 -> ShapeCombinator.INTERSECT;
            case JOptionPane.CLOSED_OPTION, 4 -> null; // canceled
            default -> throw new IllegalStateException("userChoice = " + userChoice);
        };
    }

    /**
     * A shortcut for creating a selection without history.
     * It assumes that there is no existing selection.
     */
    public void createSelectionFrom(Shape shape) {
        if (selection != null) {
            throw new IllegalStateException("There is already a selection: " + selection);
        }
        setSelection(new Selection(shape, view));
    }

    /**
     * Changing the selection reference should be done only by using
     * this method (in order to make debugging easier).
     */
    public void setSelection(Selection selection) {
        this.selection = selection;
        if (isActive()) {
            SelectionActions.update(this);
            SelectionActions.getShowHide().updateTextFrom(selection);
        }
    }

    /**
     * Promotes a draft selection into a final one.
     */
    public void promoteSelection() {
        assert selection == null || !selection.isUsable() : "selection = " + selection;
        assert draftSelection != null;
        assert draftSelection.isUsable();

        setSelection(draftSelection);
        setDraftSelection(null);
    }

    /**
     * Applies the current selection as a clip to the given graphics.
     * It's assumed that the graphics is relative to the canvas:
     * if it's coming from the image of an {@link ImageLayer}, then
     * it must be translated before calling this.
     */
    public void applySelectionClipping(Graphics2D g2) {
        if (selection != null) {
            g2.setClip(selection.getShape());
        }
    }

    /**
     * Inverts the current selection relative to the canvas bounds.
     */
    public void invertSelection() {
        if (selection == null) {
            // alternatively we could select the whole canvas
            throw new IllegalStateException();
        }

        Shape origShape = selection.getShape();
        Shape invertedShape = canvas.invertShape(origShape);
        if (invertedShape.getBounds2D().isEmpty()) {
            // everything was selected, and now nothing is
            deselect(true);
        } else {
            selection.setShape(invertedShape);
            History.add(new SelectionShapeChangeEdit(
                "Invert Selection", this, origShape));
        }
    }

    /**
     * Modifies the selection by intersecting it with the given rectangle.
     */
    public void intersectSelectionWith(Rectangle2D rect) {
        if (selection != null) {
            Shape intersection = ShapeCombinator.INTERSECT.combine(
                selection.getShape(), rect);

            if (intersection.getBounds().isEmpty()) {
                disposeSelection();
            } else {
                selection.setShape(intersection);
            }
        }
    }

    /**
     * Called when the image-space coordinates have been changed by the
     * given transform (resize, crop, etc.).
     * The View argument is used because at this point the composition
     * might not be open in a view.
     */
    public void imCoordsChanged(AffineTransform at, boolean isUndoRedo, View view) {
        // The selection is explicitly reset to a backup shape
        // when something is undone/redone.
        if (selection != null && !isUndoRedo) {
            selection.transform(at);
        }
        // The paths and the tool widgets are transformed even for undo/redo.
        // The advantage is simpler code, the disadvantage is that
        // rounding errors could accumulate if the same operation is
        // undone/redone many times.
        if (paths != null) {
            paths.imCoordsChanged(at);
        }
        Tools.imCoordsChanged(at, view);
    }

    /**
     * Called when the component-space coordinates have changed,
     * but the pixels remain the same (zooming, view resizing, etc.).
     */
    public void coCoordsChanged() {
        if (guides != null) {
            guides.coCoordsChanged(view);
        }
    }

    /**
     * Returns the cached (canvas-sized) composite image, recalculating it if necessary.
     */
    public BufferedImage getCompositeImage() {
        if (compositeImage == null) {
            compositeImage = ImageUtils.calcComposite(layerList, canvas);
            assert compositeImage != null;
        }
        return compositeImage;
    }

    @Override
    public BufferedImage getImage() {
        BufferedImage image = getCompositeImage();
        if (image.isAlphaPremultiplied() || ImageUtils.isSubImage(image)) {
            image = ImageUtils.copyTo(BufferedImage.TYPE_INT_ARGB, image);
        }

        return image;
    }

    /**
     * Invalidates the cached composite image, forcing a
     * recalculation on the next call to {@link #getCompositeImage()}.
     */
    @Override
    public void invalidateImageCache() {
        if (compositeImage != null) {
            compositeImage.flush();
        }
        compositeImage = null;
    }

    @Override
    public void update() {
        update(true);
    }

    @Override
    public void update(boolean updateHistogram) {
        update(updateHistogram, false);
    }

    /**
     * Signals that the contents of this composition have been changed.
     */
    public void update(boolean updateHistogram, boolean canvasSizeChanged) {
        invalidateImageCache();

        if (isOpen()) {
            view.repaint();
            view.repaintNavigator(canvasSizeChanged);
        }

        if (updateHistogram) {
            HistogramsPanel.updateFrom(this);
        }
    }

    public boolean isActive() {
        return Views.activeCompIs(this);
    }

    /**
     * Useful for testing, but not exposed in the UI, because
     * it could create multiple undo events instead of just one.
     */
    public void allImageLayersToCanvasSize() {
        for (Layer layer : layerList) {
            if (layer instanceof ImageLayer imageLayer) {
                imageLayer.toCanvasSizeWithHistory();
            }
        }
    }

    /**
     * Resizes the active image layer to match canvas dimensions.
     */
    public void activeLayerToCanvasSize() {
        if (!(activeTopLevelLayer instanceof ImageLayer)) {
            Messages.showNotImageLayerError(activeTopLevelLayer);
            return;
        }

        ((ImageLayer) activeTopLevelLayer).toCanvasSizeWithHistory();
    }

    /**
     * Fits the canvas to the combined bounds of all content layers.
     * If any content layer is larger than the current canvas, the
     * canvas will be enlarged to fully contain all content layers.
     */
    public void fitCanvasToLayers() {
        Outsets enlargement = Outsets.createZero();

        for (Layer layer : layerList) {
            if (layer instanceof ContentLayer contentLayer) {
                enlargement.ensureFitsContentOf(contentLayer);
            }
        }

        if (enlargement.isZero()) {
            Dialogs.showInfoDialog(getDialogParent(), "Nothing To Be Done",
                "The canvas is already large enough to show all layer content.");
            return;
        }

        new EnlargeCanvas(enlargement).process(this);
    }

    /**
     * Recursively checks if this composition contains
     * the given layer at any nesting level.
     */
    public boolean contains(Layer searched) {
        for (Layer layer : layerList) {
            if (layer.contains(searched)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsLayerOfType(Class<? extends Layer> type) {
        for (Layer layer : layerList) {
            if (layer.containsLayerOfType(type)) {
                return true;
            }
        }
        return false;
    }

    // called from assertions and unit tests
    @SuppressWarnings("SameReturnValue")
    public boolean checkInvariants() {
        if (layerList.isEmpty()) {
            throw new AssertionError("no layer in " + getName());
        }
        if (activeTopLevelLayer == null) {
            throw new AssertionError("no active top-level layer in " + getName());
        }
        if (activeLayer == null) {
            throw new AssertionError("no active layer in " + getName());
        }
        if (activeTopLevelLayer.getComp() != this) {
            throw new AssertionError("bad comp in active top-level layer '%s' (that comp='%s', this='%s')".formatted(
                activeTopLevelLayer.getName(), activeTopLevelLayer.getComp().getDebugName(), getDebugName()));
        }
        if (activeLayer.getComp() != this) {
            throw new AssertionError("bad comp in active layer '%s' (that comp='%s', this='%s')".formatted(
                activeLayer.getName(), activeLayer.getComp().getDebugName(), getDebugName()));
        }
        if (!layerList.contains(activeTopLevelLayer)) {
            throw new AssertionError(format(
                "active top-level layer ('%s') not in list (%s)",
                activeTopLevelLayer.getName(), layerList));
        }

        if (!contains(activeLayer)) {
            throw new AssertionError("Active layer '%s' not contained in '%s'"
                .formatted(activeLayer.getName(), getDebugName()));
        }
        for (Layer layer : layerList) {
            if (layer.getHolder() != this) {
                throw new AssertionError("bad holder in layer '%s' (that holder='%s', this='%s')".formatted(
                    layer.getName(), layer.getHolder().getName(), getDebugName()));
            }
            assert layer.checkInvariants();
        }

        forEachNestedLayerAndMask(layer -> {
            if (layer.getComp() != this) {
                throw new AssertionError("bad comp in '%s' (that comp='%s', this='%s')".formatted(
                    layer.getName(), layer.getComp().getDebugName(), getDebugName()));
            }
        });

        // view consistency
        if (view != null && !view.isMock()) {
            if (view.getComp() != this) {
                throw new AssertionError("bad view reference for " + getDebugName()
                    + ", unexpected comp is " + view.getComp().getDebugName());
            }
            if (view.getCanvas() != canvas) {
                throw new AssertionError("bad canvas for " + getDebugName());
            }
        }

        // smart object owner consistency
        if (owners != null) {
            for (SmartObject owner : owners) {
                if (owner.getContent() != this) {
                    throw new AssertionError("bad owner reference for " + getDebugName());
                }
            }
        }

        // selection consistency
        // TODO update unit tests
//        if (selection != null && selection.getView() != view) {
//            throw new AssertionError("bad view in selection");
//        }
//        if (draftSelection != null && draftSelection.getView() != view) {
//            throw new AssertionError("bad view in draft selection");
//        }

        return true; // all checks passed
    }

    /**
     * Saves the current composition asynchronously.
     */
    public CompletableFuture<Void> saveAsync(SaveSettings saveSettings,
                                             boolean addToRecentFiles) {
        assert calledOnEDT() : threadInfo();

        // prevent concurrent processing of the same file path
        File targetFile = saveSettings.file();
        String filePath = targetFile.getAbsolutePath();
        if (IOTasks.isPathProcessing(filePath)) {
            Messages.showInfo("Save Busy",
                "The file " + targetFile.getName() + " is currently being processed.");
            return CompletableFuture.completedFuture(null);
        }
        IOTasks.markPathForWriting(filePath);

        // Set to not dirty already at the beginning of the saving process,
        // so that subsequent closing doesn't trigger another save.
        boolean wasDirty = isDirty();
        clearDirtyFlagsRecursively();

        FileFormat format = saveSettings.format();
        FileFormat.setLastSaved(format);
        Runnable saveTask = format.createSaveTask(this, saveSettings);

        return CompletableFuture
            .runAsync(saveTask, onIOThread)
            .handleAsync((v, e) -> {
                if (e != null) {
                    Messages.showException(e);
                    setDirty(wasDirty);
                } else {
                    handleSuccessfulSave(targetFile, addToRecentFiles);
                }
                IOTasks.markWritingComplete(filePath);
                return null;
            }, onEDT);
    }

    /**
     * Actions to perform on the EDT after a successful save.
     */
    public void handleSuccessfulSave(File file, boolean addToRecentFiles) {
        assert calledOnEDT() : threadInfo();

        setFile(file);
        if (addToRecentFiles) {
            RecentFilesMenu.INSTANCE.addRecentFile(file);
        }
        ImagePreviewPanel.removeThumbFromCache(file);
        Messages.showFileSavedMessage(file);

        if (isSmartObjectContent()) {
            // Otherwise the changes might not be propagated when deactivating,
            // because this isn't dirty after saving even if it's changed.
            for (SmartObject owner : owners) {
                owner.propagateContentChanges(this, true);

                // if this saved content was previously embedded,
                // ask the user if they want to link it now
                if (!owner.isContentLinked()) {
                    boolean link = Messages.showYesNoQuestion("Link Smart Object to File",
                        format("<html>Set <b>%s</b> as the linked contents of the smart object <b>%s</b>?",
                            file.getName(), owner.getName()));
                    if (link) {
                        owner.setLinkedContentFile(file);
                    }
                }
            }
        }
    }

    public Paths getPaths() {
        return paths;
    }

    public Path getActivePath() {
        return (paths != null) ? paths.getActivePath() : null;
    }

    public boolean hasActivePath() {
        return getActivePath() != null;
    }

    public void setActivePath(Path path) {
        if (path != null && path.getComp() != this) {
            throw new IllegalArgumentException(
                "path belongs to other comp, this = " + toPathDebugString() +
                    ", path.comp = " + path.getComp().toPathDebugString());
        }

        if (paths == null) {
            paths = new Paths();
        }
        paths.setActivePath(path);
    }

    public void pathChanged() {
        pathChanged(false);
    }

    public void pathChanged(boolean deleted) {
        forEachNestedLayerOfType(TextLayer.class, textLayer -> textLayer.pathChanged(deleted));

        setDirty(true);
    }

    /**
     * Creates a new path from a shape, makes it active, and optionally activates the Node tool.
     */
    public void createPathFromShape(Shape shape, boolean addToHistory, boolean activateNodeTool) {
        Path origActivePath = getActivePath();
        Path newPath = Shapes.shapeToPath(shape, getView());
        setActivePath(newPath);

        if (addToHistory) {
            History.add(new ConvertSelectionToPathEdit(this, shape, origActivePath));
        }

        if (activateNodeTool) {
            Tools.NODE.activate();
        }
    }

    public Guides getGuides() {
        return guides;
    }

    public void setGuides(Guides guides) {
        this.guides = guides;
    }

    public void clearGuides() {
        if (guides == null) {
            return;
        }
        History.add(new GuidesChangeEdit(this, guides, null));
        setGuides(null);
        setDirty(true);
        repaint();
    }

    public void drawGuides(Graphics2D g) {
        if (guides == null) {
            return;
        }
        guides.draw(g);
    }

    public void changeMode(ImageMode newMode) {
        if (newMode == mode) {
            return;
        }
        mode = newMode;
        for (Layer layer : layerList) {
            if (layer instanceof ImageLayer imageLayer) {
                imageLayer.convertMode(newMode);
            }
        }
        update();
    }

    public ImageMode getMode() {
        return mode;
    }

    /**
     * Checks if this composition or any of its smart object contents
     * need to be reloaded due to external file modifications.
     */
    public CompletableFuture<Composition> checkForExternalModifications() {
        // check only the open compositions here - hidden
        // smart object contents will be checked later
        if (file != null && isOpen()) {
            long currentFileTimestamp = file.lastModified();
            if (currentFileTimestamp > fileTimestamp) { // a newer version is on the disk
                fileTimestamp = currentFileTimestamp;
                Views.activate(view);
                boolean reload = Messages.showReloadFileQuestion(file);
                if (reload) {
                    return view.reloadCompAsync();
                }
            }
        }

        // if the whole composition wasn't reloaded, check nested
        // smart objects recursively, looking for linked contents
        CompletableFuture<Composition> reloadFuture = CompletableFuture.completedFuture(null);
        for (Layer layer : layerList) {
            if (layer instanceof SmartObject so) {
                // open contents are checked directly via the view
                if (!so.isContentOpen()) {
                    reloadFuture = reloadFuture.thenCompose(comp -> so.checkForAutoReload());
                }
            }
        }
        return reloadFuture;
    }

    /**
     * Closes the views associated with any nested smart object content compositions.
     */
    public void closeAllNestedComps() {
        forEachNestedSmartObject(so -> so.getContent().closeView());
    }

    /**
     * Checks invariants specifically for all nested smart objects.
     */
    private boolean checkAllSOInvariants() {
        forEachNestedSmartObject(SmartObject::checkInvariants);
        return true;
    }

    /**
     * Replaces this composition in its view with a new composition
     * containing a smart object that has this composition as its content.
     */
    public void replaceWithSmartObject() {
        // create the new outer composition
        Composition newOuterComp = new Composition(canvas.copy(), mode);
        if (file != null) {
            newOuterComp.setFile(file);
            setFile(null);
        } else {
            newOuterComp.setName(name);
        }
        newOuterComp.createDebugName();

        // create the smart object referencing this composition
        SmartObject so = new SmartObject(newOuterComp, this);

        // add the smart object to the new outer composition
        newOuterComp.addLayerWithoutUI(so);

        // replace the composition in the view
        History.add(new CompositionReplacedEdit("Convert All to Smart Object",
            view, this, newOuterComp, null, false));
        view.replaceComp(newOuterComp);

        setName("Contents of " + name);
    }

    /**
     * Converts the visible top-level layers into a new smart object.
     */
    public void convertVisibleLayersToSmartObject() {
        long visibleCount = layerList.stream()
            .filter(Layer::isVisible)
            .count();
        if (visibleCount == 0) {
            Messages.showNoVisibleLayersError(this);
            return;
        }
        if (visibleCount == layerList.size()) {
            // all layers are visible, equivalent to converting the whole composition
            replaceWithSmartObject();
            return;
        }

        // create the new content composition
        Composition content = new Composition(canvas.copy(), mode);
        content.setName("visible");

        // create a copy of the current composition to become the new main one
        Composition newMainComp = copy(CopyType.UNDO, true);

        // move visible layers from the copy to the content composition
        List<Layer> visibleLayers = newMainComp.layerList.stream()
            .filter(Layer::isVisible)
            .toList();
        for (Layer layer : visibleLayers) {
            newMainComp.deleteLayer(layer, false, false);
            layer.setComp(content);
            content.addLayerWithoutUI(layer);
        }

        // create the new smart object and add it to the new main composition
        SmartObject so = new SmartObject(newMainComp, content);
        newMainComp.addLayerWithoutUI(so);

        History.add(new CompositionReplacedEdit("Convert Visible to Smart Object",
            view, this, newMainComp, null, false));
        view.replaceComp(newMainComp);
    }

    /**
     * Creates a shallow copy (clone) of the given smart object and adds it.
     */
    public void shallowDuplicate(SmartObject so) {
        if (so != activeTopLevelLayer) {
            setActiveLayer(so);
        }

        addWithHistory(so.shallowDuplicate(), "Clone");
    }

    /**
     * Checks if all fonts of all text layers can be found on the current machine.
     */
    public void checkFontsAreInstalled() {
        assert calledOnEDT();
        forEachNestedLayerOfType(TextLayer.class, TextLayer::checkFontIsInstalled);
    }

    @Override
    public DebugNode createDebugNode(String name) {
        DebugNode node = new DebugNode(name, this);

        node.add(canvas.createDebugNode("canvas"));

        node.add(activeTopLevelLayer.createDebugNode("active top-level layer"));
        node.add(activeLayer.createDebugNode("active layer"));

        forEachTopLevelLayer(layer -> node.add(layer.createDebugNode()));

        node.add(createBufferedImageNode("composite image", getCompositeImage()));

        node.addNullableDebuggable("paths", paths);
        node.addNullableDebuggable("guides", guides);

        node.addInt("num layers", getNumLayers());
        node.addQuotedString("name", getName());
        node.addQuotedString("debug name", getDebugName());

        node.addNullableDebuggable("file", file, DebugNodes::createFileNode);

        node.addBoolean("is smart object content", isSmartObjectContent());
        if (isSmartObjectContent()) {
            DebugNode ownersNode = new DebugNode("referencing SO owner names", owners);
            for (SmartObject owner : owners) {
                ownersNode.addString("name", owner.getName());
            }
            node.add(ownersNode);
        }

        node.addBoolean("dirty", isDirty());

        node.addNullableDebuggable("draft selection", draftSelection);
        node.addNullableDebuggable("selection", selection);

        return node;
    }

    private String toPathDebugString() {
        return "Composition{'" + debugName + '\''
            + ", active = " + isActive()
            + ", path = " + getActivePath()
            + '}';
    }

    @Override
    public String toString() {
        return "Composition ('" + debugName + "')";
    }
}
