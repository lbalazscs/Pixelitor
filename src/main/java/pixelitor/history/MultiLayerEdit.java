/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.history;

import pixelitor.Composition;
import pixelitor.guides.GuidesChangeEdit;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * A PixelitorEdit representing an operation that can affect multiple layers,
 * such as resize, a crop, flip, or image rotation.
 * These are undoable only if the composition has a single layer
 */
public class MultiLayerEdit extends PixelitorEdit {
    private Layer layer;
    private ImageEdit imageEdit;
    private CanvasChangeEdit canvasChangeEdit;
    private TranslationEdit translationEdit;
    private SelectionChangeEdit selectionChangeEdit;
    private DeselectEdit deselectEdit;
    private GuidesChangeEdit guidesChangeEdit;
    private final AffineTransform forwardCanvasTx;
    private AffineTransform backwardCanvasTx;

    private final boolean undoable;

    public MultiLayerEdit(String name, Composition comp,
                          MultiLayerBackup backup,
                          AffineTransform canvasTx) {
        super(name, comp);
        this.forwardCanvasTx = canvasTx;

        int numLayers = comp.getNumLayers();
        if (numLayers == 1) {
            undoable = true;
            layer = comp.getLayer(0);
        } else {
            undoable = false;
        }

        if (undoable) {
            this.canvasChangeEdit = backup.getCanvasChangeEdit();
            this.translationEdit = backup.getTranslationEdit();
            this.guidesChangeEdit = backup.getGuidesChangeEdit();

            BufferedImage maskImage = null;
            if (layer.hasMask()) {
                maskImage = layer.getMask().getImage();
            }
            if (layer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                BufferedImage layerImage = imageLayer.getImage();
                imageEdit = backup.createImageEdit(layerImage, maskImage);
            } else if (layer.hasMask()) {
                // if we have a text layer with a mask, we can still
                // create an ImageEdit for the mask
                imageEdit = backup.createImageEditForMaskOnly(maskImage);
            }
        }

        if (comp.hasSelection()) {
            assert backup.hasSavedSelection();
            selectionChangeEdit = backup.createSelectionChangeEdit();
        } else {
            if (backup.hasSavedSelection()) {
                // it was a deselect:
                // either a selection crop or a crop tool crop without
                // overlap with the existing selection.
                deselectEdit = backup.createDeselectEdit();
            }
        }
    }

    @Override
    public boolean canUndo() {
        if (!undoable) {
            return false;
        }
        return super.canUndo();
    }

    @Override
    public boolean canRedo() {
        if (!undoable) {
            return false;
        }
        return super.canRedo();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        try {
            doTheUndo();
        } catch (Exception e) {
            dumpState();
            throw e;
        }

        updateGUI();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        try {
            doTheRedo();
        } catch (Exception e) {
            dumpState();
            throw e;
        }

        updateGUI();
    }

    private void doTheUndo() {
        if (imageEdit != null) {
            imageEdit.undo();
        }
        if (translationEdit != null) {
            translationEdit.undo();
        }
        // it is important to undo the canvas change edit
        // after the image and translation edits because
        // of the image covers canvas checks
        if (canvasChangeEdit != null) {
            canvasChangeEdit.undo();
            comp.getView().revalidate(); // make sure the scrollbars are OK
        }
        if (selectionChangeEdit != null) {
            selectionChangeEdit.undo();
        }
        if (deselectEdit != null) {
            deselectEdit.undo();
        }
        if (guidesChangeEdit != null) {
            guidesChangeEdit.undo();
        }

        if (backwardCanvasTx == null && forwardCanvasTx != null) {
            try {
                backwardCanvasTx = forwardCanvasTx.createInverse();
            } catch (NoninvertibleTransformException e) {
                e.printStackTrace();
            }
        }
        if (backwardCanvasTx != null) { // successful inversion
            comp.imCoordsChanged(backwardCanvasTx, true);
        }
    }

    private void doTheRedo() {
        if (imageEdit != null) {
            imageEdit.redo();
        }
        if (translationEdit != null) {
            translationEdit.redo();
        }
        // it is important to redo the canvas change edit
        // after the image and translation edits because
        // of the image covers canvas checks
        if (canvasChangeEdit != null) {
            canvasChangeEdit.redo();
        }
        if (selectionChangeEdit != null) {
            selectionChangeEdit.redo();
            comp.getView().revalidate(); // make sure the scrollbars are OK
        }
        if (deselectEdit != null) {
            deselectEdit.redo();
        }
        if (guidesChangeEdit != null) {
            guidesChangeEdit.redo();
        }

        if (backwardCanvasTx != null) { // the backwards transform was inverted successfully
            comp.imCoordsChanged(forwardCanvasTx, true);
        }
    }

    private void dumpState() {
        System.out.printf("MultiLayerEdit:EXCEPTION getName() = '%s'%n", getName());
    }

    private void updateGUI() {
        comp.imageChanged(FULL, true);
        if (layer instanceof ImageLayer) {
            ((ImageLayer) layer).updateIconImage();
        }
        if (layer.hasMask()) {
            layer.getMask().updateIconImage();
        }
    }

    @Override
    public void die() {
        super.die();

        if (imageEdit != null) {
            imageEdit.die();
        }
        if (translationEdit != null) {
            translationEdit.die();
        }
        if (canvasChangeEdit != null) {
            canvasChangeEdit.die();
        }
        if (selectionChangeEdit != null) {
            selectionChangeEdit.die();
        }
        if (deselectEdit != null) {
            deselectEdit.die();
        }
    }
}