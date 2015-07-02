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

package pixelitor.layers;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.history.AddLayerMaskEdit;
import pixelitor.history.AddToHistory;
import pixelitor.history.CompoundEdit;
import pixelitor.history.DeleteLayerMaskEdit;
import pixelitor.history.DeselectEdit;
import pixelitor.history.History;
import pixelitor.history.LayerBlendingEdit;
import pixelitor.history.LayerOpacityEdit;
import pixelitor.history.LayerRenameEdit;
import pixelitor.history.LayerVisibilityChangeEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.selection.Selection;
import pixelitor.utils.Dialogs;
import pixelitor.utils.HistogramsPanel;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import static java.awt.AlphaComposite.SRC_OVER;
import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * The abstract superclass of all layer classes
 */
public abstract class Layer implements Serializable {
    private static final long serialVersionUID = 2L;

    protected Canvas canvas;
    String name;
    private boolean isMask = false;
    private boolean visible = true;
    final Composition comp;
    protected LayerMask mask;

    private transient LayerButton layerButton;

    float opacity = 1.0f;
    BlendingMode blendingMode = BlendingMode.NORMAL;

    /**
     * Whether the edited image is the layer image or
     * the layer mask image.
     * This flag is logically independent from the showLayerMask
     * flag in the image component.
     */
    protected boolean maskEditing = false;

