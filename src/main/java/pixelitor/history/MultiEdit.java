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
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.util.ArrayList;
import java.util.List;

/**
 * A PixelitorEdit that represents multiple edits
 * that need to be undone/redone together.
 * Similar in purpose to javax.swing.undo.CompoundEdit
 */
public class MultiEdit extends PixelitorEdit {
    private final List<PixelitorEdit> edits;

    public MultiEdit(String name, Composition comp, PixelitorEdit first, PixelitorEdit second) {
        super(name, comp);
        edits = new ArrayList<>(2);
        edits.add(first);
        edits.add(second);
    }

    public MultiEdit(String name, Composition comp) {
        super(name, comp);
        edits = new ArrayList<>(2);
    }

    /**
     * Combines two possibly null edits
     */
    public static PixelitorEdit combine(PixelitorEdit first,
                                        PixelitorEdit second,
                                        String name) {
        PixelitorEdit combined = null;
        if (first != null && second != null) {
            assert first.getComp() == second.getComp();
            combined = new MultiEdit(name, first.getComp(), first, second);
        } else if (first != null) {
            combined = first;
        } else if (second != null) {
            combined = second;
        }
        return combined;
    }

    public void add(PixelitorEdit edit) {
        edits.add(edit);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        for (int i = edits.size() - 1; i >= 0; i--) { // undo in reverse order
            PixelitorEdit edit = edits.get(i);
            edit.undo();
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        for (PixelitorEdit edit : edits) {
            edit.redo();
        }
    }

    @Override
    public void die() {
        super.die();

        for (PixelitorEdit edit : edits) {
            edit.die();
        }
    }

    @Override
    public DebugNode getDebugNode() {
        var node = super.getDebugNode();

        for (PixelitorEdit edit : edits) {
            node.add(edit.getDebugNode());
        }

        return node;
    }
}