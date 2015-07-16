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

package pixelitor.history;

import pixelitor.Composition;
import pixelitor.layers.ImageLayer;
import pixelitor.selection.IgnoreSelection;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * A PixelitorEdit representing an operation that can affect multiple layers,
 * such as resize, a crop, flip, or image rotation.
 * These are undoable only if the composition has a single image layer
 */
public class MultiLayerEdit extends PixelitorEdit {
    private ImageLayer layer;

    private final ImageEdit imageEdit;
    private final CanvasChangeEdit canvasChangeEdit;
    private SelectionChangeEdit selectionChangeEdit;
    private DeselectEdit deselectEdit;

    public MultiLayerEdit(Composition comp, String name, BufferedImage backupImage, CanvasChangeEdit canvasChangeEdit) {
        super(comp, name);
        this.canvasChangeEdit = canvasChangeEdit;

        int nrLayers = comp.getNrImageLayers();
        if (nrLayers == 1) {
            layer = comp.getAnyImageLayer();
            imageEdit = new ImageEdit("", comp, layer, backupImage,
                    IgnoreSelection.YES, false);
            imageEdit.setEmbedded(true);
        } else {
            imageEdit = null;
        }
    }

    public void setSelectionChangeEdit(SelectionChangeEdit selectionChangeEdit) {
        this.selectionChangeEdit = selectionChangeEdit;
    }

    public void setDeselectEdit(DeselectEdit deselectEdit) {
        this.deselectEdit = deselectEdit;
    }

    @Override
    public boolean canUndo() {
        if (imageEdit == null) {
            return false;
        }
        return super.canUndo();
    }

    @Override
    public boolean canRedo() {
        if (imageEdit == null) {
            return false;
        }
        return super.canRedo();
    }

    @Override
    public boolean canRepeat() {
        return false;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        imageEdit.undo();
        if (canvasChangeEdit != null) {
            canvasChangeEdit.undo();
        }
        if (selectionChangeEdit != null) {
            selectionChangeEdit.undo();
        }
        if (deselectEdit != null) {
            deselectEdit.undo();
        }

        comp.imageChanged(FULL);
        layer.updateIconImage();
        History.notifyMenus(this);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        imageEdit.redo();
        if (canvasChangeEdit != null) {
            canvasChangeEdit.redo();
        }
        if (selectionChangeEdit != null) {
            selectionChangeEdit.redo();
        }
        if (deselectEdit != null) {
            deselectEdit.redo();
        }

        comp.imageChanged(FULL);
        layer.updateIconImage();
        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        imageEdit.die();
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