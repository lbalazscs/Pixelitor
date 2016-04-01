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

package pixelitor.layers;

import pixelitor.AppLogic;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.HistogramsPanel;
import pixelitor.gui.ImageComponent;
import pixelitor.history.AddLayerMaskEdit;
import pixelitor.history.AddToHistory;
import pixelitor.history.DeleteLayerMaskEdit;
import pixelitor.history.DeselectEdit;
import pixelitor.history.EnableLayerMaskEdit;
import pixelitor.history.History;
import pixelitor.history.LayerBlendingEdit;
import pixelitor.history.LayerOpacityEdit;
import pixelitor.history.LayerRenameEdit;
import pixelitor.history.LayerVisibilityChangeEdit;
import pixelitor.history.LinkedEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.selection.Selection;
import pixelitor.utils.Messages;
import pixelitor.utils.UpdateGUI;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.awt.AlphaComposite.DstIn;
import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * The abstract superclass of all layer classes
 */
public abstract class Layer implements Serializable {
    private static final long serialVersionUID = 2L;

    protected Canvas canvas;
    protected String name;

    // the real layer for layer masks,
    // null for real layers
    protected final Layer parent;

    private boolean visible = true;
    protected Composition comp;
    protected LayerMask mask;
    private boolean maskEnabled = true;

    float opacity = 1.0f;
    BlendingMode blendingMode = BlendingMode.NORMAL;

    // transient variables from here
    private transient LayerGUI ui;
    protected transient boolean isAdjustment = false;
    private transient List<LayerChangeListener> layerChangeObservers;

    /**
     * Whether the edited image is the layer image or
     * the layer mask image.
     * This flag is logically independent from the showLayerMask
     * flag in the image component.
     */
    private transient boolean maskEditing = false;

