/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.util.PPoint;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.Debug;

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

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
import static java.lang.String.format;
import static pixelitor.Composition.LayerAdder.Position.*;
import static pixelitor.Composition.UpdateActions.FULL;
import static pixelitor.io.FileUtils.stripExtension;
import static pixelitor.utils.Threads.*;
import static pixelitor.utils.Utils.createCopyName;

/**
 * An image composition consisting of multiple layers
 */
public class Composition implements Serializable {
    // serialization is used for saving in the pxc format
    @Serial
    private static final long serialVersionUID = 1L;

    private static long debugCounter = 0;

    private String name;

    private final List<Layer> layerList = new ArrayList<>();
    private Layer activeLayer;

    // a counter for the names of new layers
    private int newLayerCount = 1;

    private final Canvas canvas;
    private Paths paths;
    private Guides guides;
    private ImageMode mode;

    //
    // transient variables from here
    //

    // Not null if this is the content of a smart object.
    // A composition can have multiple smart object owners after
    // "Shallow Duplicate", which belong to the same composition.
    // Transient because the parent compositions should not be written out.
    private transient List<SmartObject> owners;

    // useful for distinguishing between versions with the same name
    private transient String debugName;

    private transient File file;
    // the last modified time of the file in millis since the epoch
    private transient long fileTime;

    private transient boolean dirty = false;

    private transient BufferedImage compositeImage;

    private transient View view;

    private transient Selection selection;

    // a temporary, new selection which is currently built
    // by dragging with a tool, but not finalized yet
    private transient Selection builtSelection;

    /**
     * The constructor is private: a {@link Composition}
     * can be created either with one of the static factory
     * methods or through deserialization of pxc files
     */
    private Composition(Canvas canvas, ImageMode mode) {
        assert canvas != null;
        this.canvas = canvas;
        this.mode = mode;
    }

    /**
     * Creates a single-layered composition from the given image
     */
    public static Composition fromImage(BufferedImage img, File file, String name) {
        assert img != null;
        Canvas canvas = new Canvas(img.getWidth(), img.getHeight());

        ImageMode mode;
        if (AppContext.enableImageMode && img.getColorModel() instanceof IndexColorModel) {
            mode = ImageMode.Indexed;
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
            // one of the file and name arguments must be given
            throw new IllegalArgumentException("no name could be set");
        }
        comp.createDebugName();
        assert comp.getName() != null;
        return comp;
    }

    /**
     * Creates an empty composition (no layers, no name)
     */
    public static Composition createEmpty(int width, int height, ImageMode mode) {
        Canvas canvas = new Canvas(width, height);
        return new Composition(canvas, mode);
    }

    /**
     * Creates a composition with one transparent image layer.
     */
    public static Composition createTransparent(int width, int height) {
        BufferedImage img = ImageUtils.createSysCompatibleImage(width, height);
        return fromImage(img, null, "transparent");
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // init transient variables
        compositeImage = null; // will be set when needed
        file = null; // will be set later
        fileTime = 0;
        debugName = null; // will be set later
        dirty = false;
        view = null; // will be set later
        selection = null; // the selection is not saved
        builtSelection = null;

        in.defaultReadObject();

        for (Layer layer : layerList) {
            if (layer instanceof SmartObject so) {
                // things that need a full canvas and also
                // (re)load the contents of linked smart objects
                so.afterDeserialization();
            }
        }

        createDebugName();
        assert checkAllSOInvariants();
    }

    /**
     * Creates and returns a deep copy of this composition.
     */
    public Composition copy(boolean forUndo, boolean copySelection) {
        var compCopy = new Composition(canvas.copy(), mode);

        // copy layers
        for (Layer layer : layerList) {
            var layerCopy = layer.duplicate(true, true);
            layerCopy.setComp(compCopy);

            compCopy.layerList.add(layerCopy);
            if (layer == activeLayer) {
                compCopy.activeLayer = layerCopy;
            }
        }

        compCopy.newLayerCount = newLayerCount;
        compCopy.mode = mode;
        compCopy.owners = owners;

        if (copySelection && selection != null) {
            compCopy.setSelectionRef(new Selection(selection, forUndo));
        }
        if (paths != null) {
            compCopy.paths = paths.deepCopy(compCopy);
        }
        if (forUndo) {
            compCopy.dirty = dirty;
            compCopy.file = file;
            compCopy.fileTime = fileTime;
            compCopy.name = name;
            compCopy.view = view;
            // the new guides are set in the action that needed undo
        } else { // duplicate
            compCopy.dirty = false;
            compCopy.file = null;
            compCopy.fileTime = 0;
            compCopy.name = createCopyName(stripExtension(name));
            compCopy.view = null;
            if (guides != null) {
                compCopy.guides = guides.copyForNewComp(view);
            }
        }
        compCopy.createDebugName();

        assert compCopy.classInvariant();

        return compCopy;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
        if (view != null) {
            canvas.recalcCoSize(view, true);
        }

        if (selection != null) {
            if (view == null) {
                selection.die();
            } else {
                selection.setView(view);
            }
        }
        if (paths != null) {
            if (view != null) { // the view can be null when reloading
                paths.setView(view);
            }
        }
    }

