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
import pixelitor.layers.MaskViewMode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the enabling or disabling of a layer mask
 */
public class EnableLayerMaskEdit extends PixelitorEdit {
    private Layer layer;
    private final MaskViewMode oldMode;

    public EnableLayerMaskEdit(Composition comp, Layer layer, MaskViewMode oldMode) {
        super(comp, layer.isMaskEnabled() ?
                "Enable Layer Mask" : "Disable Layer Mask");

        this.layer = layer;
        this.oldMode = oldMode;
        comp.setDirty(true);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        changeEnabledState();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        changeEnabledState();
    }

    public void changeEnabledState() {
        boolean newEnabled = !layer.isMaskEnabled();
        layer.setMaskEnabled(newEnabled, AddToHistory.NO);
        if (newEnabled) {
            oldMode.activate(comp.getIC(), layer);
        }

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        layer = null;
    }
}
