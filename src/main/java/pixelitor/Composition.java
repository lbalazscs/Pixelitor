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

package pixelitor;

import pixelitor.compactions.EnlargeCanvas;
import pixelitor.compactions.Outsets;
import pixelitor.gui.HistogramsPanel;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.ImagePreviewPanel;
import pixelitor.guides.Guides;
import pixelitor.guides.GuidesChangeEdit;
import pixelitor.history.*;
import pixelitor.io.*;
import pixelitor.layers.*;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.selection.SelectionChangeResult;
import pixelitor.selection.ShapeCombinator;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.move.MoveMode;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.Paths;
import pixelitor.tools.pen.history.ConvertSelectionToPathEdit;
import pixelitor.tools.selection.SelectionChangeListener;
import pixelitor.tools.util.PPoint;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.String.format;
import static pixelitor.layers.LayerAdder.Position.ABOVE_ACTIVE;
import static pixelitor.layers.LayerAdder.Position.BELOW_ACTIVE;
import static pixelitor.utils.Threads.*;
import static pixelitor.utils.debug.DebugNodes.createBufferedImageNode;

/**
 * An image composition containing multiple layers, paths, guides, and overall state.
 */
public class Composition implements Serializable, ImageSource, LayerHolder {
    // serialization is used for saving in the pxc format
    @Serial
    private static final long serialVersionUID = 1L;

    private static final AtomicLong debugIdCounter = new AtomicLong();

    public static final int DEFAULT_DPI = 300;

    private String name;

    // the top-level layers of the composition
    private final List<Layer> layerList = new ArrayList<>();

    // the currently selected layer, potentially nested within groups or smart objects
    private Layer activeLayer;

    // a counter for the names of new layers
    private int newLayerCount = 1;

    private final Canvas canvas;
    private Paths paths;
    private Guides guides;
    private ImageMode mode;

    // defines the intended physical size of the image when printed
    private int dpi;

    //
    // transient fields from here
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

    // the current, finalized selection
    private transient Selection selection;

    // a temporary selection that is currently being created
    // by dragging, but it's not finalized yet
    private transient Selection draftSelection;

    /**
     * Private constructor. Use static factory methods
     * or deserialization to create instances.
     */
    private Composition(Canvas canvas, ImageMode mode, int dpi) {
        assert canvas != null;
        this.canvas = canvas;
        this.mode = mode;
        this.dpi = dpi;
    }

    public static Composition fromImage(BufferedImage img, File file, String name) {
        return fromImage(img, file, name, DEFAULT_DPI);
    }

    /**
     * Creates a single-layered composition from the given image.
     */
    public static Composition fromImage(BufferedImage img, File file, String name, int dpi) {
        assert img != null;
        Canvas canvas = new Canvas(img.getWidth(), img.getHeight());

        ImageMode mode;
        if (Features.enableImageMode && img.getColorModel() instanceof IndexColorModel) {
            mode = ImageMode.INDEXED;
        } else {
            mode = ImageMode.RGB;
            img = ImageUtils.toSysCompatibleImage(img);
        }

        var comp = new Composition(canvas, mode, dpi);
        comp.addBaseLayer(img);

        if (file != null) {
            comp.setFile(file); // also sets the name based on the file name
        } else if (name != null) {
            comp.setName(name);
        } else {
            throw new IllegalArgumentException("must be given a file or a name");
        }
        comp.initDebugName();
        assert comp.getName() != null;
        return comp;
    }

    /**
     * Creates an empty composition (no layers, no name).
     */
    public static Composition createEmpty(int width, int height, ImageMode mode) {
        Canvas canvas = new Canvas(width, height);
        return new Composition(canvas, mode, DEFAULT_DPI);
    }

