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
import pixelitor.layers.LayerMask;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the linking or unlinking of a layer mask
 */
public class LinkLayerMaskEdit extends PixelitorEdit {
    private LayerMask mask;

    public LinkLayerMaskEdit(Composition comp, LayerMask mask) {
        super(comp, mask.isLinked() ?
                "Link Layer Mask" : "Unlink Layer Mask");

        this.mask = mask;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        mask.setLinked(!mask.isLinked(), AddToHistory.NO);

        History.notifyMenus(this);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        mask.setLinked(!mask.isLinked(), AddToHistory.NO);

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        mask = null;
    }
}
