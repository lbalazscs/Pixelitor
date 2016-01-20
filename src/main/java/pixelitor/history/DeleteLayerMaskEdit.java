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
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the deletion of a layer mask
 */
public class DeleteLayerMaskEdit extends PixelitorEdit {
    private LayerMask oldMask;
    private Layer layer;

    public DeleteLayerMaskEdit(Composition comp, Layer layer, LayerMask oldMask) {
        super(comp, "Delete Layer Mask");

        comp.setDirty(true);
        this.layer = layer;
        this.oldMask = oldMask;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        layer.addMaskBack(oldMask);

        History.notifyMenus(this);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        layer.deleteMask(AddToHistory.NO, false);

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        layer = null;
        oldMask = null;
    }
}