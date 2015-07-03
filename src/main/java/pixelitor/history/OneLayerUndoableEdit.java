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
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * A PixelitorEdit representing an operation that can affect multiple layers,
 * such as resize, a crop, flip, or image rotation.
 * These are undoable only if the composition consists of a single layer
 */
// TODO find better name for the class - perhaps MultiLayerEdit. Or maybe redesign it.
public class OneLayerUndoableEdit extends PixelitorEdit {
    private BufferedImage backupImage;
    private final boolean saveSelection;
    private int backupTranslationX;
    private int backupTranslationY;

    private int backupCanvasWidth;
    private int backupCanvasHeight;

    private Shape backupShape;

    /**
     * Private constructor, use the static createAndAddToHistory method from outside
     */
    private OneLayerUndoableEdit(String presentationName, BufferedImage backupImage, Composition comp, boolean saveSelection) {
        super(comp, presentationName);
        this.backupImage = backupImage;
        this.saveSelection = saveSelection;

        if (saveSelection) {
            Optional<Selection> selection = comp.getSelection();
            if (selection.isPresent()) {
                backupShape = selection.get().getShape();
            }
        }

        int nrLayers = comp.getNrLayers();
        if (backupImage != null) {
            if (nrLayers != 1) { // make backups only if there is only one layer
                throw new IllegalArgumentException("(backupImage != null, nrLayers = " + nrLayers);
            }

            // TODO this will never return the layer mask
            ImageLayer layer = (ImageLayer) comp.getActiveLayer();

            backupTranslationX = layer.getTranslationX();
            backupTranslationY = layer.getTranslationY();

            backupCanvasWidth = comp.getCanvasWidth();
            backupCanvasHeight = comp.getCanvasHeight();
        }
    }

    public static void createAndAddToHistory(Composition comp, String presentationName, boolean saveSubImageOnly, boolean saveSelection) {
        int nrLayers = comp.getNrLayers();
        if (nrLayers > 1) {
            History.addEdit(new OneLayerUndoableEdit(presentationName, null, comp, saveSelection));
        } else {
            BufferedImage backup = null;
            Layer layer = comp.getLayer(0);
            if (layer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                if (saveSubImageOnly) {
                    backup = imageLayer.getImageOrSubImageIfSelected(false, true);
                } else {
                    backup = imageLayer.getImage(); // for crop/resize  we save the whole image
                }
            }

            History.addEdit(new OneLayerUndoableEdit(presentationName, backup, comp, saveSelection));
        }
    }

    @Override
    public boolean canUndo() {
        if (backupImage == null) {
            return false;
        }
        return super.canUndo();
    }

    @Override
    public boolean canRedo() {
        if (backupImage == null) {
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

//        System.out.println("OneLayerUndoableEdit.undo CALLED");
//        AppLogic.debugImage(backupImage, "backup before undo");

        swapImages();

        if (saveSelection && (backupShape != null)) {
            comp.createSelectionFromShape(backupShape);
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        if (saveSelection) {
            comp.deselect(AddToHistory.NO);
        }
        swapImages();
    }

    private void swapImages() {
        if (comp.getNrLayers() != 1) {
            throw new IllegalStateException("nr of layers = " + comp.getNrLayers());
        }

        // TODO this will never return the layer mask
        ImageLayer layer = (ImageLayer) comp.getActiveLayer();
        BufferedImage tmp = layer.getImageOrSubImageIfSelected(false, true);

        int tmpTranslationX = layer.getTranslationX();
        int tmpTranslationY = layer.getTranslationY();

        int tmpCanvasWidth = comp.getCanvasWidth();
        int tmpCanvasHeight = comp.getCanvasHeight();

        comp.getActiveImageLayerOrMask().changeImageUndoRedo(backupImage);

        if (!comp.hasSelection()) {
            layer.setTranslationX(backupTranslationX);
            layer.setTranslationY(backupTranslationY);
            comp.getCanvas().updateSize(backupCanvasWidth, backupCanvasHeight);
        }

        backupImage = tmp;
        backupTranslationX = tmpTranslationX;
        backupTranslationY = tmpTranslationY;
        backupCanvasWidth = tmpCanvasWidth;
        backupCanvasHeight = tmpCanvasHeight;

        History.notifyMenus(this);
    }
}