    Layer(Composition comp, String name, Layer parent) {
        this.comp = comp;
        this.name = name;
        this.isMask = parent != null;
        this.opacity = 1.0f;

        canvas = comp.getCanvas();

        if (parent != null) { // this is a layer mask
            layerButton = parent.getLayerButton();
        } else { // normal layer
            layerButton = new LayerButton(this);
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
        comp.setDirty(true);
        layerButton.setOpenEye(newVisibility);

        if (addToHistory == AddToHistory.YES) {
            LayerVisibilityChangeEdit edit = new LayerVisibilityChangeEdit(comp, this, newVisibility);
            History.addEdit(edit);
        }
    }

    public LayerButton getLayerButton() {
        return layerButton;
    }

    public void setLayerButton(LayerButton layerButton) {
        this.layerButton = layerButton;
    }

    String getDuplicateLayerName() {
        String copyString = "copy"; // could be longer or shorter in other languages
        int copyStringLength = copyString.length();

        int index = name.lastIndexOf(copyString);
        if (index == -1) {
            return name + ' ' + copyString;
        }
        if (index == name.length() - copyStringLength) {
            // it ends with the copyString - this was the first copy
            return name + " 2";
        }
        String afterCopyString = name.substring(index + copyStringLength);

        int copyNr;
        try {
            copyNr = Integer.parseInt(afterCopyString.trim());
        } catch (NumberFormatException e) {
            // the part after copy was not a number...
            return name + ' ' + copyString;
        }

        copyNr++;

        return name.substring(0, index + copyStringLength) + ' ' + copyNr;
    }

    public abstract Layer duplicate();

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

    public void setOpacity(float newOpacity, boolean updateGUI, AddToHistory addToHistory, boolean updateImage) {
        assert newOpacity <= 1.0f : "newOpacity = " + newOpacity;
        assert newOpacity >= 0.0f : "newOpacity = " + newOpacity;

        if (addToHistory == AddToHistory.YES) {
            LayerOpacityEdit edit = new LayerOpacityEdit(this, opacity);
            History.addEdit(edit);
        }

        this.opacity = newOpacity;

        if (updateGUI) {
            LayerBlendingModePanel.INSTANCE.setOpacity(newOpacity);
        }
        if(updateImage) {
            updateAfterBMorOpacityChange();
        }
    }

    public void setBlendingMode(BlendingMode mode, boolean updateGUI, AddToHistory addToHistory, boolean updateImage) {
        if (addToHistory == AddToHistory.YES) {
            LayerBlendingEdit edit = new LayerBlendingEdit(this, blendingMode);
            History.addEdit(edit);
        }

        this.blendingMode = mode;
        if (updateGUI) {
            LayerBlendingModePanel.INSTANCE.setBlendingModeNotUI(mode);
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

        layerButton.changeNameProgrammatically(newName);

        if (addToHistory == AddToHistory.YES) {
            LayerRenameEdit edit = new LayerRenameEdit(this, previousName, name);
            History.addEdit(edit);
        }
    }

    public String getName() {
        return name;
    }

    public Composition getComp() {
        return comp;
    }

    public abstract void mergeDownOn(ImageLayer bellow);

    public void makeActive(AddToHistory addToHistory) {
        comp.setActiveLayer(this, addToHistory);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // We create a layer button only for real layers.
        // For layer masks, we share the button of the real layer.
        if (!isMask) {
            layerButton = new LayerButton(this);

            if (mask != null) {
                mask.setLayerButton(layerButton);
                layerButton.addMaskIcon();
                mask.updateIconImage();
            }
        }
    }

    boolean isActive() {
        return comp.isActiveLayer(this);
    }

    public boolean hasMask() {
        return mask != null;
    }

    public void addMask(LayerMaskAddType addType) {
        if (mask != null) {
            Dialogs.showInfoDialog("Has layer mask",
                    String.format("The layer \"%s\" already has a layer mask.", getName()));
            return;
        }
        Selection selection = comp.getSelectionOrNull();
        if (addType.missingSelection(selection)) {
            Dialogs.showInfoDialog("No selection",
                    String.format("The composition \"%s\" has no selection.", comp.getName()));
            return;
        }

        // needs to be added first, because the inherited layer
        // mask constructor already will try to update the image
        layerButton.addMaskIcon();

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        BufferedImage bwMask = addType.getBWImage(canvasWidth, canvasHeight, selection);
        mask = new LayerMask(comp, bwMask, this);

        comp.imageChanged(FULL);

        PixelitorEdit edit = new AddLayerMaskEdit(comp, this);
        if (addType.needsSelection()) {
            Shape backupShape = selection.getShape();
            comp.deselect(AddToHistory.NO);
            DeselectEdit deselectEdit = new DeselectEdit(comp, backupShape, "nested deselect");
            edit = new CompoundEdit(comp, "Layer Mask from Selection", edit, deselectEdit);
        }

        History.addEdit(edit);
    }

    // called if the deletion of a layer mask is undone
    public void addMaskBack(LayerMask mask) {
        this.mask = mask;
        comp.imageChanged(FULL);
        layerButton.addMaskIcon();
        mask.updateIconImage();
    }

    public void deleteMask(AddToHistory addToHistory) {
        LayerMask oldMask = mask;
        mask = null;
        maskEditing = false;

        comp.imageChanged(FULL);

        if (addToHistory == AddToHistory.YES) {
            DeleteLayerMaskEdit edit = new DeleteLayerMaskEdit(comp, this, oldMask);
            History.addEdit(edit);
        }

        layerButton.deleteMaskIcon();
    }

    /**
     * A layer can choose to draw on the Graphics2D or change the given BufferedImage.
     * If the BufferedImage is changed, the method returns the new image and null otherwise.
     * The reason is that adjustment layers and watermarked text layers change a BufferedImage,
     * while other layers just paint on the graphics
     */
    public abstract BufferedImage paintLayer(Graphics2D g, boolean firstVisibleLayer, BufferedImage imageSoFar);

    public abstract void resize(int targetWidth, int targetHeight, boolean progressiveBilinear);

    protected void resizeMask(int targetWidth, int targetHeight, boolean progressiveBilinear) {
        if (mask != null) {
            mask.resize(targetWidth, targetHeight, progressiveBilinear);
        }
    }

    public abstract void crop(Rectangle selectionBounds);

    protected void cropMask(Rectangle selectionBounds) {
        if (mask != null) {
            mask.crop(selectionBounds);
        }
    }

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
        this.maskEditing = b;
    }

    public boolean isMaskEditing() {
        if (maskEditing) {
            assert mask != null;
        }
        return maskEditing;
    }

    /**
     * Returns true if the layer is in normal mode and the opacity is 100%
     */
    public boolean isNormalAndOpaque() {
        return blendingMode == BlendingMode.NORMAL && opacity > 0.999f;
    }

    /**
     * Configures tha composite of the given Graphics,
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
}
