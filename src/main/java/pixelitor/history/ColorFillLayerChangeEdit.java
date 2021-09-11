/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.ColorFillLayer;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Color;

public class ColorFillLayerChangeEdit extends PixelitorEdit {
    private final ColorFillLayer layer;
    private final Color before;
    private final Color after;

    public ColorFillLayerChangeEdit(ColorFillLayer layer, Color before, Color after) {
        super("Color Fill Layer Change", layer.getComp());

        this.layer = layer;
        this.before = before;
        this.after = after;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        layer.changeColor(before, false);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        layer.changeColor(after, false);
    }
}
