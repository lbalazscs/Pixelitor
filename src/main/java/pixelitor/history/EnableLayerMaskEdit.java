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
 * A PixelitorEdit that represents the enabling or disabling of a layer mask
 */
public class EnableLayerMaskEdit extends PixelitorEdit {
    private Layer layer;

    public EnableLayerMaskEdit(Composition comp, Layer layer) {
        super(comp, layer.isMaskEnabled() ?
                "Enable Layer Mask" : "Disable Layer Mask");

        this.layer = layer;
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

    private void changeEnabledState() {
        boolean newEnabled = !layer.isMaskEnabled();
        layer.setMaskEnabled(newEnabled, AddToHistory.NO);

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        layer = null;
    }
}
