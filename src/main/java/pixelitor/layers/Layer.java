/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.Layers;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.gui.utils.PAction;
import pixelitor.gui.utils.RestrictedLayerAction;
import pixelitor.history.*;
import pixelitor.tools.Tools;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

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
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.awt.AlphaComposite.DstIn;
import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.String.format;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * The abstract superclass of all layer classes
 */
public abstract class Layer implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    // the composition to which this layer belongs
    protected Composition comp;

    protected String name;

    private boolean visible = true;
    private float opacity = 1.0f;
    private BlendingMode blendingMode = BlendingMode.NORMAL;

    protected LayerMask mask;
    private boolean maskEnabled = true;

    /**
     * Whether the edited image is the layer image or
     * the layer mask image.
     * Related to {@link MaskViewMode}.
     */
    private transient boolean maskEditing = false;

    protected boolean isAdjustment = false;

    // transient or static variables from here

    // A mask uses the UI of its owner.
    protected transient LayerUI ui;

    private transient List<LayerListener> listeners;

    // unit tests use a different LayerUI implementation
    // by assigning a different UI factory
    public static Function<Layer, LayerUI> uiFactory = LayerButton::new;

    // can be called on any thread
    Layer(Composition comp, String name) {
        assert comp != null;
        assert name != null;

        setComp(comp);
        this.name = name;
        opacity = 1.0f;

        listeners = new ArrayList<>();
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
    }

    public LayerUI createUI() {
        if (hasUI()) {
            return ui;
        }

        ui = uiFactory.apply(this);
        if (hasMask()) {
            mask.ui = ui;
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
        assert AppContext.isUnitTesting() || calledOnEDT();
        ui.setSelected(true);
    }

    public final Layer duplicate(boolean compCopy) {
        String duplicateName = compCopy ? this.name : Utils.createCopyName(name);
        Layer d = createTypeSpecificDuplicate(duplicateName);

        d.setOpacity(getOpacity(), false);
        d.setBlendingMode(getBlendingMode(), false);
        d.setVisible(isVisible(), false);

        duplicateMask(d, compCopy);

        return d;
    }

    protected abstract Layer createTypeSpecificDuplicate(String duplicateName);

    // Duplicates the mask of a duplicated layer.
    private void duplicateMask(Layer duplicate, boolean compCopy) {
        if (hasMask()) {
            LayerMask newMask = mask.duplicate(duplicate);
            if (compCopy) {
                // this could be running outside the EDT, and anyway it is
                // not necessary to add the duplicate to the GUI
                duplicate.mask = newMask;
            } else {
                duplicate.addConfiguredMask(newMask);
            }
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean newVisibility, boolean addToHistory) {
        if (visible == newVisibility) {
            return;
        }

        visible = newVisibility;
        comp.update();

        if (hasUI()) {
            ui.setOpenEye(newVisibility);
        }

        if (addToHistory) {
            History.add(new LayerVisibilityChangeEdit(comp, this, newVisibility));
        }
    }

    public Object getVisibilityAsORAString() {
        return isVisible() ? "visible" : "hidden";
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float newOpacity, boolean addToHistory) {
        assert newOpacity <= 1.0f : "newOpacity = " + newOpacity;
        assert newOpacity >= 0.0f : "newOpacity = " + newOpacity;

        if (opacity == newOpacity) {
            return;
        }
        float prevOpacity = opacity;
        opacity = newOpacity;

        if (hasUI()) {
            LayerBlendingModePanel.get().setOpacityFromModel(newOpacity);
            comp.update();
        }
        if (addToHistory) {
            History.add(new LayerOpacityEdit(this, prevOpacity));
        }
    }

    public BlendingMode getBlendingMode() {
        return blendingMode;
    }

    public void setBlendingMode(BlendingMode newMode, boolean addToHistory) {
        if (blendingMode == newMode) {
            return;
        }

        BlendingMode prevMode = blendingMode;
        blendingMode = newMode;

        if (hasUI()) {
            LayerBlendingModePanel.get().setBlendingModeFromModel(newMode);
            comp.update();
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

        ui.setLayerName(newName);

        if (addToHistory) {
            History.add(new LayerRenameEdit(this, prevName, name));
        }
    }

    public Composition getComp() {
        return comp;
    }

    public void setComp(Composition comp) {
        this.comp = comp;
        if (hasMask()) {
            mask.setComp(comp);
        }
    }

    public boolean isActive() {
        return comp.isActive(this);
    }

    public void activate(boolean addToHistory) {
        comp.setActiveLayer(this, addToHistory, null);
    }

    public boolean hasMask() {
        return mask != null;
    }

    public LayerMask getMask() {
        return mask;
    }

    public void addMask(LayerMaskAddType addType) {
        if (hasMask()) {
            RestrictedLayerAction.Condition.NO_LAYER_MASK.showErrorMessage(this);
            return;
        }
        if (addType.needsSelection() && !comp.hasSelection()) {
            String msg = format("The composition \"%s\" has no selection.", comp.getName());
            Messages.showInfo("No selection", msg, comp.getView());
            return;
        }

        Shape selShape = comp.getSelectionShape();
        BufferedImage bwMask = addType.createBWImage(this, selShape);
        assert bwMask.getWidth() == comp.getCanvasWidth();
        assert bwMask.getHeight() == comp.getCanvasHeight();

        String editName = "Add Layer Mask";
        boolean deselect = addType.needsSelection();
        if (deselect) {
            assert comp.hasSelection();
            editName = "Layer Mask from Selection";
        }

        addImageAsMask(bwMask, false, true, true,
            editName, deselect);
    }

    public PixelitorEdit addImageAsMask(BufferedImage bwMask,
                                        boolean inheritTranslation,
                                        boolean createEdit, boolean addEdit,
                                        String editName, boolean deselect) {
        assert mask == null;

        int maskTx = 0;
        int maskTy = 0;
        if (inheritTranslation && this instanceof ContentLayer contentLayer) {
            maskTx = contentLayer.getTx();
            maskTy = contentLayer.getTy();
        }

        mask = new LayerMask(comp, bwMask, this, maskTx, maskTy);
        maskEnabled = true;

        if (hasUI()) {
            // in rare cases (like selection crop) this could be running
            // on a comp which is not added yet to the GUI
            ui.addMaskIcon();
        }

        if (!createEdit) {
            // history and UI update will be handled in an
            // enclosing nonrectangular selection crop
            return null;
        }

        comp.update();

        Layers.maskAddedTo(this);

        PixelitorEdit edit = new AddLayerMaskEdit(editName, comp, this);
        if (deselect) {
            assert comp.hasSelection();
            DeselectEdit deselectEdit = comp.deselect(false);
            if (deselectEdit != null) {
                edit = new MultiEdit(editName, comp, edit, deselectEdit);
            }
        }

        if (isActive()) {
            MaskViewMode.EDIT_MASK.activate(comp, this);
        }

        if (addEdit) {
            History.add(edit);
            return null;
        } else {
            return edit;
        }
    }

    public void addOrReplaceMaskImage(BufferedImage bwMask, String editName) {
        if (hasMask()) {
            mask.replaceImage(bwMask, editName);
        } else {
            addImageAsMask(bwMask, true, true, true,
                editName, false);
        }
    }

    /**
     * Adds a mask that is already configured to be used
     * with this layer
     */
    public void addConfiguredMask(LayerMask mask) {
        assert mask != null;
        assert mask.getOwner() == this;

        this.mask = mask;
        comp.update();
        if (hasUI() && !ui.hasMaskIcon()) {
            ui.addMaskIcon();
        }
        Layers.maskAddedTo(this);
    }

    public void deleteMask(boolean addToHistory) {
        LayerMask oldMask = mask;
        View view = comp.getView();
        MaskViewMode oldMode = view.getMaskViewMode();
        mask = null;
        setMaskEditing(false);

        ui.removeMaskIcon();
        Layers.maskDeletedFrom(this);

        if (addToHistory) {
            History.add(new DeleteLayerMaskEdit(comp, this, oldMask, oldMode));
        }
        if (isActive()) {
            MaskViewMode.NORMAL.activate(view, this);
        }
        comp.update();
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
            Tools.editedObjectChanged(this);
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
            var maskImage = LayerMaskAddType.REVEAL_SELECTION.createBWImage(this, shape);
            return addImageAsMask(maskImage, false, createEdit, false,
                "Add Layer Mask", false);
        }
    }

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

        mask.updateFromBWImage();

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

        comp.update();
        mask.updateIconImage();
        notifyListeners();

        if (addToHistory) {
            History.add(new EnableLayerMaskEdit(comp, this));
        }
    }

    private boolean useMask() {
        return mask != null && maskEnabled;
    }

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
     * Applies the effect of this layer on the given Graphics2D
     * or on the given BufferedImage.
     * Adjustment layers and watermarked text layers change the
     * BufferedImage, while other layers just paint on the Graphics2D.
     * If the BufferedImage is changed, this method returns the new image
     * and null otherwise.
     */
    public BufferedImage applyLayer(Graphics2D g,
                                    BufferedImage imageSoFar,
                                    boolean firstVisibleLayer) {
        if (isAdjustment) { // adjustment layer or watermarked text layer
            return adjustImageWithMasksAndBlending(imageSoFar, firstVisibleLayer);
        } else {
            if (!useMask()) {
                setupDrawingComposite(g, firstVisibleLayer);
                paintLayerOnGraphics(g, firstVisibleLayer);
            } else {
                paintLayerOnGraphicsWithMask(g, firstVisibleLayer);
            }
        }
        return null;
    }

    // used by the non-adjustment stuff
    // This method assumes that the composite of the graphics is already
    // set up according to the transparency and blending mode
    public abstract void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer);

    /**
     * Returns the masked image for the non-adjustment case.
     * The returned image is canvas-sized, and the masks and the
     * translations are taken into account
     */
    private void paintLayerOnGraphicsWithMask(Graphics2D g, boolean firstVisibleLayer) {
        // 1. create the masked image
        // TODO the masked image should be cached
        var maskedImage = new BufferedImage(
            comp.getCanvasWidth(), comp.getCanvasHeight(), TYPE_INT_ARGB);
        Graphics2D mig = maskedImage.createGraphics();
        paintLayerOnGraphics(mig, firstVisibleLayer);
        mig.setComposite(DstIn);
        mig.drawImage(mask.getTransparencyImage(),
            mask.getTx(), mask.getTy(), null);
        mig.dispose();

        // 2. paint the masked image onto the graphics
        setupDrawingComposite(g, firstVisibleLayer);
        g.drawImage(maskedImage, 0, 0, null);
    }

    /**
     * Used by adjustment layers and watermarked text layers
     */
    private BufferedImage adjustImageWithMasksAndBlending(BufferedImage imgSoFar,
                                                          boolean isFirstVisibleLayer) {
        if (isFirstVisibleLayer) {
            return imgSoFar; // there's nothing we can do
        }
        BufferedImage transformed = applyOnImage(imgSoFar);
        if (useMask()) {
            mask.applyToImage(transformed);
        }
        if (!useMask() && isNormalAndOpaque()) {
            return transformed;
        } else {
            Graphics2D g = imgSoFar.createGraphics();
            setupDrawingComposite(g, isFirstVisibleLayer);
            g.drawImage(transformed, 0, 0, null);
            g.dispose();
            return imgSoFar;
        }
    }

    /**
     * Used by adjustment layers and watermarked text layers
     * to apply this layer's effect on the given image.
     */
    protected abstract BufferedImage applyOnImage(BufferedImage src);

    public abstract CompletableFuture<Void> resize(Dimension newSize);

    /**
     * The given crop rectangle is given in image space,
     * relative to the canvas
     */
    public abstract void crop(Rectangle2D cropRect,
                              boolean deleteCroppedPixels,
                              boolean allowGrowing);

    public void changeStackIndex(int newIndex) {
        comp.changeStackIndex(this, newIndex);
    }

    /**
     * Returns true if the layer is in normal mode and the opacity is 100%
     */
    public boolean isNormalAndOpaque() {
        return blendingMode == BlendingMode.NORMAL
            && opacity > BlendingModePanel.CRITICAL_OPACITY;
    }

    /**
     * Configures the composite of the given Graphics,
     * according to the blending mode and opacity of the layer
     */
    public void setupDrawingComposite(Graphics2D g, boolean isFirstVisibleLayer) {
        if (isFirstVisibleLayer) {
            // the first visible layer is always painted with normal mode
            g.setComposite(AlphaComposite.getInstance(SRC_OVER, opacity));
        } else {
            Composite composite = blendingMode.getComposite(opacity);
            g.setComposite(composite);
        }
    }

    /**
     * On this level startMovement, moveWhileDragging and
     * endMovement only care about the movement of the linked
     * mask or owner.
     * This object's own movement is handled in {@link ContentLayer}.
     */
    public void startMovement() {
        Layer linked = getLinked();
        if (linked != null) {
            linked.startMovement();
        }
    }

    public void moveWhileDragging(double x, double y) {
        Layer linked = getLinked();
        if (linked != null) {
            linked.moveWhileDragging(x, y);
        }
    }

    public PixelitorEdit endMovement() {
        // Returns the edit of the linked layer.
        // Handles the case when we are in an adjustment
        // layer and the layer mask needs to be moved.
        // Otherwise the ContentLayer will override this,
        // and call super for the linked edit.
        Layer linked = getLinked();
        if (linked != null) {
            return linked.endMovement();
        }
        return null;
    }

    /**
     * Returns the layer that should move together with the current one,
     * (Assuming that we are in the edited layer)
     * or null if this layer should move alone
     */
    protected Layer getLinked() {
        if (hasMask()) {
            if (!maskEditing) { // we are in the edited layer
                if (mask.isLinked()) {
                    return mask;
                }
            }
        }
        return null;
    }

    /**
     * Return a canvas-sized image representing this layer.
     * This can be the temporarily rasterized image of a text layer.
     *
     * @param applyMask if false, then the mask is ignored
     */
    public abstract BufferedImage asImage(boolean applyMask);

    public void addListener(LayerListener listener) {
        listeners.add(listener);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    protected void notifyListeners() {
        for (LayerListener listener : listeners) {
            listener.layerStateChanged(this);
        }
    }

    public JPopupMenu createLayerIconPopupMenu() {
        if (comp.canMergeDown(this)) {
            JPopupMenu popup = new JPopupMenu();
            var mergeDownAction = new PAction(GUIText.MERGE_DOWN) {
                @Override
                public void onClick() {
                    comp.mergeDown(Layer.this);
                }
            };
            mergeDownAction.setToolTip(GUIText.MERGE_DOWN_TT);
            popup.add(mergeDownAction);
            return popup;
        }
        return null;
    }

    @Override
    public String toString() {
        return "{name='" + name + '\''
            + ", visible=" + visible
            + ", mask=" + mask
            + ", maskEditing=" + maskEditing
            + ", maskEnabled=" + maskEnabled
            + ", isAdjustment=" + isAdjustment + '}';
    }
}