    Layer(Composition comp, String name, Layer parent) {
        this.comp = comp;
        this.name = name;
        this.parent = parent;
        this.opacity = 1.0f;

        canvas = comp.getCanvas();

        if (parent != null) { // this is a layer mask
            ui = parent.getUI();
        } else { // normal layer
            ui = new LayerGUI(this);
        }
        layerChangeObservers = new ArrayList<>();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        layerChangeObservers = new ArrayList<>();

        // We create a layer button only for real layers.
        // For layer masks, we share the button of the real layer.
        if (parent == null) { // not mask
            ui = new LayerGUI(this);

            if (mask != null) {
                mask.setUI(ui);
            }
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean newVisibility, AddToHistory addToHistory) {
        if (this.visible == newVisibility) {
            return;
        }

        this.visible = newVisibility;
        comp.imageChanged(FULL);
        ui.setOpenEye(newVisibility);

        History.addEdit(addToHistory, () -> new LayerVisibilityChangeEdit(comp, this, newVisibility));
    }

    public LayerGUI getUI() {
        return ui;
    }

    public void setUI(LayerGUI ui) {
        this.ui = ui;
    }

    /**
     * If sameName is true, then the duplicate layer will
     * have the same name, otherwise a new "copy name" is generated
     */
    public abstract Layer duplicate(boolean sameName);

    public float getOpacity() {
        return opacity;
    }

    public BlendingMode getBlendingMode() {
        return blendingMode;
    }

    private void updateAfterBMorOpacityChange() {
        comp.imageChanged(FULL);

        HistogramsPanel hp = HistogramsPanel.INSTANCE;
        if (hp.areHistogramsShown()) {
            hp.updateFromCompIfShown(comp);
        }
    }

    public void setOpacity(float newOpacity, UpdateGUI updateGUI, AddToHistory addToHistory, boolean updateImage) {
        assert newOpacity <= 1.0f : "newOpacity = " + newOpacity;
        assert newOpacity >= 0.0f : "newOpacity = " + newOpacity;

        if (opacity == newOpacity) {
            return;
        }

        History.addEdit(addToHistory, () -> new LayerOpacityEdit(this, opacity));

        this.opacity = newOpacity;

        if (updateGUI.isYes()) {
            ui.setOpacityFromModel(newOpacity);
        }
        if(updateImage) {
            updateAfterBMorOpacityChange();
        }
    }

    public void setBlendingMode(BlendingMode mode, UpdateGUI updateGUI, AddToHistory addToHistory, boolean updateImage) {
        History.addEdit(addToHistory, () -> new LayerBlendingEdit(this, blendingMode));

        this.blendingMode = mode;
        if (updateGUI.isYes()) {
            LayerBlendingModePanel.INSTANCE.setBlendingModeFromModel(mode);
        }

        if(updateImage) {
            updateAfterBMorOpacityChange();
        }
    }

    public void setName(String newName, AddToHistory addToHistory) {
        String previousName = name;
        this.name = newName;

        if (name.equals(previousName)) { // important because this might be called twice for a single rename
            return;
        }

        ui.changeNameProgrammatically(newName);

        History.addEdit(addToHistory, () -> new LayerRenameEdit(this, previousName, name));
    }

    public String getName() {
        return name;
    }

    public Composition getComp() {
        return comp;
    }

    public void setComp(Composition comp) {
        this.comp = comp;
    }

    public void mergeDownOn(ImageLayer bellow) {
        // TODO what about translations of the bellow layer

        BufferedImage bellowImage = bellow.getImage();
        Graphics2D g = bellowImage.createGraphics();
        BufferedImage result = applyLayer(g, false, bellowImage);
        if(result != null) {  // this was an adjustment
            bellow.setImage(result);
        }
        g.dispose();
    }

    public void makeActive(AddToHistory addToHistory) {
        comp.setActiveLayer(this, addToHistory);
    }

    boolean isActive() {
        return comp.isActiveLayer(this);
    }

    public boolean hasMask() {
        return mask != null;
    }

    public void addMask(LayerMaskAddType addType) {
        if (mask != null) {
            Messages.showInfo("Has layer mask",
                    String.format("The layer \"%s\" already has a layer mask.", getName()));
            return;
        }
        Selection selection = comp.getSelection();
        if (addType.missingSelection(selection)) {
            Messages.showInfo("No selection",
                    String.format("The composition \"%s\" has no selection.", comp.getName()));
            return;
        }

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        BufferedImage bwMask = addType.getBWImage(canvasWidth, canvasHeight, selection);


        String editName = "Add Layer Mask";
        boolean deselect = addType.needsSelection();
        if (deselect) {
            editName = "Layer Mask from Selection";
        }

        addImageAsMask(bwMask, deselect, editName, false);
    }

    public void addImageAsMask(BufferedImage bwMask, boolean deselect, String editName, boolean inheritTranslation) {
        assert mask == null;

        mask = new LayerMask(comp, bwMask, this, inheritTranslation);
        maskEnabled = true;

        // needs to be added first, because the inherited layer
        // mask constructor already will try to update the image
        ui.addMaskIconLabel();

        comp.imageChanged(FULL);

        AppLogic.maskChanged(this);

        PixelitorEdit edit = new AddLayerMaskEdit(comp, this, editName);
        if (deselect) {
            Shape backupShape = comp.getSelectionShape();
            comp.deselect(AddToHistory.NO);
            if (backupShape != null) { // TODO on Mac Random GUI test we can get null here
                DeselectEdit deselectEdit = new DeselectEdit(comp, backupShape, "nested deselect");
                edit = new LinkedEdit(comp, editName, edit, deselectEdit);
            }
        }

        History.addEdit(edit);
        MaskViewMode.EDIT_MASK.activate(comp, this);
    }

    public void addOrReplaceMaskImage(BufferedImage bwMask, String editName) {
        if (hasMask()) {
            mask.replaceImage(bwMask, editName);
        } else {
            addImageAsMask(bwMask, false, editName, true);
        }
    }

    /**
     * Adds a mask that is already configured to be used
     * with this layer
     */
    public void addMask(LayerMask mask) {
        assert mask != null;
        assert mask.getParent() == this;

        this.mask = mask;
        comp.imageChanged(FULL);
        ui.addMaskIconLabel();
        AppLogic.maskChanged(this);
        mask.updateIconImage();
    }

    public void deleteMask(AddToHistory addToHistory) {
        LayerMask oldMask = mask;
        ImageComponent ic = comp.getIC();
        MaskViewMode oldMode = ic.getMaskViewMode();
        mask = null;
        maskEditing = false;

        comp.imageChanged(FULL);

        History.addEdit(addToHistory, () -> new DeleteLayerMaskEdit(comp, this, oldMask, oldMode));

        AppLogic.maskChanged(this);
        ui.deleteMaskIconLabel();

        MaskViewMode.NORMAL.activate(ic, this);
    }

    /**
     * Applies the effect of this layer on the given Graphics2D or on the given BufferedImage.
     * Adjustment layers and watermarked text layers change a BufferedImage, while other layers
     * just paint on the graphics.
     * If the BufferedImage is changed, the method returns the new image and null otherwise.
     */
    public BufferedImage applyLayer(Graphics2D g, boolean firstVisibleLayer, BufferedImage imageSoFar) {
        if (isAdjustment) { // adjustment layer or watermarked text layers
            return adjustImageWithMasksAndBlending(imageSoFar, firstVisibleLayer);
        } else {
            if (!useMask()) {
                setupDrawingComposite(g, firstVisibleLayer);
                paintLayerOnGraphics(g, firstVisibleLayer);
            } else {
                paintLayerOnGraphicsWithMask(firstVisibleLayer, g);
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
    private void paintLayerOnGraphicsWithMask(boolean firstVisibleLayer, Graphics2D g) {
//        Canvas canvas = comp.getCanvas();

        // 1. create the masked image
        // TODO the masked image should be cached
        BufferedImage maskedImage = new BufferedImage(canvas.getWidth(), canvas.getHeight(), TYPE_INT_ARGB);
        Graphics2D mig = maskedImage.createGraphics();
        paintLayerOnGraphics(mig, firstVisibleLayer);
        mig.setComposite(DstIn);
        mig.drawImage(mask.getTransparencyImage(), mask.getTX(), mask.getTY(), null);
        mig.dispose();

        // 2. paint the masked image onto the graphics
//            g.drawImage(maskedImage, getTX(), getTY(), null);
        setupDrawingComposite(g, firstVisibleLayer);
        g.drawImage(maskedImage, 0, 0, null);
    }

    /**
     * Used by adjustment layers and watermarked text layers
     */
    private BufferedImage adjustImageWithMasksAndBlending(BufferedImage imgSoFar, boolean isFirstVisibleLayer) {
        if (isFirstVisibleLayer) {
            return imgSoFar; // there's nothing we can do
        }
        BufferedImage transformed = adjustImage(imgSoFar);
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
     */
    protected abstract BufferedImage adjustImage(BufferedImage src);

    public abstract void resize(int targetWidth, int targetHeight, boolean progressiveBilinear);

    public abstract void crop(Rectangle2D cropRect);

    public LayerMask getMask() {
        return mask;
    }

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    public Object getVisibilityAsORAString() {
        return isVisible() ? "visible" : "hidden";
    }

    public void dragFinished(int newIndex) {
        comp.dragFinished(this, newIndex);
    }

    public void setMaskEditing(boolean b) {
        assert b ? hasMask() : true;
        this.maskEditing = b;
        ui.setMaskEditing(b); // sets the border around the icon
    }

    public boolean isMaskEditing() {
        assert maskEditing ? hasMask() : true;
        return maskEditing;
    }

    /**
     * Returns true if the layer is in normal mode and the opacity is 100%
     */
    protected boolean isNormalAndOpaque() {
        return blendingMode == BlendingMode.NORMAL && opacity > 0.999f;
    }

    /**
     * Configures the composite of the given Graphics,
     * according to the blending mode and opacity of the layer
     */
    public void setupDrawingComposite(Graphics2D g, boolean isFirstVisibleLayer) {
        if (isFirstVisibleLayer) {  // the first visible layer is always painted with normal mode
            g.setComposite(AlphaComposite.getInstance(SRC_OVER, opacity));
        } else {
            Composite composite = blendingMode.getComposite(opacity);
            g.setComposite(composite);
        }
    }

    // On this level startMovement, moveWhileDragging and
    // endMovement only care about the movement of the
    // mask or parent. Our own movement is handled in
    // ContentLayer.
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
            //noinspection TailRecursion
            return linked.endMovement();
        }
        return null;
    }

    /**
     * Returns the layer that should move together with the current one,
     * (Assuming that we are in the edited layer)
     * or null if this layer should move alone
     */
    private Layer getLinked() {
        if (mask != null) {
            if (!maskEditing) { // we are in the edited layer
                if (mask.isLinked()) {
                    return mask;
                }
            }
        }
        if (parent != null) { // we are in a mask
            if (parent.isMaskEditing()) { // we are in the edited layer
                if (((LayerMask) this).isLinked()) {
                    return parent;
                }
            }
        }
        return null;
    }

    public boolean isMaskEnabled() {
        return maskEnabled;
    }

    public void setMaskEnabled(boolean maskEnabled, AddToHistory addToHistory) {
        assert mask != null;
        this.maskEnabled = maskEnabled;

        comp.imageChanged(FULL);
        mask.updateIconImage();
        notifyLayerChangeObservers();

        History.addEdit(addToHistory, () -> new EnableLayerMaskEdit(comp, this));
    }

    private boolean useMask() {
        return mask != null && maskEnabled;
    }

    public Layer getParent() {
        return parent;
    }

    public void activateUI() {
        ui.setSelected(true);
    }

    public void addLayerChangeObserver(LayerChangeListener listener) {
        layerChangeObservers.add(listener);
    }

    protected void notifyLayerChangeObservers() {
        //noinspection Convert2streamapi
        for (LayerChangeListener observer : layerChangeObservers) {
            observer.layerStateChanged();
        }
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
