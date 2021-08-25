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

import pixelitor.Composition;
import pixelitor.layers.Layer;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Represents the replacement of a layer with another
 */
public class ReplaceLayerEdit extends PixelitorEdit {
    private Layer before;
    private Layer after;

    public ReplaceLayerEdit(Composition comp, Layer before, Layer after, String editName) {
        super(editName, comp);

        this.before = before;
        this.after = after;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        comp.replaceLayer(after, before);

        assert before.isActive();
        assert before.hasUI();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        comp.replaceLayer(before, after);
    }

    @Override
    public void die() {
        super.die();

        before = null;
        after = null;
    }

    @Override
    public DebugNode createDebugNode() {
        DebugNode node = super.createDebugNode();

        node.add(before.createDebugNode("before"));
        node.add(after.createDebugNode("after"));

        return node;
    }
}
