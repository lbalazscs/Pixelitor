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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that changes the canvas size,
 * such as resize, crop, enlarge layer, or rotation.
 * Always part of a MultiLayerEdit.
 */
public class CanvasChangeEdit extends PixelitorEdit {
    private int backupTranslationX;
    private int backupTranslationY;

    private int backupCanvasWidth;
    private int backupCanvasHeight;
    private final ImageLayer layer;

    public CanvasChangeEdit(String name,
                            Composition comp) {
        super(comp, name);
        embedded = true;

        layer = comp.getAnyImageLayer();
        if (layer != null) { // could be null, if there are only text layers
            backupTranslationX = layer.getTranslationX();
            backupTranslationY = layer.getTranslationY();
        }

        backupCanvasWidth = comp.getCanvasWidth();
        backupCanvasHeight = comp.getCanvasHeight();
    }

    @Override
    public boolean canUndo() {
        return super.canUndo();
    }

    @Override
    public boolean canRedo() {
        return super.canRedo();
    }

    @Override
    public boolean canRepeat() {
        return false;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        swapCanvasDimensions();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        swapCanvasDimensions();
    }

    private void swapCanvasDimensions() {
        if (layer != null) {
            int tmpTranslationX = layer.getTranslationX();
            int tmpTranslationY = layer.getTranslationY();
            layer.setTranslation(backupTranslationX, backupTranslationY);
            backupTranslationX = tmpTranslationX;
            backupTranslationY = tmpTranslationY;
        }

        int tmpCanvasWidth = comp.getCanvasWidth();
        int tmpCanvasHeight = comp.getCanvasHeight();

        // TODO think about the translation of the mask
        comp.getCanvas().updateSize(backupCanvasWidth, backupCanvasHeight);

        backupCanvasWidth = tmpCanvasWidth;
        backupCanvasHeight = tmpCanvasHeight;

        if (!embedded) {
            comp.updateAllIconImages();
            History.notifyMenus(this);
        }
    }
}