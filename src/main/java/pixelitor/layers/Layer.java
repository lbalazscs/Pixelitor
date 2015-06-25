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
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.history.LayerBlendingEdit;
import pixelitor.history.LayerOpacityEdit;
import pixelitor.history.LayerRenameEdit;
import pixelitor.history.LayerVisibilityChangeEdit;
import pixelitor.utils.HistogramsPanel;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * The abstract superclass of all layer classes
 */
public abstract class Layer implements Serializable {
    private static final long serialVersionUID = 2L;

    protected Canvas canvas;
    String name;
    private boolean visible = true;
    final Composition comp;
    LayerMask layerMask;

    private transient LayerButton layerButton;

    float opacity = 1.0f;
    BlendingMode blendingMode = BlendingMode.NORMAL;

    /**
     * Whether the edited image is the layer image or
     * the layer mask image.
     * This flag is logically independent from the showLayerMask
     * flag in the image component.
     */
    protected boolean layerMaskEditing = false;

    Layer(Composition comp, String name) {
        this.comp = comp;
        this.name = name;
        this.opacity = 1.0f;

        layerButton = new LayerButton(this);
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

        layerButton.setName(newName);

        if (addToHistory == AddToHistory.YES) {
            LayerRenameEdit edit = new LayerRenameEdit(this, previousName, name);
            History.addEdit(edit);
        }
    }

    public String getName() {
        return name;
    }

    public Composition getComposition() {
        return comp;
    }

    public abstract void mergeDownOn(ImageLayer bellow);

    public void makeActive(AddToHistory addToHistory) {
        comp.setActiveLayer(this, addToHistory);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        layerButton = new LayerButton(this);
    }

    boolean isActiveLayer() {
        return comp.isActiveLayer(this);
    }

    public boolean hasLayerMask() {
        return layerMask != null;
    }

    public void addLayerMask(LayerMaskAddType addType) {
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        BufferedImage bwLayerMask = addType.getBWImage(canvasWidth, canvasHeight);
        layerMask = new LayerMask(comp, bwLayerMask);

        comp.imageChanged(FULL);
    }

    /**
     * A layer can choose to draw on the Graphics2D or change the given BufferedImage.
     * If the BufferedImage is changed, the method returns the new image and null otherwise.
     * The reason is that adjustment layers change a BufferedImage, while other layers
     * just change the graphics
     */
    public abstract BufferedImage paintLayer(Graphics2D g, boolean firstVisibleLayer, BufferedImage imageSoFar);

    public abstract void resize(int targetWidth, int targetHeight, boolean progressiveBilinear);

    public abstract void crop(Rectangle selectionBounds);

    public LayerMask getLayerMask() {
        return layerMask;
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

    public void deleteLayerMask() {
        layerMask = null;
        layerMaskEditing = false;
    }

    public void setLayerMaskEditing(boolean b) {
        this.layerMaskEditing = b;
    }

    public boolean isLayerMaskEditing() {
        if (layerMaskEditing) {
            assert layerMask != null;
        }
        return layerMaskEditing;
    }
}
