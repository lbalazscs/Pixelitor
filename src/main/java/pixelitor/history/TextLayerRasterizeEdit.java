/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.Composition.LayerAdder;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.MaskViewMode;
import pixelitor.layers.TextLayer;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import static pixelitor.Composition.LayerAdder.Position.ABOVE_ACTIVE;

/**
 * A PixelitorEdit that represents the rasterization of a text layer
 */
public class TextLayerRasterizeEdit extends PixelitorEdit {
    private TextLayer before;
    private ImageLayer after;
    private MaskViewMode maskViewMode = null;

    public TextLayerRasterizeEdit(Composition comp, TextLayer before, ImageLayer after) {
        super("Rasterize Text Layer", comp);

        this.before = before;
        this.after = after;

        if (before.hasMask()) {
            maskViewMode = comp.getView().getMaskViewMode();
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        new LayerAdder(comp)
                .atPosition(ABOVE_ACTIVE)
                .noRefresh()
                .add(before);
        comp.deleteLayer(after, false);

        assert before.isActive();
        assert before.hasUI();
        // restore the original mask view mode of the text layer
        if (before.hasMask()) {
            maskViewMode.activate(before, "rasterize undone");
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        new LayerAdder(comp)
                .atPosition(ABOVE_ACTIVE)
                .noRefresh()
                .add(after);
        comp.deleteLayer(before, false);
    }

    @Override
    public void die() {
        super.die();

        before = null;
        after = null;
    }
}
