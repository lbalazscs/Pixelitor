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

package pixelitor.history;

import pixelitor.Composition;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.LayerMask;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * Saves and restores the translation of a ContentLayer.
 */
public class TranslationEdit extends PixelitorEdit {
    private ContentLayer layer;
    private int backupTX = 0;
    private int backupTY = 0;

    private TranslationEdit maskEdit;

    /**
     * This constructor must be called before the change because
     * the current translation is considered the old value
     */
    public TranslationEdit(Composition comp, ContentLayer layer, boolean considerMask) {
        this(comp, layer, layer.getTX(), layer.getTY(), considerMask);
    }

    /**
     * This constructor can be called after the change
     * if the mask can be ignored
     */
    public TranslationEdit(Composition comp, ContentLayer layer, int oldTX, int oldTY, boolean considerMask) {
        super(comp, "");

        this.layer = layer;
        this.backupTX = oldTX;
        this.backupTY = oldTY;

        if (considerMask && layer.hasMask()) {
            LayerMask mask = layer.getMask();
            maskEdit = new TranslationEdit(comp, mask, mask.getTX(), mask.getTY(), false);
        }

        // currently always embedded
        embedded = true;
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
        int tmpTX = layer.getTX();
        int tmpTY = layer.getTY();

        layer.setTranslation(backupTX, backupTY);
        backupTX = tmpTX;
        backupTY = tmpTY;

        if (!embedded) {
            layer.getComp().imageChanged(FULL);
            History.notifyMenus(this);
        }
    }

    @Override
    public void die() {
        super.die();

        layer = null;
        if (maskEdit != null) {
            maskEdit.die();
        }
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addIntChild("Backup TX", backupTX);
        node.addIntChild("Backup TY", backupTY);

        return node;
    }
}
