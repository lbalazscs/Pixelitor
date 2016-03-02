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
import pixelitor.layers.MaskViewMode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the adding of a layer mask.
 */
public class AddLayerMaskEdit extends PixelitorEdit {
    private Layer layer;
    private LayerMask layerMask;
    private MaskViewMode newMode;

    public AddLayerMaskEdit(Composition comp, Layer layer, String name) {
        super(comp, name);

        this.layer = layer;
        this.layerMask = layer.getMask();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        // has to be saved here, because when the constructor is
        // called, we do not know yet the mode before the undo
        newMode = comp.getIC().getMaskViewMode();

        layer.deleteMask(AddToHistory.NO);

        History.notifyMenus(this);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        layer.addMask(layerMask);

        assert newMode != null;
        newMode.activate(comp, layer);

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        layer = null;
        layerMask = null;
    }
}