    public Component getDialogParent() {
        assert view != null;
        if (view == null) {
            return null;
        }
        return view.getDialogParent();
    }

    public void closed() {
        setView(null);
    }

    public void deactivated() {
        if (isSmartObjectContent()) {
            for (SmartObject owner : owners) {
                owner.propagateChanges(this, false);
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
        } else {
            for (SmartObject owner : owners) {
                if (owner == newOwner) {
                    return;
                }
            }
        }
        owners.add(newOwner);
    }

    public boolean isSmartObjectContent() {
        // if a content file is open independently of its parent,
        // then this will  return false, even for pxc files!
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

    public PPoint getRandomPointInCanvas() {
        return canvas.getRandomPoint(view);
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isUnsaved() {
        if (dirty) {
            // if this is the contents of a smart object,
            // then the dirty flag matters only if it's linked,
            // otherwise the parent composition saves it.
            if (isSmartObjectContent()) {
                boolean savedByParent = false;
                for (SmartObject owner : owners) {
                    if (!owner.isContentLinked()) {
                        savedByParent = true;
                        break;
                    }
                }
                return !savedByParent;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    private void clearAllDirtyFlagsAfterSave() {
        setDirty(false);

        forAllNestedSmartObjects(so -> {
            if (so.getSavingComp() == this) {
                so.getContent().setDirty(false);
            }
        });
    }

    public boolean isOpen() {
        return view != null;
    }

    public void close() {
        if (isOpen()) {
            view.close();
        }
    }

    /**
     * Returns the first open view in the hierarchy of parents
     */
    public View getParentView() {
        if (isOpen()) {
            return view;
        }
        if (isSmartObjectContent()) {
            // Recursively search in the hierarchy of parents.
            // It checks only the first owner, because it assumes
            // that all owners are in the same composition.
            return owners.get(0).getParentView();
        }

        throw new IllegalStateException("no view for top-level comp " + getDebugName());
    }

    public String getName() {
        return name;
    }

    public String getFileNameWithExt(String ext) {
        if (file == null) {
            return name + "." + ext;
        } else {
            return FileUtils.replaceExt(file.getName(), ext);
        }
    }

    public String calcTitle() {
        return name + " (" + canvas.getSizeString() + ")";
    }

    public void rename(TabViewContainer owner) {
        String oldName = getName();
        String newName = JOptionPane.showInputDialog(owner,
            "New Name:", oldName);
        if (newName.equals(oldName)) {
            return;
        }
        setName(newName);
        History.add(new CompRenamedEdit(this, oldName, newName));
    }

    public void setName(String name) {
        this.name = name;
        if (view != null) {
            view.updateTitle();
            PixelitorWindow.get().updateTitle(this);
        }
    }

    public String getDebugName() {
        return debugName;
    }

    public void createDebugName() {
        assert name != null;
        this.debugName = name + " " + debugCounter++;
    }

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
        this.fileTime = file.lastModified();
        setName(file.getName());
    }

    public boolean hasSameFileAs(Composition other, boolean allowNull) {
        if (file == null) {
            if (allowNull) {
                return other.file == null;
            } else {
                return false;
            }
        } else {
            return file.equals(other.file);
        }
    }

    public boolean isEmpty() {
        return layerList.isEmpty();
    }

    private void addBaseLayer(BufferedImage baseLayerImage) {
        var newLayer = new ImageLayer(this,
            baseLayerImage, generateNewLayerName());

        addLayerInInitMode(newLayer);
    }

    /**
     * "Init mode" means that this is part of the composition construction,
     * the layer is not added as a result of a user interaction
     */
    public void addLayerInInitMode(Layer newLayer) {
        new LayerAdder(this).compInitMode().add(newLayer);
    }

    public ImageLayer addNewEmptyImageLayer(String name, boolean bellowActive) {
        var newLayer = ImageLayer.createEmpty(this, name);
        new LayerAdder(this)
            .withHistory("New Empty Layer")
            .atPosition(bellowActive ? BELLOW_ACTIVE : ABOVE_ACTIVE)
            .noRefresh()
            .add(newLayer);

        return newLayer;
    }

    public void addExternalImageAsNewLayer(BufferedImage image, String layerName, String editName) {
        var newLayer = ImageLayer.fromExternalImage(image, this, layerName);
        new LayerAdder(this)
            .withHistory(editName)
            .atPosition(ABOVE_ACTIVE)
            .add(newLayer);
    }

    public void addNewLayerFromComposite() {
        var newLayer = new ImageLayer(this,
            getCompositeImage(), "Composite");

        new LayerAdder(this)
            .withHistory("New Layer from Visible")
            .noRefresh()
            .atIndex(layerList.size())
            .add(newLayer);
    }

    public void duplicateActiveLayer() {
        Layer duplicate = activeLayer.duplicate(false, true);
        if (duplicate == null) {
            // there was an out of memory error
            return;
        }
        new LayerAdder(this)
            .withHistory("Duplicate Layer")
            .atPosition(ABOVE_ACTIVE)
            .add(duplicate);
        assert classInvariant();
    }

    public void flattenImage() {
        assert isActive();

        if (layerList.size() < 2) {
            return;
        }

        int numLayers = getNumLayers();
        BufferedImage bi = getCompositeImage();

        Layer flattened = new ImageLayer(this, bi, "flattened");
        new LayerAdder(this)
            .atIndex(numLayers) // add to the top
            .noRefresh()
            .add(flattened);

        for (int i = numLayers - 1; i >= 0; i--) { // delete the rest
            deleteLayer(layerList.get(i), false);
        }

        Layers.numLayersChanged(this, 1);
        History.add(new NotUndoableEdit("Flatten Image", this));
    }

    public void addAllLayersToGUI() {
        assert classInvariant();

        Layer activeLayerBefore = activeLayer;

        // this shouldn't change the active layer here,
        // but sets the last button to selected
        forEachLayer(this::addLayerToGUI);
        assert activeLayer == activeLayerBefore;

        // correct the selection
        LayerButton ui = (LayerButton) activeLayer.getUI();
        if (!ui.isSelected()) {
            ui.setSelected(true);
        }
    }

    private void addLayerToGUI(Layer layer) {
        int layerIndex = layerList.indexOf(layer);
        view.addLayerToGUI(layer, layerIndex);
    }

    public boolean canMergeDown(Layer layer) {
        int index = layerList.indexOf(layer);
        if (index > 0 && layer.isVisible()) {
            Layer bellow = layerList.get(index - 1);
            return bellow instanceof ImageLayer && bellow.isVisible();
        }
        return false;
    }

    public void mergeActiveLayerDown() {
        assert classInvariant();

        if (canMergeDown(activeLayer)) {
            mergeDown(activeLayer);
        }
    }

    // this method assumes that canMergeDown() previously returned true
    public void mergeDown(Layer layer) {
        int layerIndex = layerList.indexOf(layer);
        var bellowLayer = (ImageLayer) layerList.get(layerIndex - 1);

        var bellowImage = bellowLayer.getImage();
        var maskViewModeBefore = view.getMaskViewMode();
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

    public void deleteActiveLayer(boolean addToHistory) {
        deleteLayer(activeLayer, addToHistory);
    }

    public void deleteLayer(Layer layer, boolean addToHistory) {
        deleteLayer(layer, addToHistory, true);
    }

    public void deleteLayer(Layer layer, boolean addToHistory, boolean updateUI) {
        if (layerList.size() < 2) {
            throw new IllegalStateException("there are " + layerList.size() + " layers");
        }

        int layerIndex = layerList.indexOf(layer);

        if (addToHistory) {
            History.add(new DeleteLayerEdit(this, layer, layerIndex));
        }

        layerList.remove(layer);

        if (layer == activeLayer) {
            if (layerIndex > 0) {
                setActiveLayer(layerList.get(layerIndex - 1));
            } else {  // deleted the fist layer, set the new first layer as active
                setActiveLayer(layerList.get(0));
            }
        }

        if (updateUI) {
            LayerUI ui = layer.getUI();
            if (ui != null) { // can be null if part of layer rasterization
                view.removeLayerUI(ui);

                if (isActive()) {
                    Layers.numLayersChanged(this, layerList.size());
                }

                update();
            }
        }
    }

    /**
     * Replaces a layer with another, while keeping its position, mask, ui
     */
    public void replaceLayer(Layer before, Layer after) {
        boolean wasActive = before == activeLayer;

        before.transferMaskAndUITo(after);

        int layerIndex = layerList.indexOf(before);
        assert layerIndex != -1;
        layerList.set(layerIndex, after);

        if (wasActive) {
            activeLayer = after;
        }
        Tools.editingTargetChanged(activeLayer);
    }

    public void setActiveLayer(Layer layer) {
        setActiveLayer(layer, false, null);
    }

    public void setActiveLayer(Layer layer, boolean addToHistory, String editName) {
        if (activeLayer == layer) {
            return;
        }
        assert layerList.contains(layer)
            : format("new active layer '%s' (%s) not in the layer list of '%s'",
            layer.getName(), System.identityHashCode(layer), getDebugName());

        Layer oldLayer = activeLayer;
        activeLayer = layer;

        if (activeLayer.hasUI()) {
            activeLayer.activateUI();
            Layers.layerActivated(layer, false);
        }

        if (addToHistory) {
            History.add(new LayerSelectionChangeEdit(editName, this, oldLayer, layer));
        }

        if (view != null) {  // shouldn't run while loading the composition
            Tools.editingTargetChanged(activeLayer);
        }

        assert classInvariant();
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

    public int getNumExportableImages() {
        return (int) layerList.stream()
            .filter(Layer::canExportImage)
            .count();
    }

    public void isolateActive() {
        isolate(activeLayer, true);
    }

    public void isolate(Layer target, boolean addHistory) {
        if (addHistory) { // currently not undoing an "isolate"
            // check if we should undo the last isolate
            if (History.getEditToBeUndone() instanceof IsolateEdit isolateEdit) {
                if (isolateEdit.getLayer() == target) {
                    History.undo();
                    return;
                }
            }

            int numLayers = layerList.size();
            boolean[] backupVisibility = new boolean[numLayers];
            for (int i = 0; i < numLayers; i++) {
                backupVisibility[i] = layerList.get(i).isVisible();
            }
            History.add(new IsolateEdit(this, target, backupVisibility));

            Messages.showInStatusBar("Layer <b>" + target.getName() + "</b> was isolated.");
        }

        for (Layer layer : layerList) {
            layer.setVisible(layer == target);
        }
        update();
    }

    public void forEachLayer(Consumer<Layer> action) {
        layerList.forEach(action);
    }

    @SuppressWarnings("unchecked")
    public <T extends Layer> void forEachLayer(Class<T> clazz, Consumer<T> action) {
        for (Layer layer : layerList) {
            if (clazz.isAssignableFrom(layer.getClass())) {
                action.accept((T) layer);
            }
        }
    }

    /**
     * Recursively applies the given action to all nested smart objects.
     */
    public void forAllNestedSmartObjects(Consumer<SmartObject> action) {
        forEachLayer(SmartObject.class, so -> so.forAllNestedSmartObjects(action));
    }

    public Layer findLayerAtPoint(Point2D p) {
        // In mask editing mode never auto-select another layer
        if (getView().getMaskViewMode().showMask()) {
            return getActiveLayer();
        }

        Point pixelLoc = new Point((int) p.getX(), (int) p.getY());

        // iterate in reverse order (we need to search layers from top to bottom)
        ListIterator<Layer> li = layerList.listIterator(layerList.size());
        while (li.hasPrevious()) {
            Layer layer = li.previous();
            if (!(layer instanceof ContentLayer contentLayer)) {
                continue;
            }
            if (!layer.isVisible()) {
                continue;
            }
            if (layer.getOpacity() < 0.05f) {
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

    public void updateAllIconImages() {
        forEachLayer(Layer::updateIconImage);
    }

    public boolean activeAcceptsToolDrawing() {
        if (activeLayer.getClass() == ImageLayer.class) { // but not smart objects
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

    /**
     * Returns the active mask or image layer or null
     */
    public Drawable getActiveDrawable() {
        assert classInvariant();
        if (activeLayer.isMaskEditing()) {
            return activeLayer.getMask();
        }
        if (activeLayer instanceof Drawable) {
            return (Drawable) activeLayer;
        }
        return null;
    }

    public Filterable getActiveFilterable() {
        assert classInvariant();
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

    public boolean hasSmartObjects() {
        for (Layer layer : layerList) {
            if (layer.getClass() == SmartObject.class) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called when the contents of one of the smart objects
     * belonging to this composition have changed
     */
    public void smartObjectChanged(boolean linked) {
        invalidateCompositeCache();
        if (!linked) {
            setDirty(true);
        }

        // recursively invalidate the image caches in the smart
        // objects and compositions until the top composition
        if (isSmartObjectContent()) {
            for (SmartObject owner : owners) {
                owner.invalidateImageCache();

                Composition parent = owner.getComp();
                parent.smartObjectChanged(owner.isContentLinked());
            }
        }
    }

    public void startMovement(MoveMode mode, boolean duplicateLayer) {
        if (mode.movesLayer()) {
            if (duplicateLayer) {
                duplicateActiveLayer();
            }

            getActiveMaskOrLayer().startMovement();
        }
        if (mode.movesSelection()) {
            if (selection != null) {
                selection.startMovement();
            }
        }
    }

    public void moveActiveContent(MoveMode mode, double relImX, double relImY) {
        if (mode.movesLayer()) {
            Layer layer = getActiveMaskOrLayer();
            layer.moveWhileDragging(relImX, relImY);
        }
        if (mode.movesSelection()) {
            if (selection != null) {
                selection.moveWhileDragging(relImX, relImY);
            }
        }
        update();
    }

    public void endMovement(MoveMode mode) {
        PixelitorEdit layerEdit = null;
        if (mode.movesLayer()) {
            Layer layer = getActiveMaskOrLayer();
            // the layer edit will be null if an adjustment
            // layer without a mask was moved.
            layerEdit = layer.endMovement();
        }

        PixelitorEdit selectionEdit = null;
        if (mode.movesSelection()) {
            if (selection != null) {
                selectionEdit = selection.endMovement(false);
            }
        }

        var combinedEdit = MultiEdit.combine(
            layerEdit, selectionEdit, MoveMode.MOVE_BOTH.getEditName());
        if (combinedEdit != null) {
            History.add(combinedEdit);
            update();
        }
    }

    public void drawMovementContours(Graphics2D g, MoveMode mode) {
        if (mode.movesLayer()) {
            Layer layer = getActiveMaskOrLayer();
            if (layer instanceof ContentLayer contentLayer) {
                Rectangle imBounds = contentLayer.getContentBounds();
                if (imBounds != null) {
                    Shapes.drawVisibly(g, view.imageToComponentSpace(imBounds));
                }
            }
        }
    }

    public void moveActiveLayerUp() {
        assert classInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        changeLayerOrder(oldIndex, oldIndex + 1,
            true, LayerMoveAction.RAISE_LAYER);
    }

    public void moveActiveLayerDown() {
        assert classInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        changeLayerOrder(oldIndex, oldIndex - 1,
            true, LayerMoveAction.LOWER_LAYER);
    }

    public void moveActiveLayerToTop() {
        assert classInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        int newIndex = layerList.size() - 1;
        changeLayerOrder(oldIndex, newIndex,
            true, LayerMoveAction.LAYER_TO_TOP);
    }

    public void moveActiveLayerToBottom() {
        assert classInvariant();

        int oldIndex = layerList.indexOf(activeLayer);
        changeLayerOrder(oldIndex, 0,
            true, LayerMoveAction.LAYER_TO_BOTTOM);
    }

    public void changeLayerOrder(int oldIndex, int newIndex) {
        changeLayerOrder(oldIndex, newIndex, false, null);
    }

    // Called when the layer order is changed by an action.
    // The GUI has to be updated.
    public void changeLayerOrder(int oldIndex, int newIndex,
                                 boolean addToHistory, String editName) {
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

        view.changeLayerButtonOrder(oldIndex, newIndex);
        update();
        Layers.layerOrderChanged(this);

        if (addToHistory) {
            History.add(new LayerOrderChangeEdit(editName, this, oldIndex, newIndex));
        }
    }

    // Called when the layer order is changed by drag-reordering in the GUI.
    // The GUI doesn't have to be updated.
    public void changeLayerIndex(Layer layer, int newIndex) {
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

    public void raiseLayerSelection() {
        int oldIndex = layerList.indexOf(activeLayer);

        int newIndex = oldIndex + 1;
        if (newIndex >= layerList.size()) {
            return;
        }
        setActiveLayer(layerList.get(newIndex), true,
            LayerMoveAction.RAISE_LAYER_SELECTION);

        assert ConsistencyChecks.fadeWouldWorkOn(this);
    }

    public void lowerLayerSelection() {
        int oldIndex = layerList.indexOf(activeLayer);
        int newIndex = oldIndex - 1;
        if (newIndex < 0) {
            return;
        }

        setActiveLayer(layerList.get(newIndex), true,
            LayerMoveAction.LOWER_LAYER_SELECTION);

        assert ConsistencyChecks.fadeWouldWorkOn(this);
    }

    public void createLayerUIs() {
        for (Layer layer : layerList) {
            layer.createUI();
        }
    }

    private BufferedImage calculateCompositeImage() {
        if (layerList.size() == 1) { // shortcut
            Layer layer = layerList.get(0);
            if (Tools.currentTool.isDirectDrawing() && layer.isVisible()) {
                return layer.asImage(true, true);
            }
        }

        var imageSoFar = new BufferedImage(
            canvas.getWidth(), canvas.getHeight(), TYPE_INT_ARGB_PRE);
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

    public void repaint() {
        view.repaint();
    }

    public void repaintRegion(PPoint start, PPoint end, double thickness) {
        invalidateCompositeCache();
        if (view != null) { // during reload image it can be null
            view.repaintRegion(start, end, thickness);
            view.repaintNavigator(false);
        }
    }

    public void repaintRegion(PRectangle area) {
        invalidateCompositeCache();
        if (view != null) { // during reload image it can be null
            view.repaintRegion(area);
            view.repaintNavigator(false);
        }
    }

    public void dispose() {
        if (selection != null) {
            // stop the timer thread
            selection.die();
        }
    }

    public void paintSelection(Graphics2D g) {
        boolean ruby = false; // feature to be added one day
        if (ruby) {
            paintSelectionAsRubyOverlay(g);
        } else {
            paintSelectionAsMarchingAnts(g);
        }
    }

    private void paintSelectionAsMarchingAnts(Graphics2D g) {
        if (builtSelection != null) {
            builtSelection.paintMarchingAnts(g);
        }
        if (selection != null) {
            selection.paintMarchingAnts(g);
        }
    }

    private void paintSelectionAsRubyOverlay(Graphics2D g) {
        Shape totalShape = calcTotalSelectionShape();
        if (totalShape != null) {
            Shape inverted = canvas.invertShape(totalShape);
            g.setComposite(AlphaComposite.SrcOver.derive(0.5f));
            g.setColor(Color.RED);
            g.fill(inverted);
        }
    }

    private Shape calcTotalSelectionShape() {
        Shape totalShape = null;
        if (builtSelection != null) {
            totalShape = builtSelection.getShape();
        }
        if (selection != null) {
            if (totalShape == null) {
                totalShape = selection.getShape();
            } else {
                totalShape = ShapeCombinator.ADD.combine(
                    totalShape, selection.getShape());
            }
        }
        return totalShape;
    }

    public DeselectEdit deselect(boolean addToHistory) {
        if (builtSelection != null) {
            builtSelection.die();
            builtSelection = null;
        }

        DeselectEdit edit = null;
        if (selection != null) {
            Shape shape = selection.getShape();
            if (shape != null) { // null for a simple click without a previous selection
                edit = new DeselectEdit(this, shape);
            }
            if (addToHistory && edit != null) {
                History.add(edit);
            }

            boolean wasHidden = selection.isHidden();
            selection.die();
            setSelectionRef(null);

            if (isActive()) {
                if (wasHidden) {
                    SelectionActions.getShowHide().setHideText();
                }
            } else {
                // we can get here from a DeselectEdit.redo on a non-active composition
            }
        }
        return edit;
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
        builtSelection = selection;
    }

    /**
     * Creates a selection from the given shape and also handles
     * existing selections
     */
    public PixelitorEdit changeSelection(Shape newShape) {
        newShape = clipToCanvasBounds(newShape);
        if (newShape.getBounds().isEmpty()) {
            // the new selection would be outside the canvas
            return null;
        }

        PixelitorEdit edit;
        if (selection != null) {
            int answer = Dialogs.showYesNoCancelDialog(getDialogParent(), "Existing Selection",
                "<html>There is already a selection on " + getName() +
                ".<br>How do you want to combine new selection with the existing one?",
                new String[]{"Replace", "Add", "Subtract", "Intersect", GUIText.CANCEL},
                JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.CLOSED_OPTION || answer == 4) {
                // canceled
                return null;
            }
            Shape oldShape = selection.getShape();
            ShapeCombinator combinator = switch (answer) {
                case 0 -> ShapeCombinator.REPLACE;
                case 1 -> ShapeCombinator.ADD;
                case 2 -> ShapeCombinator.SUBTRACT;
                case 3 -> ShapeCombinator.INTERSECT;
                default -> throw new IllegalStateException("answer = " + answer);
            };
            selection.setShape(combinator.combine(oldShape, newShape));
            selection.setHidden(false, false);
            edit = new SelectionShapeChangeEdit("Selection Change", this, oldShape);
        } else { // no existing selection
            setSelectionRef(new Selection(newShape, view));
            edit = new NewSelectionEdit(this, selection.getShape());
        }
        return edit;
    }

    /**
     * A shortcut for creating a selection without history.
     * It assumes that there is no existing selection.
     */
    public void createSelectionFrom(Shape shape) {
        if (selection != null) {
            throw new IllegalStateException("There is already a selection: " + selection);
        }
        setSelectionRef(new Selection(shape, view));
    }

    /**
     * Changing the selection reference should be done only by using this method
     * (in order to make debugging easier)
     */
    @VisibleForTesting
    public void setSelectionRef(Selection selection) {
        this.selection = selection;
        if (isActive()) {
            SelectionActions.update(this);
            SelectionActions.getShowHide().updateTextFrom(selection);
        }
    }

    /**
     * Promote a "built selection" into a final one.
     */
    public void promoteSelection() {
        assert selection == null || !selection.isAlive() : "selection = " + selection;
        assert builtSelection != null;
        setSelectionRef(builtSelection);
        setBuiltSelection(null);
    }

    public boolean hasSelection() {
        return selection != null;
    }

    @VisibleForTesting
    public boolean hasBuiltSelection() {
        return builtSelection != null;
    }

    /**
     * Sets the clip area on the given graphics according to the selection.
     * It is assumed that the graphics is relative to the canvas:
     * if it is coming from the image of an ImageLayer, then it must be
     * translated before calling this.
     */
    public void applySelectionClipping(Graphics2D g2) {
        if (selection != null) {
            g2.setClip(selection.getShape());
        }
    }

    public void invertSelection() {
        if (selection != null) {
            Shape backupShape = selection.getShape();
            Shape inverted = canvas.invertShape(backupShape);
            if (inverted.getBounds2D().isEmpty()) {
                // everything was selected, and now nothing is
                deselect(true);
            } else {
                selection.setShape(inverted);
                History.add(new SelectionShapeChangeEdit(
                    "Invert Selection", this, backupShape));
            }
        }
    }

    /**
     * Intersects the selection with the given (crop) rectangle.
     * The selection is not translated here into the coordinate
     * system of the new, cropped image.
     */
    public void intersectSelection(Rectangle2D cropRect) {
        if (selection != null) {
            Shape currentShape = selection.getShape();
            Shape intersection = ShapeCombinator.INTERSECT.combine(currentShape, cropRect);
            if (intersection.getBounds().isEmpty()) {
                selection.die();
                setSelectionRef(null);
            } else {
                selection.setShape(intersection);
            }
        }
    }

    public void createSelectionFromText() {
        if (activeLayer instanceof TextLayer textLayer) {
            textLayer.createSelectionFromText();
        } else {
            throw new IllegalStateException("active layer is not text layer");
        }
    }

    /**
     * Called when the image-space coordinates have been changed by the
     * given transform (resize, crop, etc.)
     */
    public void imCoordsChanged(AffineTransform at, boolean isUndoRedo) {
        // The selection is explicitly reset to a backup shape
        // when something is undone/redone
        if (selection != null && !isUndoRedo) {
            selection.transform(at);
        }
        // The paths and the tool widgets are transformed even for undo redo.
        // The advantage is simpler code, the disadvantage is that
        // rounding errors could accumulate if the same operation is
        // undone/redone many times
        if (paths != null) {
            paths.imCoordsChanged(at);
        }
        Tools.imCoordsChanged(this, at);
    }

    /**
     * Called when the component-space coordinates have changed,
     * but the pixels remain the same  (zooming, view resizing etc.)
     */
    public void coCoordsChanged() {
        if (guides != null) {
            guides.coCoordsChanged(view);
        }
    }

    /**
     * Returns the (canvas-sized) composite image.
     */
    public BufferedImage getCompositeImage() {
        if (compositeImage == null) {
            compositeImage = calculateCompositeImage();
        }
        return compositeImage;
    }

    /**
     * Forces the recalculation of the composite image
     * the next time when getCompositeImage() is called.
     */
    public void invalidateCompositeCache() {
//        Debug.debugCall(getName() + " cache invalidated", 1);
        if (compositeImage != null) {
            compositeImage.flush();
        }
        compositeImage = null;
    }

    public void update() {
        update(FULL);
    }

    public void update(UpdateActions actions) {
        update(actions, false);
    }

    /**
     * Signals that the contents of this composition have been changed:
     * the cache is invalidated, and additional actions might be necessary
     */
    public void update(UpdateActions actions, boolean sizeChanged) {
        invalidateCompositeCache();

        if (actions.repaintNeeded()) {
            if (view != null) {
                view.repaint();
                view.repaintNavigator(sizeChanged);
            }
        }

        if (actions.histogramChanged()) {
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
    @VisibleForTesting
    public void allImageLayersToCanvasSize() {
        for (Layer layer : layerList) {
            if (layer instanceof ImageLayer imageLayer) {
                imageLayer.toCanvasSizeWithHistory();
            }
        }
    }

    public void activeLayerToCanvasSize() {
        if (!(activeLayer instanceof ImageLayer)) {
            Messages.showNotImageLayerError(activeLayer);
            return;
        }

        ((ImageLayer) activeLayer).toCanvasSizeWithHistory();
    }

    public void fitCanvasToLayers() {
        EnlargeCanvas enlargeCanvas = new EnlargeCanvas(0, 0, 0, 0);

        for (Layer layer : layerList) {
            if (layer instanceof ContentLayer contentLayer) {
                enlargeCanvas.setupToFitContentOf(contentLayer);
            }
        }

        if (enlargeCanvas.doesNothing()) {
            Dialogs.showInfoDialog(getDialogParent(), "Nothing to be done",
                "The canvas is already large enough to show all layer content.");
            return;
        }

        enlargeCanvas.process(this);
    }

    // called from assertions and unit tests
    @SuppressWarnings("SameReturnValue")
    public boolean classInvariant() {
        if (layerList.isEmpty()) {
            if (AppContext.isUnitTesting()) {
                return true;
            }
            throw new IllegalStateException("no layer in " + getName());
        }
        if (activeLayer == null) {
            throw new IllegalStateException("no active layer in " + getName());
        }
        if (!layerList.contains(activeLayer)) {
            throw new IllegalStateException(format(
                "active layer (%s) not in list (%s)",
                activeLayer.getName(), layerList));
        }
        return true;
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

    public CompletableFuture<Void> saveAsync(SaveSettings saveSettings,
                                             boolean addToRecentMenus) {
        assert calledOnEDT() : threadInfo();

        FileFormat format = saveSettings.getFormat();
        Runnable saveTask = format.createSaveTask(this, saveSettings);
        FileFormat.setLastOutput(format);

        // prevents starting a new save on the EDT while an asynchronous
        // save is already scheduled or running on the IO thread
        File savedFile = saveSettings.getFile();
        String path = savedFile.getAbsolutePath();
        if (IOTasks.isProcessing(path)) {
            return CompletableFuture.completedFuture(null);
        }
        IOTasks.markWriteProcessing(path);

        // set to not dirty already at the beginning of the saving process,
        // so that subsequent closing does not trigger another, parallel save
        boolean wasDirty = isDirty();
        clearAllDirtyFlagsAfterSave();

        return CompletableFuture
            .runAsync(saveTask, onIOThread)
            .handleAsync((v, e) -> {
                if (e != null) {
                    Messages.showException(e);
                    setDirty(wasDirty);
                } else {
                    afterSuccessfulSaveActions(savedFile, addToRecentMenus);
                }
                IOTasks.writingFinishedFor(path);
                return null;
            }, onEDT);
    }

    public void afterSuccessfulSaveActions(File file, boolean addToRecentMenus) {
        assert calledOnEDT() : threadInfo();

        setFile(file);
        if (addToRecentMenus) {
            RecentFilesMenu.INSTANCE.addFile(file);
        }
        ImagePreviewPanel.removeThumbFromCache(file);
        Messages.showFileSavedMessage(file);

        if (isSmartObjectContent()) {
            // otherwise the changes might not be propagated when deactivating,
            // because this is not dirty after saving even if it's changed
            for (SmartObject owner : owners) {
                owner.propagateChanges(this, true);

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
        if (paths != null) {
            return paths.getActivePath();
        }
        return null;
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

    public CompletableFuture<Composition> checkForAutoReload() {
        // check only the open compositions here
        if (file != null && isOpen()) {
            long newFileTime = file.lastModified();
            if (newFileTime > fileTime) { // a newer version is on the disk
                fileTime = newFileTime;
                Views.activate(view);
                boolean reload = Messages.reloadFileQuestion(file);
                if (reload) {
                    return view.reloadAsync();
                }
            }
        }
        // also check in the smart objects
        CompletableFuture<Composition> cf = CompletableFuture.completedFuture(null);
        for (Layer layer : layerList) {
            if (layer instanceof SmartObject so) {
                // open contents are checked directly via the view
                if (!so.isContentOpen()) {
                    cf = cf.thenCompose(comp -> so.checkForAutoReload());
                }
            }
        }
        return cf;
    }

    public void closeAllNestedComps() {
        forAllNestedSmartObjects(so -> so.getContent().close());
    }

    private boolean checkAllSOInvariants() {
        forAllNestedSmartObjects(SmartObject::checkInvariant);
        return true;
    }

    // replaces the entire composition with a smart object
    // that has this composition as contents
    public void replaceWithSmartObject() {
        Composition newComp = new Composition(canvas.copy(), mode);

        if (file != null) {
            newComp.setFile(file);
            setFile(null);
        } else {
            newComp.setName(name);
        }

        SmartObject so = new SmartObject(newComp, this);
        newComp.addLayerInInitMode(so);
        History.add(new CompositionReplacedEdit("Convert All to Smart Object",
            view, this, newComp, null, false));
        view.replaceComp(newComp);
        setName("Contents of " + name);
    }

    public void convertVisibleLayersToSmartObject() {
        long visibleCount = layerList.stream().filter(Layer::isVisible).count();
        if (visibleCount == 0) {
            Messages.showInfo("No visible layers",
                "There are no visible layers in " + getName());
            return;
        }
        if (visibleCount == layerList.size()) {
            replaceWithSmartObject();
            return;
        }

        Composition content = new Composition(canvas.copy(), mode);
        content.setName("visible");
        Composition newMainComp = copy(true, true);

        List<Layer> visibleLayers = newMainComp.layerList.stream()
            .filter(Layer::isVisible)
            .toList();

        for (Layer layer : visibleLayers) {
            newMainComp.deleteLayer(layer, false, false);
            content.addLayerInInitMode(layer);
            layer.setComp(content);
        }

        SmartObject so = new SmartObject(newMainComp, content);
        newMainComp.addLayerInInitMode(so);
        History.add(new CompositionReplacedEdit("Convert Visible to Smart Object",
            view, this, newMainComp, null, false));
        view.replaceComp(newMainComp);
    }

    public void debugImages() {
        if (compositeImage != null) {
            Debug.debugImage(compositeImage, "cached composite for " + getDebugName());
        }
        BufferedImage calc = calculateCompositeImage();
        Debug.debugImage(calc, "calculated composite for " + getDebugName());
    }

    public void shallowDuplicate(SmartObject so) {
        if (so != activeLayer) {
            setActiveLayer(so);
        }

        SmartObject duplicate = so.shallowDuplicate();
        new Composition.LayerAdder(this)
            .withHistory("Clone")
            .atPosition(ABOVE_ACTIVE)
            .add(duplicate);
    }

    public enum UpdateActions {
        REPAINT(true, false) {
        }, HISTOGRAM(false, true) {
        }, FULL(true, true) {
        };

        private final boolean repaint;
        private final boolean updateHistogram;

        UpdateActions(boolean repaint, boolean updateHistogram) {
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

    public String toPathDebugString() {
        return "Composition{name='" + name + '\''
               + ", active = " + isActive()
               + ", path = " + getActivePath()
               + '}';
    }

    @Override
    public String toString() {
        return "Composition{name='" + name + '\''
               + ", active = " + isActive()
               + ", path = " + getActivePath()
               + ", activeLayer=" + (activeLayer == null ? "null" : activeLayer.getName())
               + ", layerList=" + layerList
               + ", canvas=" + canvas
               + ", selection=" + selection
               + ", dirty=" + dirty
               + '}';
    }

    public static class LayerAdder {
        private final Composition comp;
        private String editName; // null if the add should not be added to history
        private int newLayerIndex = -1;
        private boolean refresh = true;

        public enum Position {TOP, ABOVE_ACTIVE, BELLOW_ACTIVE}

        private Position position = TOP;
        private boolean compInit = false;

        public LayerAdder(Composition comp) {
            this.comp = comp;
        }

        public LayerAdder withHistory(String editName) {
            this.editName = editName;
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

        /**
         * The final operation which actually adds the layer.
         */
        public void add(Layer newLayer) {
            Layer activeLayerBefore = null;
            MaskViewMode oldViewMode = null;
            if (needsHistory()) {
                activeLayerBefore = comp.activeLayer;
                oldViewMode = comp.view.getMaskViewMode();
            }

            if (newLayerIndex == -1) { // no index was explicitly set
                if (position == TOP) {
                    newLayerIndex = comp.layerList.size();
                } else {
                    calcIndexBasedOnRelPosition();
                }
            }
            comp.layerList.add(newLayerIndex, newLayer);

            if (!compInit) {
                comp.view.addLayerToGUI(newLayer, newLayerIndex);

                // mocked views will not set a UI
                assert AppContext.isUnitTesting() || newLayer.hasUI();
            }

            comp.setActiveLayer(newLayer);
            if (!compInit) {
                comp.setDirty(true);

                if (refresh) {
                    comp.update();
                }
            }
            if (needsHistory()) {
                History.add(new NewLayerEdit(editName,
                    comp, newLayer, activeLayerBefore, oldViewMode));
            }
            assert comp.classInvariant();
        }

        private boolean needsHistory() {
            return editName != null;
        }
    }
}
