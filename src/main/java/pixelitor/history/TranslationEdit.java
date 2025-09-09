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

package pixelitor.history;

import pixelitor.Composition;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.LayerMask;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Saves and restores the translation of a ContentLayer.
 * Not to be confused with {@link ContentLayerMoveEdit}:
 * this one is used only internally by other edits
 */
public class TranslationEdit extends PixelitorEdit {
    private final ContentLayer layer;
    private int backupTx = 0;
    private int backupTy = 0;

    private TranslationEdit maskEdit;

    /**
     * This constructor must be called before the change because
     * the current translation is considered the old value
     */
    public TranslationEdit(Composition comp, ContentLayer layer, boolean considerMask) {
        this(comp, layer, layer.getTx(), layer.getTy(), considerMask);
    }

    /**
     * This constructor can be called after the change
     * if the mask can be ignored
     */
    public TranslationEdit(Composition comp, ContentLayer layer, int oldTx, int oldTy, boolean considerMask) {
        // needs no name, because this is always embedded into another edit
        super("", comp);
        embedded = true;

        this.layer = layer;
        backupTx = oldTx;
        backupTy = oldTy;

        if (considerMask && layer.hasMask()) {
            LayerMask mask = layer.getMask();
            maskEdit = new TranslationEdit(comp, mask, mask.getTx(), mask.getTy(), false);
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        swapTranslation();
        if (maskEdit != null) {
            maskEdit.undo();
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        swapTranslation();
        if (maskEdit != null) {
            maskEdit.redo();
        }
    }

    private void swapTranslation() {
        int tmpTx = layer.getTx();
        int tmpTy = layer.getTy();

        layer.setTranslation(backupTx, backupTy);
        backupTx = tmpTx;
        backupTy = tmpTy;

        if (!embedded) {
            layer.update();
        }
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addInt("backup tx", backupTx);
        node.addInt("backup ty", backupTy);
        node.add(layer.createDebugNode("layer"));
        node.addNullableDebuggable("mask edit", maskEdit);
        return node;
    }
}
