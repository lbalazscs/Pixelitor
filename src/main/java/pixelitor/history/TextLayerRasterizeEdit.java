/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.MaskViewMode;
import pixelitor.layers.TextLayer;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the rasterization of a text layer
 */
public class TextLayerRasterizeEdit extends PixelitorEdit {
    private TextLayer before;
    private ImageLayer after;
    private MaskViewMode maskViewMode = null;

    public TextLayerRasterizeEdit(Composition comp, TextLayer before, ImageLayer after) {
        super("Text Layer Rasterize", comp);

        this.before = before;
        this.after = after;

        if (before.hasMask()) {
            maskViewMode = comp.getIC().getMaskViewMode();
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        comp.addLayer(before, false, null, false, false);
        comp.deleteLayer(after, false, true);

        // restore the original mask view mode of the text layer
        if (before.hasMask()) {
            maskViewMode.activate(before);
        }

        History.notifyMenus(this);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        comp.addLayer(after, false, null, false, false);
        comp.deleteLayer(before, false, true);

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        before = null;
        after = null;
    }
}
