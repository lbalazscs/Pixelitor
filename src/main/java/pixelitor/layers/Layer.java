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

import org.jdesktop.swingx.painter.CheckerboardPainter;
import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.CopyType;
import pixelitor.Features;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.gui.utils.RestrictedLayerAction;
import pixelitor.gui.utils.TaskAction;
import pixelitor.history.*;
import pixelitor.io.ORAImageInfo;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.awt.AlphaComposite.DstIn;
import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.String.format;
import static pixelitor.layers.LayerMaskAddType.HIDE_ALL;
import static pixelitor.layers.LayerMaskAddType.HIDE_SELECTION;
import static pixelitor.layers.LayerMaskAddType.REVEAL_ALL;
import static pixelitor.layers.LayerMaskAddType.REVEAL_SELECTION;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * The base class for all layer classes.
 */
public abstract class Layer implements Serializable, Debuggable {
    @Serial
    private static final long serialVersionUID = 2L;

    // the composition to which this layer belongs
    protected Composition comp;

    protected String name;

    private boolean visible = true;
    protected float opacity = 1.0f;
    protected BlendingMode blendingMode = BlendingMode.NORMAL;

    protected LayerMask mask;
    private boolean maskEnabled = true;

    /**
     * Whether the mask is currently being edited.
     * Related to {@link MaskViewMode}.
     */
    private transient boolean maskEditing = false;

    // true if this layer modifies the image created by the layers
    // below it rather than painting its own content
    protected boolean isAdjustment = false;

    /**
     * The {@link LayerHolder} that contains this layer.
     */
    protected LayerHolder holder;

    // transient or static variables from here

    // a mask uses the UI of its owner.
    protected transient LayerUI ui;

    private transient List<LayerListener> listeners;

    // unit tests use a different LayerUI implementation
    // by assigning a different UI factory
    public static Function<Layer, LayerUI> uiFactory = LayerGUI::new;

    protected static final CheckerboardPainter thumbCheckerBoardPainter
        = ImageUtils.createCheckerboardPainter();

    // can be called on any thread
    Layer(Composition comp, String name) {
        assert comp != null;
        assert name != null;

        this.comp = comp;
        this.name = name;
        opacity = 1.0f;

        listeners = new ArrayList<>();

        holder = comp;
    }

    // can be called on any thread
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // defaults for transient fields
        maskEditing = false;
        ui = null;

        in.defaultReadObject();
        listeners = new ArrayList<>();

        if (mask != null) {
            // necessary for pre-4.2.4 pxc files, which store
            // the mask's owner at the Layer level
            mask.changeOwner(this);
        }

