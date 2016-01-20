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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that uis created when a new layer is selected
 */
public class LayerSelectionChangeEdit extends PixelitorEdit {
    private Layer oldLayer;
    private Layer newLayer;

    public LayerSelectionChangeEdit(Composition comp, Layer oldLayer, Layer newLayer) {
        super(comp, "Layer Selection Change");

        this.oldLayer = oldLayer;
        this.newLayer = newLayer;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        comp.setActiveLayer(oldLayer, AddToHistory.NO);

        History.notifyMenus(this);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        comp.setActiveLayer(newLayer, AddToHistory.NO);

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        oldLayer = null;
        newLayer = null;
    }
}
