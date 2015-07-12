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
import pixelitor.layers.TextLayer;
import pixelitor.utils.UpdateGUI;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the rasterization of a text layer
 */
public class TextLayerRasterizeEdit extends PixelitorEdit {
    private TextLayer before;
    private ImageLayer after;

    public TextLayerRasterizeEdit(Composition comp, TextLayer before, ImageLayer after) {
        super(comp, "Text Layer Rasterize");
        this.before = before;
        this.after = after;
        comp.setDirty(true);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        comp.addLayer(before, AddToHistory.NO, false, false);
        comp.removeLayer(after, AddToHistory.NO, UpdateGUI.YES);
        History.notifyMenus(this);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        comp.addLayer(after, AddToHistory.NO, false, false);
        comp.removeLayer(before, AddToHistory.NO, UpdateGUI.YES);
        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        before = null;
        after = null;
        comp = null;
    }

    @Override
    public boolean canRepeat() {
        return false;
    }
}