        // migrate old pxc files
        if (holder == null) {
            holder = comp;
        }
    }

    /**
     * Creates the UI for this layer if it doesn't exist.
     */
    public LayerUI createUI() {
        if (hasUI()) {
            return ui;
        }

        ui = uiFactory.apply(this);
        updateIconImage();
        if (hasMask()) {
            mask.ui = ui; // a mask shares its owner's UI
            mask.updateIconImage();
        }
        return ui;
    }

    public LayerUI getUI() {
        return ui;
    }

    public boolean hasUI() {
        return ui != null;
    }

    public void activateUI() {
        assert AppMode.isUnitTesting() || calledOnEDT();
        ui.setSelected(true);
    }

    // redraws the UI to reflect changes in layer activation
    public void updateUI() {
        if (ui != null) {
            ui.updateSelectionState();

            // the whole GUI must be repainted when switching to another layer
            LayerUI topLevelUI = getTopLevelLayer().getUI();
            if (topLevelUI != null) { // null when "Convert Visible to Group"
                topLevelUI.repaint();
            }
        }
    }

    /**
     * Creates a copy of this layer.
     */
    public final Layer copy(CopyType copyType, boolean copyMask, Composition newComp) {
        Layer copy = createTypeSpecificCopy(copyType, newComp);

        copyCommonPropertiesTo(copy);

        copy.comp = newComp;
        if (newComp != comp && isActive()) {
            newComp.setActiveLayerRef(copy);
        }

        if (copyMask) {
            copyMaskTo(copy, copyType, newComp);
        }

        return copy;
    }

    /**
     * Creates a copy of the subclass-specific content,
     * without handling the common layer properties or mask.
     */
    protected abstract Layer createTypeSpecificCopy(CopyType copyType, Composition newComp);

    /**
     * Copies common layer properties to the target layer.
     */
    protected void copyCommonPropertiesTo(Layer target) {
        target.setVisible(isVisible());
        target.setBlendingMode(getBlendingMode());
        target.setOpacity(getOpacity());
    }

    /**
     * Copies the mask of this layer to the target layer.
     */
    protected void copyMaskTo(Layer target, CopyType copyType, Composition newComp) {
        if (hasMask()) {
            LayerMask newMask = mask.duplicate(target, newComp);
            if (copyType == CopyType.UNDO) {
                // this could be running outside the EDT, and direct assignment is fine
                // as no UI interaction or history is needed for UNDO mask restoration
                target.mask = newMask;
            } else {
                target.addConfiguredMask(newMask);
            }
            target.maskEnabled = this.maskEnabled;
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean newVisibility) {
        setVisible(newVisibility, false, false);
    }

    public void setVisible(boolean newVisibility, boolean addToHistory, boolean update) {
        if (visible == newVisibility) {
            return;
        }

        visible = newVisibility;

        if (update) {
            holder.update();
        }

        if (hasUI()) {
            ui.setOpenEye(newVisibility);
        }

        if (addToHistory) {
            History.add(new LayerVisibilityChangeEdit(comp, this, newVisibility));
        }
    }

    /**
     * Returns the visibility as a string for the OpenRaster format.
     */
    public Object getVisibilityAsORAString() {
        return isVisible() ? "visible" : "hidden";
    }

    /**
     * Isolates this layer, making all other layers temporarily invisible.
     */
    public void isolate() {
        comp.isolateLayer(this, true);
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float newOpacity) {
        setOpacity(newOpacity, false, false);
    }

    public void setOpacity(float newOpacity, boolean addToHistory, boolean update) {
        assert newOpacity <= 1.0f : "newOpacity = " + newOpacity;
        assert newOpacity >= 0.0f : "newOpacity = " + newOpacity;

        if (opacity == newOpacity) {
            return;
        }
        float prevOpacity = opacity;
        opacity = newOpacity;

        if (hasUI()) {
            LayerBlendingModePanel.get().opacityChangedForLayer(newOpacity);
        }

        if (update) {
            holder.update();
        }

        if (addToHistory) {
            History.add(new LayerOpacityEdit(this, prevOpacity));
        }
    }

    public BlendingMode getBlendingMode() {
        return blendingMode;
    }

    public void setBlendingMode(BlendingMode newMode) {
        setBlendingMode(newMode, false, false);
    }

    public void setBlendingMode(BlendingMode newMode, boolean addToHistory, boolean update) {
        assert newMode != BlendingMode.PASS_THROUGH || isGroup();

        if (blendingMode == newMode) {
            return;
        }

        BlendingMode prevMode = blendingMode;
        blendingMode = newMode;

        if (hasUI()) {
            LayerBlendingModePanel.get().blendingModeChangedForLayer(newMode, this);
        }

        if (update) {
            holder.update();
        }

        if (addToHistory) {
            History.add(new LayerBlendingEdit(this, prevMode));
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String newName, boolean addToHistory) {
        String prevName = name;
        name = newName;

        // important because this might be called twice for a single rename
        if (name.equals(prevName)) {
            return;
        }

        // the ui could be null when setting the name of a smart object's content
        if (ui != null) {
            ui.updateName();
        }

        if (addToHistory) {
            History.add(new LayerRenameEdit(this, prevName, name));
        }
    }

    public Composition getComp() {
        return comp;
    }

    public void setHolder(LayerHolder holder) {
        assert holder != null;
        assert holder != this;

        this.holder = holder;
    }

    public LayerHolder getHolder() {
        return holder;
    }

    /**
     * Returns the holder where new layers should be added.
     */
    public LayerHolder getHolderForNewLayers() {
        // usually called on the currently active layer,
        // but if a new layer is added while a smart filter
        // is active, then it's called on the smart object
        assert isActive() || this instanceof SmartObject;

        return holder;
    }

    public void setComp(Composition comp) {
        assert comp != null;
        this.comp = comp;
        if (hasMask()) {
            mask.setComp(comp);
        }
    }

    /**
     * Checks if this layer is the active top-level layer in the composition.
     */
    public boolean isActiveTopLevel() {
        return comp.isActiveTopLevelLayer(this);
    }

    /**
     * Checks if this layer is the active layer in the composition.
     */
    public boolean isActive() {
        return comp.isActiveLayer(this);
    }

    /**
     * Returns the top-level layer containing this layer.
     */
    public Layer getTopLevelLayer() {
        Layer top = this;
        while (!top.isTopLevel()) {
            if (!(top.holder instanceof Layer)) {
                // non-top-level holders must be layers
                throw new IllegalStateException("this = " + getName() + ", top.holder = " + top.holder.getName());
            }

            top = (Layer) top.holder;
        }

        if (top instanceof SmartFilter) {
            // smart filters are never top-level
            throw new IllegalStateException();
        }

        return top;
    }

    /**
     * Checks if this layer is a direct child of the composition.
     */
    public boolean isTopLevel() {
        return holder == comp;
    }

    /**
     * Return the current layer or the owner if this is a mask.
     */
    public Layer getLayer() {
        return this;
    }

    /**
     * Activates this layer in the composition.
     */
    public void activate() {
        comp.setActiveLayer(this);
    }

    public boolean hasMask() {
        return mask != null;
    }

    public LayerMask getMask() {
        return mask;
    }

    public void addMask(boolean ctrlPressed) {
        LayerMaskAddType type;
        if (comp.hasSelection()) {
            type = ctrlPressed ? HIDE_SELECTION : REVEAL_SELECTION;
        } else {
            type = ctrlPressed ? HIDE_ALL : REVEAL_ALL;
        }
        addMask(type);
    }

    public void addMask(LayerMaskAddType addType) {
        if (hasMask()) {
            RestrictedLayerAction.LayerRestriction.NO_LAYER_MASK.showErrorMessage(this);
            return;
        }
        if (addType.needsSelection() && !comp.hasSelection()) {
            String msg = format("The composition \"%s\" has no selection to create a mask from.", comp.getName());
            Messages.showInfo("No Selection", msg, comp.getDialogParent());
            return;
        }

        Shape selShape = comp.getSelectionShape();
        BufferedImage bwMask = addType.createBWImage(this, selShape);
        assert bwMask.getWidth() == comp.getCanvasWidth();
        assert bwMask.getHeight() == comp.getCanvasHeight();

        String editName = addType.needsSelection()
            ? "Layer Mask from Selection"
            : "Add Layer Mask";
        addImageAsMask(bwMask, true, true,
            editName, addType.needsSelection());
    }

    /**
     * Adds the given grayscale image as a mask to this layer.
     */
    public PixelitorEdit addImageAsMask(BufferedImage bwMask,
                                        boolean createEdit, boolean addEdit,
                                        String editName, boolean deselect) {
        assert mask == null;

        // new masks are always canvas-sized (no translation),
        // even if their owner layer is bigger than the canvas
        mask = new LayerMask(comp, bwMask, this, 0, 0);
        maskEnabled = true;

        if (hasUI()) {
            // in rare cases (like selection crop), this could be
            // running on a comp which isn't added yet to the GUI
            ui.addMaskIcon();
        }

        if (!createEdit) {
            // history and UI update will be handled in an enclosing
            // operation (such as a non-rectangular selection crop)
            return null;
        }

        maskChanged();
        holder.update();

        Layers.maskAdded(this); // notify global mask listeners

        PixelitorEdit edit = new AddLayerMaskEdit(editName, comp, this);
        if (deselect) {
            assert comp.hasSelection();
            DeselectEdit deselectEdit = comp.deselect(false);
            if (deselectEdit != null) {
                edit = new MultiEdit(editName, comp, edit, deselectEdit);
            }
        }

        if (isActive()) {
            // switch to mask editing mode if the layer is active
            MaskViewMode.EDIT_MASK.activate(comp, this);
        }

        if (addEdit) {
            History.add(edit);
            return null; // edit was added, nothing to return
        } else {
            return edit; // return the edit for the caller to manage
        }
    }

    /**
     * Adds or replaces the mask image from a grayscale image.
     */
    public void addOrReplaceMaskImage(BufferedImage bwMask, String editName) {
        if (hasMask()) {
            mask.setTranslation(0, 0);
            mask.replaceImage(bwMask, editName);
        } else {
            addImageAsMask(bwMask, true, true,
                editName, false);
        }
    }

    /**
     * Adds a mask that is already configured to be used
     * with this layer.
     */
    public void addConfiguredMask(LayerMask mask) {
        assert mask != null;
        assert this.mask == null;
        assert mask.getOwner() == this;
        assert mask.getComp() == comp;

        this.mask = mask;
        if (hasUI() && !ui.hasMaskIcon()) {
            ui.addMaskIcon();
        }
        if (comp.isActive()) {
            maskChanged();
            holder.update();
            if (isActive()) {
                Layers.maskAdded(this);
            }
        } else {
            comp.invalidateImageCache();
        }
    }

    /**
     * Deletes the mask from this layer.
     */
    public void deleteMask(boolean addToHistory) {
        assert hasMask();

        LayerMask prevMask = mask;
        View view = comp.getView();
        MaskViewMode prevMode = view.getMaskViewMode();
        mask = null;

        ui.removeMaskIcon();
        Layers.maskDeleted(this); // notify global mask listeners

        // call this only after AddLayerMaskAction is notified,
        // because in some cases it might trigger a consistency check
        setMaskEditing(false);

        if (addToHistory) {
            History.add(new DeleteLayerMaskEdit(comp, this, prevMask, prevMode));
        }

        // DeleteLayerMaskEdit assumes that it's created with
        // an active layer (when the undo activates the MaskViewMode).
        // If this isn't always true, then MaskViewMode activation
        // must also be guarded in DeleteLayerMaskEdit.undo.
        assert isActive() || AppMode.isUnitTesting();

        if (isActive()) {
            // switch back to normal layer editing
            MaskViewMode.NORMAL.activate(view, this);
        }
        maskChanged();
        holder.update();
    }

    protected void maskChanged() {
        // empty by default
    }

    public boolean isMaskEditing() {
        //noinspection SimplifiableConditionalExpression
        assert maskEditing ? hasMask() : true;

        return maskEditing;
    }

    public void setMaskEditing(boolean newValue) {
        //noinspection SimplifiableConditionalExpression
        assert newValue ? hasMask() : true;

        if (maskEditing != newValue) {
            maskEditing = newValue;
            ui.updateSelectionState();
            Tools.editingTargetChanged(this);
        }
    }

    /**
     * Adds a mask corresponding to the given shape if there is no mask,
     * or modifies the existing one.
     * It doesn't add an edit to the history, only returns one, if requested.
     */
    public PixelitorEdit hideWithMask(Shape shape, boolean createEdit) {
        if (hasMask()) {
            return modifyMaskToHide(shape, createEdit);
        } else {
            // create a new mask that reveals only the shape area
            var maskImage = REVEAL_SELECTION.createBWImage(this, shape);
            return addImageAsMask(maskImage, createEdit, false,
                "Add Layer Mask", false);
        }
    }

    // helper to modify an existing mask to hide areas outside the shape
    private PixelitorEdit modifyMaskToHide(Shape shape, boolean createEdit) {
        BufferedImage maskImage = mask.getImage();
        BufferedImage maskImageBackup = null;
        if (createEdit) {
            maskImageBackup = ImageUtils.copyImage(maskImage);
        }
        Graphics2D g = maskImage.createGraphics();

        // Fill the unselected part with black to hide it.
        // The rest remains as it was.
        Shape unselectedPart = comp.getCanvas().invertShape(shape);
        g.setColor(Color.BLACK);
        g.fill(unselectedPart);
        g.dispose();

        mask.updateTransparencyImage();

        if (createEdit) {
            return new ImageEdit("Modify Mask",
                comp, mask, maskImageBackup, true);
        } else {
            return null;
        }
    }

    public boolean isMaskEnabled() {
        return maskEnabled;
    }

    public void setMaskEnabled(boolean maskEnabled, boolean addToHistory) {
        assert hasMask();
        this.maskEnabled = maskEnabled;

        maskChanged();
        holder.update();
        mask.updateIconImage(); // add or remove the red X
        notifyListeners();

        if (addToHistory) {
            History.add(new EnableLayerMaskEdit(comp, this));
        }
    }

    protected boolean usesMask() {
        return mask != null && maskEnabled;
    }

    /**
     * Transfers the mask and UI from this layer to a new owner layer.
     */
    public void transferMaskAndUITo(Layer newOwner) {
        if (hasMask()) {
            mask.changeOwner(newOwner);

            // new, updated listeners will be added when the UI is
            // transferred and the mask icon is recreated
            mask.removeAllListeners();

            newOwner.mask = mask;
            newOwner.maskEnabled = maskEnabled;
            newOwner.maskEditing = maskEditing;
            mask = null;
        }

        newOwner.ui = this.ui;
        if (newOwner.hasMask()) {
            newOwner.mask.ui = ui;
        }
        newOwner.ui.changeLayer(newOwner);
        this.ui = null;
    }

    /**
     * Renders this layer onto the given Graphics2D
     * or transforms the given image.
     * Adjustment layers and watermarked text layers change the
     * BufferedImage, while other layers just paint on the Graphics2D.
     * Returns the new image if transformation occurs, otherwise null.
     */
    public BufferedImage render(Graphics2D g,
                                BufferedImage currentComposite,
                                boolean firstVisibleLayer) {
        if (isAdjustment) { // adjustment layer or watermarked text layer
            return adjustImage(currentComposite, firstVisibleLayer);
        } else {
            setupComposite(g, firstVisibleLayer);
            if (usesMask()) {
                paintWithMask(g, firstVisibleLayer);
            } else {
                paint(g, firstVisibleLayer);
            }
        }
        return null;
    }

    /**
     * Paints the layer content on the given Graphics2D.
     * Called by non-adjustment layers.
     * The Graphics2D is assumed to be already configured
     * with the correct blending mode and opacity.
     */
    public abstract void paint(Graphics2D g, boolean firstVisibleLayer);

    // paints the layer content applying its mask
    private void paintWithMask(Graphics2D g, boolean firstVisibleLayer) {
        // 1. create the masked image
        // TODO the masked image should be cached
        var maskedImage = new BufferedImage(
            comp.getCanvasWidth(), comp.getCanvasHeight(), TYPE_INT_ARGB);
        Graphics2D mig = maskedImage.createGraphics();
        paint(mig, firstVisibleLayer);
        mig.setComposite(DstIn);
        mig.drawImage(mask.getTransparencyImage(),
            mask.getTx(), mask.getTy(), null);
        mig.dispose();

        // 2. paint the masked image onto the graphics
        g.drawImage(maskedImage, 0, 0, null);
    }

    /**
     * Applies the layer's effect to the given image.
     * Used by adjustment and watermarking text layers.
     */
    protected BufferedImage adjustImage(BufferedImage currentComposite,
                                        boolean firstVisibleLayer) {
        if (firstVisibleLayer) {
            return currentComposite; // there's nothing to transform
        }
        BufferedImage transformed = transformImage(currentComposite);
        boolean useMask = usesMask();
        if (useMask) {
            mask.applyTo(transformed);
        }
        if (!useMask && isNormalAndOpaque()) {
            return transformed;
        } else {
            Graphics2D g = currentComposite.createGraphics();
            setupComposite(g, firstVisibleLayer);
            g.drawImage(transformed, 0, 0, null);
            g.dispose();
            return currentComposite;
        }
    }

    /**
     * Used by adjustment layers and watermarking text layers
     * to apply this layer's effect on the given image.
     */
    protected abstract BufferedImage transformImage(BufferedImage src);

    /**
     * Resizes the layer content to new canvas dimensions.
     */
    public abstract CompletableFuture<Void> resize(Dimension newSize);

    /**
     * Crops the layer content to the given rectangle, which is
     * in image space, relative to the canvas.
     */
    public abstract void crop(Rectangle2D cropRect,
                              boolean deleteCropped,
                              boolean allowGrowing);

    /**
     * Changes the stacking order of this layer.
     */
    public void changeStackIndex(int newIndex) {
        comp.changeStackIndex(this, newIndex);
    }

    /**
     * Checks if the layer has normal blending mode and full opacity.
     */
    protected boolean isNormalAndOpaque() {
        return blendingMode == BlendingMode.NORMAL
            && opacity > BlendingModePanel.FULLY_OPAQUE_THRESHOLD;
    }

    /**
     * Configures the composite of the given Graphics,
     * according to the blending mode and opacity of the layer
     */
    public void setupComposite(Graphics2D g, boolean firstVisibleLayer) {
        if (firstVisibleLayer) {
            // the first visible layer is always painted with normal mode
            g.setComposite(AlphaComposite.getInstance(SRC_OVER, opacity));
        } else {
            g.setComposite(blendingMode.getComposite(opacity));
        }
    }

    /**
     * Prepares a movement with the Move Tool.
     * On this level prepareMovement(), moveWhileDragging and finalizeMovement
     * only care about the movement of the linked mask or owner.
     * This object's own movement is handled in {@link ContentLayer}.
     */
    public void prepareMovement() {
        Layer linked = getLinked();
        if (linked != null) {
            linked.prepareMovement();
        }
    }

    /**
     * Updates the layer position during a mouse-dragged movement.
     */
    public void moveWhileDragging(double imDx, double imDy) {
        Layer linked = getLinked();
        if (linked != null) {
            linked.moveWhileDragging(imDx, imDy);
        }
    }

    /**
     * Finalizes a movement with the Move Tool and returns the edit that can undo it.
     */
    public PixelitorEdit finalizeMovement() {
        // Handles the case of moving the layer mask of a layer without content.
        // Content layers should override it to also include their own edit.
        return createLinkedMovementEdit();
    }

    protected PixelitorEdit createLinkedMovementEdit() {
        Layer linked = getLinked();
        if (linked != null) {
            return linked.finalizeMovement();
        }
        return null;
    }

    /**
     * Returns the layer/mask that should move together with this
     * one when this layer is being moved.
     * Returns null if this layer should move independently.
     */
    protected Layer getLinked() {
        // Returns the mask if it is linked.
        // Overridden for masks to return their owner layer.
        if (hasMask() && !maskEditing && mask.isLinked()) {
            return mask;
        }
        return null;
    }

    /**
     * Checks if this layer can be converted to a Smart Object.
     */
    public boolean isConvertibleToSmartObject() {
        return true; // default, subclasses can override
    }

    /**
     * Checks if this layer can be exported to the OpenRaster format.
     */
    public boolean canExportORAImage() {
        return true;
    }

    /**
     * Returns information needed for OpenRaster export.
     */
    public ORAImageInfo getORAImageInfo() {
        // the default implementation is good for color and gradient fill layers
        return new ORAImageInfo(toImage(true, false), 0, 0);
    }

    /**
     * Returns a canvas-sized image corresponding to the contents of this layer.
     * Returns null if no such image can be returned (adjustment layer).
     * The layer's blending mode is always ignored.
     *
     * @param applyMask    if false, then the mask is ignored
     * @param applyOpacity if false, then the layer's opacity is ignored.
     */
    public BufferedImage toImage(boolean applyMask, boolean applyOpacity) {
        BufferedImage img = comp.getCanvas().createTmpImage();
        Graphics2D g = img.createGraphics();
        if (applyOpacity) {
            // the layer's blending mode will be ignored
            // because firstVisibleLayer is set to true
            setupComposite(g, true);
        }

        if (applyMask && usesMask()) {
            paintWithMask(g, true);
        } else {
            paint(g, true);
        }
        g.dispose();
        return img;
    }

    /**
     * Adds a listener for layer state changes.
     */
    public void addListener(LayerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes all layer state listeners from this layer.
     */
    public void removeAllListeners() {
        listeners.clear();
    }

    /**
     * Notifies all registered listeners of a state change.
     */
    protected void notifyListeners() {
        for (LayerListener listener : listeners) {
            listener.layerStateChanged(this);
        }
    }

    /**
     * Creates a popup menu for this layer's icon in the layers panel.
     */
    public JPopupMenu createLayerIconPopupMenu() {
        JPopupMenu popup = null;
        if (holder.canMergeDown(this)) {
            popup = new JPopupMenu();
            var mergeDownAction = new TaskAction(GUIText.MERGE_DOWN, () -> {
                // check again to be sure that the layer below
                // this didn't change in the meantime
                if (holder.canMergeDown(this)) {
                    holder.mergeDown(this);
                }
            });
            mergeDownAction.setToolTip(GUIText.MERGE_DOWN_TT);
            popup.add(mergeDownAction);
        }

        if (isRasterizable()) {
            if (popup == null) {
                popup = new JPopupMenu();
            }
            popup.add(new TaskAction("Rasterize", this::replaceWithRasterized));
        }

        if (Features.enableExperimental) {
            if (popup == null) {
                popup = new JPopupMenu();
            }

            addSmartObjectMenus(popup);
        }

        return popup;
    }

    protected void addSmartObjectMenus(JPopupMenu popup) {
        if (isConvertibleToSmartObject()) {
            popup.add(new TaskAction("Convert to Smart Object", this::replaceWithSmartObject));
        }
    }

    /**
     * Replaces this layer with a Smart Object containing this layer's content.
     */
    public void replaceWithSmartObject() {
        if (!isConvertibleToSmartObject()) {
            String msg = format("<html>The layer <b>%s</b> can't be converted to a smart object because it's %s.",
                getName(), Utils.addArticle(getTypeStringLC()));
            Messages.showInfo("Convert Layer to Smart Object", msg);
            return;
        }
        SmartObject so = new SmartObject(this);
        so.setHolder(holder);

        holder.replaceLayer(this, so);
        History.add(new ReplaceLayerEdit(this, so, "Convert to Smart Object"));

        Messages.showStatusMessage(format(
            "The layer <b>\"%s\"</b> was converted to a smart object.", getName()));
    }

    public void updateIconImage() {
        // otherwise this method must be overridden
        assert hasRasterIcon();

        if (ui != null) {
            ui.updateLayerIconImageAsync(this);
        }
        if (!isTopLevel()) {
            ((CompositeLayer) holder).updateIconImage();
        }
    }

    /**
     * Returns whether this layer type has a raster
     * (dynamically changing, based on the layer contents) icon image.
     */
    public boolean hasRasterIcon() {
        return true;
    }

    /**
     * Creates a thumbnail icon for this layer.
     * Returns non-null if {@link #hasRasterIcon()} returns true.
     */
    public BufferedImage createIconThumbnail() {
        return null; // implemented in subclasses, if needed
    }

    /**
     * Activates the UI for editing this layer.
     * Returns true if the user accepted the edit.
     */
    public boolean edit() {
        // by default does nothing, but overridden for non-image layers
        return true;
    }

    /**
     * Checks if this layer can be rasterized.
     */
    public boolean isRasterizable() {
        return true;
    }

    /**
     * Returns the name for the rasterized version of this layer.
     */
    protected String getRasterizedName() {
        return getName(); // default, overridden if the name changes when rasterizing
    }

    /**
     * Replaces this layer with a rasterized {@link ImageLayer} version.
     */
    public ImageLayer replaceWithRasterized() {
        assert isRasterizable();

        // render the layer to an image, ignoring its mask, blending mode and opacity,
        // because these properties will be preserved in the new layer
        var rasterizedImage = toImage(false, false);
        assert rasterizedImage != null;

        var newImageLayer = new ImageLayer(comp, rasterizedImage, getRasterizedName());
        newImageLayer.setHolder(holder);
        copyCommonPropertiesTo(newImageLayer);
        holder.replaceLayer(this, newImageLayer);
        History.add(new ReplaceLayerEdit(this, newImageLayer, "Rasterize " + getTypeString()));
        Messages.showStatusMessage(format(
            "The %s <b>\"%s\"</b> was rasterized.", getTypeStringLC(), getName()));

        return newImageLayer;
    }

    /**
     * Returns the preferred tool for editing this layer
     * or null if it has no preferred tool.
     */
    public Tool getPreferredTool() {
        return null; // no preference by default, overridden if needed
    }

    /**
     * Checks if this layer is a layer group.
     */
    public boolean isGroup() {
        return this instanceof LayerGroup;
    }

    /**
     * Ungroups this layer if it's part of a layer group.
     */
    public void unGroup() {
        if (holder instanceof LayerGroup group) {
            group.replaceWithUnGrouped(null, true);
        } else {
            Messages.showError("Can't Ungroup",
                "<html>The layer \"<b>%s</b>\" isn't inside a layer group.".formatted(getName()));
        }
    }

    /**
     * Checks if this layer (or its children) is or contains the given layer.
     */
    public boolean contains(Layer layer) {
        // by default it only contains itself, overridden for composite layers
        return layer == this;
    }

    /**
     * Checks if this layer (or its children) is of or contains the given type.
     */
    public boolean containsLayerOfType(Class<? extends Layer> type) {
        // by default check only itself, overridden for composite layers
        return type.isInstance(this);
    }

    /**
     * Performs an action on this layer and its children (if any),
     * optionally including all involved masks.
     */
    public void forEachNestedLayer(Consumer<Layer> action, boolean includeMasks) {
        action.accept(this);
        if (includeMasks && hasMask()) {
            action.accept(getMask());
        }
        // overridden by composite layers to iterate through children
    }

    public void update(boolean updateHistogram) {
        holder.update(updateHistogram);
    }

    public void update() {
        update(true);
    }

    public boolean checkInvariants() {
        if (comp == null) {
            throw new AssertionError("comp is null for " + getName());
        }
        if (holder == null) {
            throw new AssertionError("holder is null for " + getName());
        }
        if (name == null) {
            throw new AssertionError("name is null for layer of type " + getClass().getSimpleName());
        }
        if (blendingMode == null) {
            throw new AssertionError("blendingMode is null for " + getName());
        }

        if (ui != null) {
            // compare with the result of this.getLayer() so that this method works when called on masks
            if (ui.getLayer() != this.getLayer()) {
                throw new AssertionError("ui has bad layer reference for '%s', ui.getLayer() is '%s'"
                    .formatted(getName(), ui.getLayer() != null ? ui.getLayer().getName() : "null"));
            }
            ui.checkInvariants();
        }

        if (mask != null) {
            if (mask.getOwner() != this) {
                throw new AssertionError("mask owner mismatch for " + getName());
            }
            if (mask.getComp() != comp) {
                throw new AssertionError("mask comp mismatch for " + getName());
            }
            mask.checkInvariants();
        }

        return true;
    }

    public final DebugNode createDebugNode() {
        return createDebugNode(getTypeStringLC());
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key + " - " + getName(), this);

        node.addQuotedString("name", getName());
        node.addClass();
        node.addQuotedString("comp debug name", comp.getDebugName());
        node.addQuotedString("holder name", holder.getName());

        node.addBoolean("active top-level", isActiveTopLevel());
        node.addBoolean("active", isActive());

        if (hasMask()) {
            node.addString("has mask", "yes");
            node.addBoolean("mask enabled", isMaskEnabled());
            node.addBoolean("mask editing", isMaskEditing());
            node.add(getMask().createDebugNode());
        } else {
            node.addString("has mask", "no");
        }

        node.addBoolean("visible", isVisible());
        node.addFloat("opacity", getOpacity());
        node.addQuotedString("blending mode", getBlendingMode().toString());
        node.addNullableDebuggable("ui", ui);

        return node;
    }

    public final String getTypeStringLC() {
        return getTypeString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Returns a short string describing the type of this layer.
     */
    public abstract String getTypeString();

    @Override
    public String toString() {
        return "'" + name + "' (" + getClass().getSimpleName() + ")";
    }
}
