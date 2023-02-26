/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.Layer;
import pixelitor.layers.LayerHolder;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Represents the replacement of a layer with another
 */
public class ReplaceLayerEdit extends PixelitorEdit {
    private LayerHolder holder;
    private Layer before;
    private Layer after;

    public ReplaceLayerEdit(Layer before, Layer after, String editName) {
        super(editName, after.getComp(), true);

        this.holder = after.getHolder();
        this.before = before;
        this.after = after;

        assert after.isActive();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        assert after.isActive();

        holder.replaceLayer(after, before);

        assert before.isActive();
        assert before.hasUI();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        holder.replaceLayer(before, after);

        assert after.isActive();
        assert after.hasUI();
    }

    @Override
    public void die() {
        super.die();

        before = null;
        after = null;
        holder = null;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(before.createDebugNode("before"));
        node.add(after.createDebugNode("after"));

        return node;
    }
}