    /**
     * Creates a composition with a single, transparent image layer.
     */
    public static Composition createTransparent(Canvas canvas) {
        BufferedImage img = ImageUtils.createSysCompatibleImage(canvas);
        return fromImage(img, null, "transparent");
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // initialize transient variables
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

        if (dpi == 0) { // migrate old pxc files
            dpi = DEFAULT_DPI;
        }

        if (activeLayer == null) {
            // recover from corrupted pxc file
            activeLayer = layerList.getFirst();
        }

        // perform actions that need a full canvas and also
        // (re)load the contents of linked smart objects
        forEachNestedLayerOfType(CompositeLayer.class, CompositeLayer::afterDeserialization);

        initDebugName();
        assert checkAllSOInvariants();
    }

    /**
     * Creates and returns a deep copy of this composition.
     * The copy will have no view set.
     * In the case of undo, the view will be transferred later.
     * (Two comps should never point to the same view).
     */
    public Composition copy(CopyOptions options) {
        assert checkInvariants();
        var compCopy = new Composition(canvas.copy(), mode, dpi);

        // copy layers recursively
        for (Layer layer : layerList) {
            // Layer.copy handles recursion, and also sets the
            // active layer of the copied composition
            var layerCopy = layer.copy(options, compCopy);

            // fully set up only the top-level stuff here
            layerCopy.setHolder(compCopy);
            compCopy.layerList.add(layerCopy);
        }

        compCopy.newLayerCount = newLayerCount;

        if (options.copySelection() && selection != null) {
            compCopy.setSelection(new Selection(selection));
        }
        if (options.copyGuides() && guides != null) {
            compCopy.guides = guides.copy(view);
        }
        if (paths != null) {
            compCopy.paths = paths.deepCopy(compCopy);
        }

        if (options.preserveFileState()) {
            compCopy.dirty = dirty;
            compCopy.file = file;
            compCopy.fileTimestamp = fileTimestamp;
        }

        compCopy.name = options.createCompCopyName(name);

        compCopy.initDebugName();

        assert checkInvariants();
        assert compCopy.checkInvariants();

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
            // propagate the new view reference to all child objects
            if (selection != null) {
                selection.setView(view);
            }
            if (draftSelection != null) {
                draftSelection.setView(view);
            }
            if (paths != null) {
                paths.setView(view);
            }
            // guides don't store a view, and Guides.coCoordsChanged will be called elsewhere

            // now that all children are initialized, it's safe to trigger updates
            canvas.recalcCoSize(view, true);
        } else { // the new view can be null when closing or reloading
            if (selection != null) {
                disposeSelection();
            }
            if (draftSelection != null) {
                draftSelection.dispose();
                draftSelection = null;
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

    // TODO there is currently no removeOwner method (called when a
    //   smart object is deleted), because undo must be considered
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
        // if a content file is opened independently of its parent,
        // then this will return false, even for PXC files
        return owners != null;
    }

    public void addLinkedSmartObject() {
        File linkedContentFile = FileChoosers.selectSupportedOpenFile();
        if (linkedContentFile == null) {
            return;
        }

        FileIO.loadCompAsync(linkedContentFile)
            .thenAcceptAsync(content ->
                getHolderForNewLayers().addWithHistory(
                    new SmartObject(linkedContentFile, this, content),
                    "Add Linked Smart Object"
                ), onEDT)
            .exceptionally(Messages::showExceptionOnEDT);
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        if (this.dpi == dpi) {
            return;
        }
        this.dpi = dpi;

        // set explicitly, since there is no undo edit for changing the DPI
        setDirty(true);
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
        return canvas.intersect(shape);
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

        // if this is the content of a smart object, the dirty flag
        // is relevant only if the content is linked (not embedded)
        if (isSmartObjectContent()) {
            for (SmartObject owner : owners) {
                if (owner.isContentEmbedded()) {
                    // if embedded anywhere, the parent saves it
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

    /**
     * Collects all dirty compositions in this saving hierarchy, clears their
     * dirty flags, and returns them for rollback in case of an error.
     */
    private List<Composition> collectAndClearDirtyComps() {
        List<Composition> cleared = new ArrayList<>();
        if (isDirty()) {
            cleared.add(this);
            setDirty(false);
        }
        forEachNestedSmartObject(so -> {
            if (so.findSavingComp() == this) {
                Composition content = so.getContent();
                if (content.isDirty()) {
                    cleared.add(content);
                    content.setDirty(false);
                }
            }
        });
        return cleared;
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
        removeTopLevelLayerUIs();
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
            // that all owners share the same root composition.
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

        if (isActive() && !AppMode.isUnitTesting()) {
            view.updateTitle();
            PixelitorWindow.get().updateTitle(this);
        }
    }

    /**
     * Creates a file name to suggest in the save dialog.
     */
    public String suggestFileName(String extension) {
        if (file == null) {
            return name + "." + extension;
        } else {
            return FileUtils.replaceExtension(file.getName(), extension);
        }
    }

    /**
     * Calculates the title shown in the window title bar.
     */
    public String calcWindowTitle() {
        String baseTitle = dirty ? "* " + name : name;
        return baseTitle + " (" + canvas.getSizeString() + ")";
    }

    /**
     * Renames the composition and adds a history entry.
     */
    public void rename(String oldName, String newName) {
        if (newName == null) { // can happen if the user canceled or closed the dialog
            return;
        }
        if (newName.equals(oldName)) {
            return;
        }
        setName(newName);
        History.add(new CompositionRenameEdit(this, oldName, newName));
    }

    public String getDebugName() {
        return debugName;
    }

    public void initDebugName() {
        assert name != null;
        assert debugName == null;

        this.debugName = name + " " + debugIdCounter.getAndIncrement();
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
            this.fileTimestamp = 0;
            return;
        }
        this.fileTimestamp = file.lastModified();
        setName(file.getName());
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
        getHolderForNewLayers().addWithHistory(newLayer, editName);
    }

    /**
     * Creates a new layer by merging all currently visible layers.
     */
    public void addNewLayerFromVisible() {
        var newLayer = new ImageLayer(this,
            getCompositeImage(), "Composite");

        adder()
            .withHistory("New Layer from Visible")
            .skipCompUpdate()
            .atIndex(layerList.size())
            .add(newLayer);
    }

    /**
     * Duplicates the currently active layer and adds it above the original.
     */
    public void duplicateActiveLayer() {
        Layer duplicate = activeLayer.copy(CopyOptions.duplicateLayer(), this);
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

        // create the new flattened layer from the composite of visible layers
        BufferedImage flattenedImg = getCompositeImage();
        Layer flattenedLayer = new ImageLayer(this, flattenedImg, "flattened");

        // clear old layers
        removeTopLevelLayerUIs();
        layerList.clear();

        // add the flattened layer, with a single ui update at the end
        adder().add(flattenedLayer);

        History.add(new NotUndoableEdit("Flatten Image", this));
    }

    /**
     * Adds all layers in this composition to the UI.
     */
    public void addLayersToUI() {
        assert checkInvariants();

        view.addAllLayerUIs(layerList);
        fireLayerUICountChanged();
    }

    public void fireLayerUICountChanged() {
        if (view.isActive() && isHolderOfActiveLayer()) {
            LayerEvents.fireLayerCountChanged(this, getNumLayers());
        }
    }

    private void removeTopLevelLayerUIs() {
        view.removeAllLayerUIs();
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
    public void insertLayer(Layer newLayer, int index, boolean update) {
        if (update) {
            adder().atIndex(index).add(newLayer);
        } else {
            layerList.add(index, newLayer);
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
        assert layer.isDirectChildOf(this);
        assert layerList.size() >= 2;

        int deletedIndex = layerList.indexOf(layer);
        if (addToHistory) {
            History.add(new DeleteLayerEdit(this, layer, deletedIndex));
        }

        layerList.remove(deletedIndex);

        if (layer.contains(activeLayer)) {
            // the active layer was deleted, a new one must be activated
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
            LayerEvents.fireLayerCountChanged(this, layerList.size());
        }
        update();
    }

    @Override
    public void reorderLayerUI(int oldIndex, int newIndex) {
        view.reorderLayerUI(oldIndex, newIndex);
    }

    @Override
    public void removeDirectChild(Layer layer, boolean removeUI) {
        layerList.remove(layer);

        if (removeUI && isOpen() && layer.hasUI()) {
            view.removeLayerUI(layer.getUI());
        }
    }

    @Override
    public boolean canBeEmpty() {
        // unlike other layer holders, a composition can't be empty
        return false;
    }

    @Override
    public void replaceLayer(Layer before, Layer after) {
        boolean containedActive = before.contains(activeLayer);

        before.transferMaskAndUITo(after);

        int layerIndex = layerList.indexOf(before);
        assert layerIndex != -1;
        layerList.set(layerIndex, after);

        if (containedActive) {
            // Avoids calling setActiveLayer, because it would set the mask view mode to NORMAL.

            // This means that after a rasterization undo, the active layer status
            // won't be restored correctly if a container of the active layer was
            // rasterized. However, normally rasterization is called on the active layer.
            activeLayer = after;
        }

        assert checkInvariants();
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
        assert layer.getComp() == this && contains(layer);

        if (layer == this.activeLayer) {
            return;
        }

        Layer prevActiveLayer = this.activeLayer;
        this.activeLayer = layer;

        if (isActive()) {
            Tools.editingTargetChanged(layer);
            LayerEvents.fireLayerActivated(layer, true);

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

    @Override
    public int indexOf(Layer layer) {
        return layerList.indexOf(layer);
    }

    @Override
    public Layer getLayer(int i) {
        return layerList.get(i);
    }

    @Override
    public boolean hasDirectChild(Layer layer) {
        return layerList.contains(layer);
    }

    @Override
    public Stream<? extends Layer> directChildrenStream() {
        return layerList.stream();
    }

    @Override
    public void insertDirectChild(Layer newLayer, int index) {
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
     * Returns the total number of images in this composition including any mask images.
     */
    public int getNumImages() {
        int[] count = {0};
        forEachNestedLayer(layer -> {
            if (layer instanceof ImageLayer) {
                count[0]++;
            }
            if (layer.hasMask()) {
                count[0]++;
            }
        }, false);
        return count[0];
    }

    @Override
    public String getORAStackXML() {
        return "<stack>\n";
    }

    // this is the only GUI entry point for isolation =>
    // only the isolation of top-level layers is supported
    public void isolateActiveTopLevelLayer() {
        isolateLayer(activeLayer.getTopLevelLayer(), true);
    }

    /**
     * Shows only the given layer and hides all others.
     */
    public void isolateLayer(Layer layer, boolean addToHistory) {
        if (!layer.isTopLevel()) {
            // TODO currently only top-level layers can be isolated
            return;
        }

        if (addToHistory) { // not undoing an "isolate"
            // check if we should undo the last isolation of the same layer
            if (IsolateEdit.undoIfIsolating(layer)) {
                return;
            }

            int numLayers = layerList.size();
            boolean[] backupVisibility = new boolean[numLayers];
            for (int i = 0; i < numLayers; i++) {
                backupVisibility[i] = layerList.get(i).isVisible();
            }
            History.add(new IsolateEdit(this, layer, backupVisibility));

            Messages.showStatusMessage("Layer <b>" + layer.getName() + "</b> was isolated.");
        }

        // assumes that the isolated layer is top-level
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
     * Applies an action to all nested layers, traversing groups and smart objects.
     */
    public void forEachNestedLayer(Consumer<Layer> action, boolean includeMasks) {
        for (Layer layer : layerList) {
            layer.forEachNestedLayer(action, includeMasks);
        }
    }

    /**
     * Applies an action to all nested layers of a specific type.
     */
    public <T extends Layer> void forEachNestedLayerOfType(Class<T> layerType, Consumer<T> action) {
        forEachNestedLayer(layer -> {
            if (layerType.isInstance(layer)) {
                action.accept(layerType.cast(layer));
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
     * Recursively searches for any layer that satisfies the given predicate.
     */
    public Layer findFirstLayerWhere(Predicate<Layer> predicate, boolean includeMasks) {
        for (Layer layer : layerList) {
            Layer foundLayer = layer.findFirstLayerWhere(predicate, includeMasks);
            if (foundLayer != null) {
                return foundLayer;
            }
        }
        return null;
    }

    /**
     * Finds the topmost layer that is opaque at a given image-space point.
     */
    public Layer findLayerAtPoint(Point p) {
        assert isOpen();

        // in mask editing mode never auto-select another layer
        if (getView().getMaskViewMode().isShowingMask()) {
            return getActiveLayer();
        }

        // iterate in reverse order to search layers from top to bottom
        return ContentLayer.findOpaqueInList(layerList, p);
    }

    public Rectangle2D calcContentBounds(boolean includeTransparent) {
        return Utils.calcCombinedBounds(layerList, includeTransparent);
    }

    public void setMaskViewMode(MaskViewMode maskViewMode, Layer activeLayer) {
        view.setMaskViewMode(maskViewMode, activeLayer);
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

    private Layer getActiveMoveTarget() {
        return activeLayer.isMaskEditing() ? activeLayer.getMask() : activeLayer;
    }

    /**
     * Returns the active {@link Drawable} (layer or mask), or null if none is active.
     */
    public Drawable getActiveDrawable() {
        assert checkInvariants();
        if (activeLayer.isMaskEditing()) {
            return activeLayer.getMask();
        }
        return activeLayer instanceof Drawable dr ? dr : null;
    }

    /**
     * Returns the active {@link Filterable} (layer or mask), or null if none is active.
     */
    public Filterable getActiveFilterable() {
        assert checkInvariants();
        if (activeLayer.isMaskEditing()) {
            return activeLayer.getMask();
        }
        return activeLayer instanceof Filterable f ? f : null;
    }

    /**
     * Returns the active {@link Drawable} (layer or mask), or throws an exception if none is active.
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
        // smart objects, propagate the change upwards
        // (the GUI must ensure that the composition graph is acyclic)
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

            getActiveMoveTarget().prepareMovement();
        }
        if (mode.movesSelection()) {
            if (selection != null) {
                selection.prepareForTransform();
            }
        }
    }

    /**
     * Updates the position of a content layer/selection during a drag operation (Move Tool).
     */
    public void moveActiveContent(MoveMode mode, double imDx, double imDy) {
        if (mode.movesLayer()) {
            Layer target = getActiveMoveTarget();
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
            Layer target = getActiveMoveTarget();

            // will be null if a non-content layer without mask was moved
            layerEdit = target.finalizeMovement();
        }

        PixelitorEdit selectionEdit = null;
        if (mode.movesSelection()) {
            if (selection != null) {
                selectionEdit = selection.finalizeTransform();
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
            Layer target = getActiveMoveTarget();
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
    public void reorderTopLevelLayer(Layer layer, int newIndex) {
        int oldIndex = layerList.indexOf(layer);
        assert oldIndex != -1;
        assert newIndex >= 0 && newIndex < layerList.size()
            : "oldIndex = " + oldIndex + ", newIndex = " + newIndex;

        if (oldIndex == newIndex) {
            return;
        }

        layerList.remove(oldIndex);
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
     * Removes the current selection and cancels any draft selection,
     * optionally adding an undo/redo edit to the history.
     * The edit is returned, allowing callers to embed it into a
     * composite edit. If there was no selection, then null is returned.
     */
    public DeselectEdit deselect(boolean addToHistory) {
        if (draftSelection != null) {
            draftSelection.dispose();
            draftSelection = null;
        }

        if (selection == null) {
            return null;
        }

        Shape shape = selection.getShape();
        boolean wasHidden = selection.isHidden();

        disposeSelection();

        DeselectEdit edit = new DeselectEdit(this, shape);
        if (addToHistory) {
            History.add(edit);
        }

        if (isActive()) {
            if (wasHidden) {
                SelectionActions.getShowHide().setHideText();
            }
            Tool activeTool = Tools.getActive();
            if (activeTool instanceof SelectionChangeListener listener) {
                listener.selectionDeleted();
            }
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
     * Changes the selection based on a new shape, potentially combining
     * with the existing selection via user interaction (dialog).
     */
    public SelectionChangeResult updateSelectionInteractively(Shape newShape) {
        newShape = clipToCanvasBounds(newShape);
        if (newShape.getBounds().isEmpty()) {
            // the new shape is entirely outside the canvas
            return SelectionChangeResult.outOfBounds();
        }

        if (selection == null) { // no existing selection
            // a new selection is created successfully.
            setSelection(new Selection(newShape, view));
            return SelectionChangeResult.success(
                new NewSelectionEdit(this, selection.getShape()));
        }

        // modify existing selection
        ShapeCombinator combinator = Dialogs.selectShapeCombinator(this);
        if (combinator == null) {
            // the user canceled the dialog
            return SelectionChangeResult.cancelled();
        }

        Shape origShape = selection.getShape();
        Shape combinedShape = combinator.combine(origShape, newShape);

        if (combinedShape.getBounds().isEmpty()) {
            // the combination resulted in an empty shape => deselect
            deselect(false);
            return SelectionChangeResult.success(new DeselectEdit(this, origShape));
        } else {
            // the selection was successfully modified to a new, non-empty shape
            selection.setShape(combinedShape);
            selection.setHidden(false);
            return SelectionChangeResult.success(new SelectionShapeChangeEdit(
                combinator.getHistoryName(), this, origShape));
        }
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
        }
    }

    /**
     * Promotes a draft selection into a final one.
     */
    public void promoteSelection() {
        assert draftSelection != null;
        assert draftSelection.isValid();

        if (selection != null) {
            selection.dispose();
        }

        setSelection(draftSelection);
        setDraftSelection(null);
    }

    /**
     * Applies the current selection as a clip to the given graphics.
     * It's assumed that the graphics context is relative to the canvas:
     * if it's coming from the image of an {@link ImageLayer}, then
     * it must be translated before calling this.
     */
    public void applySelectionClipping(Graphics2D g) {
        if (selection != null) {
            g.setClip(selection.getShape());
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
            Tools.notifySelectionChanged();
        }
    }

    /**
     * Called when the image-space coordinates have been changed by the
     * given transform (resize, crop, etc.).
     * The View argument is used because at this point the composition
     * might not be open in a view.
     */
    public void imCoordsChanged(AffineTransform at, boolean isUndoRedo, View view) {
        // the selection is explicitly reset to a backup shape
        // when something is undone/redone
        if (selection != null && !isUndoRedo) {
            selection.transform(at);
        }

        // the paths and the tool widgets are transformed even for undo/redo
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
        update(false);
    }

    /**
     * Signals that the contents of this composition have been changed.
     */
    public void update(boolean canvasSizeChanged) {
        invalidateImageCache();

        if (isOpen()) {
            view.repaint();
            view.repaintNavigator(canvasSizeChanged);
        }

        HistogramsPanel.updateFrom(this);
    }

    public boolean isActive() {
        return Views.getActiveComp() == this;
    }

    /**
     * Useful for testing, but not exposed in the UI, because
     * it could create multiple undo events instead of just one.
     */
    public void cropAllImageLayersToCanvasSize() {
        forEachNestedLayerOfType(ImageLayer.class, ImageLayer::cropToCanvasSizeWithHistory);
    }

    /**
     * Crops the active image layer to match canvas dimensions.
     */
    public void cropActiveLayerToCanvasSize() {
        if (activeLayer instanceof ImageLayer imageLayer) {
            imageLayer.cropToCanvasSizeWithHistory();
        } else {
            Messages.showNotImageLayerError(activeLayer);
        }
    }

    /**
     * Enlarges the canvas to fit the content of all layers.
     */
    public void fitCanvasToLayers() {
        Rectangle2D bounds = calcContentBounds(true);
        if (bounds != null && !bounds.isEmpty()) {
            int left = Math.max(0, (int) Math.ceil(-bounds.getMinX()));
            int top = Math.max(0, (int) Math.ceil(-bounds.getMinY()));
            int right = Math.max(0, (int) Math.ceil(bounds.getMaxX() - canvas.getWidth()));
            int bottom = Math.max(0, (int) Math.ceil(bounds.getMaxY() - canvas.getHeight()));

            Outsets enlargement = new Outsets(top, right, bottom, left);
            if (!enlargement.isZero()) {
                new EnlargeCanvas(enlargement).process(this);
                return;
            }
        }

        Dialogs.showInfo(getDialogParent(), "Nothing To Be Done",
            "The canvas is already large enough to show all layer content.");
    }

    /**
     * Recursively checks if this composition contains
     * the given layer at any nesting level.
     */
    @Override
    public boolean contains(Layer searched) {
        for (Layer layer : layerList) {
            if (layer.contains(searched)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively checks if this composition contains
     * a layer of the given type at any nesting level.
     * Content compositions inside smart objects are not checked.
     */
    @Override
    public boolean containsLayerOfType(Class<? extends Layer> type) {
        for (Layer layer : layerList) {
            if (layer.containsLayerOfType(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Saves the current composition asynchronously.
     */
    public CompletableFuture<Void> saveAsync(SaveSettings saveSettings,
                                             boolean addToRecentFiles) {
        assert calledOnEDT() : callInfo();

        // prevent concurrent processing of the same file path
        File targetFile = saveSettings.file();
        String filePath = targetFile.getAbsolutePath();
        if (IOTasks.isPathProcessing(filePath)) {
            Messages.showInfo("Save Busy",
                "The file " + targetFile.getName()
                    + " is currently being processed.");
            return CompletableFuture.completedFuture(null);
        }
        IOTasks.markPathForWriting(filePath);

        // cleared at the start of the saving process
        // so that a subsequent close does not trigger another save
        List<Composition> clearedDirtyComps = collectAndClearDirtyComps();

        FileFormat format = saveSettings.format();
        FileFormat.setLastSaved(format);
        Runnable saveTask = format.createSaveTask(this, saveSettings);

        return CompletableFuture
            .runAsync(saveTask, onIOThread)
            .handleAsync((v, e) -> {
                if (e != null) {
                    Messages.showException(e);
                    clearedDirtyComps.forEach(c -> c.setDirty(true));
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
        assert calledOnEDT() : callInfo();

        setFile(file);
        if (addToRecentFiles) {
            RecentFilesMenu.INSTANCE.addRecentFile(file);
        }
        ImagePreviewPanel.removeThumbFromCache(file);
        Messages.showFileSavedMessage(file);

        if (isSmartObjectContent()) {
            // otherwise the changes might not be propagated when deactivating,
            // because this composition is no longer dirty after saving, even though it's changed
            for (SmartObject owner : owners) {
                owner.propagateContentChanges(this, true);

                // if this saved content was previously embedded,
                // ask the user if they want to link it now
                if (!owner.isContentLinked()) {
                    boolean link = Messages.showYesNoQuestion("Link Smart Object to File",
                        format("<html>Set <b>%s</b> as the linked content of the smart object <b>%s</b>?",
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
                "path belongs to another comp, this = " + toPathDebugString() +
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
     * Creates a new path from an image-space shape, makes it active, and optionally activates the Node tool.
     */
    public void createPathFromImShape(Shape shape, boolean addToHistory, boolean activateNodeTool) {
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
        forEachNestedLayerOfType(ImageLayer.class, layer -> layer.convertMode(newMode));
        setDirty(true);
        update();
    }

    public ImageMode getMode() {
        return mode;
    }

    /**
     * Checks if this composition or any of its smart object contents
     * need to be reloaded due to external file modifications.
     */
    public CompletableFuture<Composition> checkAutoReload() {
        // check only the open compositions here; hidden
        // smart object contents will be checked later
        if (file != null && isOpen()) {
            long currentFileTimestamp = file.lastModified();
            if (currentFileTimestamp > fileTimestamp) { // a newer version is on disk
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

        List<SmartObject> nestedSOs = new ArrayList<>();
        forEachNestedLayerOfType(SmartObject.class, nestedSOs::add);

        for (SmartObject so : nestedSOs) {
            // open contents are checked directly via the view
            if (!so.isContentOpen()) {
                reloadFuture = reloadFuture.thenCompose(comp -> so.checkAutoReload());
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
     * Converts the visible top-level layers into a new smart object.
     */
    public void convertVisibleLayersToSmartObject() {
        List<Layer> visibleLayers = layerList.stream()
            .filter(Layer::isVisible)
            .toList();
        if (visibleLayers.isEmpty()) {
            Messages.showNoVisibleLayersError(this);
            return;
        }

        // determine the target index based on the topmost
        // visible layer, matching LayerHolder.convertToGroup
        int lastVisibleIndex = layerList.lastIndexOf(visibleLayers.getLast());
        int targetIndex = lastVisibleIndex + 1 - visibleLayers.size();

        // create the new content composition
        Composition content = new Composition(canvas.copy(), mode, dpi);
        content.setName("visible");
        content.initDebugName();

        // create a copy of the current composition to become the new
        // main one (the current one will be used as undo backup)
        Composition newMainComp = copy(CopyOptions.fullStateBackup());

        // remove visible layers from newMainComp and transfer to content
        for (Layer layer : newMainComp.layerList.stream().filter(Layer::isVisible).toList()) {
            newMainComp.removeDirectChild(layer, true);
            layer.setComp(content);
            content.addLayerWithoutUI(layer);
        }

        SmartObject so = new SmartObject(newMainComp, content);
        newMainComp.adder()
            .skipUIAdd()
            .atIndex(targetIndex)
            .add(so);

        History.add(new CompositionReplacedEdit("Convert Visible to Smart Object",
            view, this, newMainComp, null, false));
        view.replaceComp(newMainComp);
    }

    /**
     * Creates a shallow copy (clone) of the given smart object and adds it.
     */
    public void shallowDuplicate(SmartObject so) {
        so.getHolder().addWithHistory(so.shallowDuplicate(), "Clone");
    }

    /**
     * Checks if all fonts of all text layers can be found on the current machine.
     */
    public void warnIfFontsMissing() {
        assert calledOnEDT();
        forEachNestedLayerOfType(TextLayer.class, TextLayer::warnIfFontMissing);
    }

    // called from assertions and unit tests
    @SuppressWarnings("SameReturnValue")
    public boolean checkInvariants() {
        if (layerList.isEmpty()) {
            throw new AssertionError("no layers in " + getName());
        }
        if (activeLayer == null) {
            throw new AssertionError("no active layer in " + getName());
        }
        if (activeLayer.getComp() != this) {
            throw new AssertionError(
                "bad comp in active layer '%s' (that comp='%s', this='%s')".formatted(
                    activeLayer.getName(), activeLayer.getComp().getDebugName(), getDebugName()));
        }

        if (!contains(activeLayer)) {
            throw new AssertionError("active layer '%s' not contained in '%s'"
                .formatted(activeLayer.getName(), getDebugName()));
        }
        for (Layer layer : layerList) {
            if (!layer.isDirectChildOf(this)) {
                throw new AssertionError(
                    "bad holder in layer '%s' (that holder='%s', this='%s')".formatted(
                        layer.getName(), layer.getHolder().getName(), getDebugName()));
            }
        }

        forEachNestedLayerAndMask(layer -> {
            assert layer.checkInvariants();
            if (layer.getComp() != this) {
                throw new AssertionError(
                    "bad comp in '%s' (that comp='%s', this='%s')".formatted(
                        layer.getName(), layer.getComp().getDebugName(), getDebugName()));
            }
        });

        // view consistency
        if (isOpen() && !view.isMock()) {
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
                    throw new AssertionError(
                        "bad owner reference for " + getDebugName());
                }
            }
        }

        // selection consistency
        if (selection != null && selection.getView() != view) {
            throw new AssertionError("bad view in selection");
        }
        if (draftSelection != null && draftSelection.getView() != view) {
            throw new AssertionError("bad view in draft selection");
        }

        if (dpi <= 0) {
            throw new AssertionError("DPI must be positive, was: " + dpi);
        }

        return true; // all checks passed
    }

    @Override
    public DebugNode createDebugNode(String name) {
        DebugNode node = new DebugNode(name, this);

        node.add(canvas.createDebugNode("canvas"));
        node.addInt("dpi", dpi);

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
