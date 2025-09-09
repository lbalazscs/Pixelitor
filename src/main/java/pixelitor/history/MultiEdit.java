/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import java.util.Collections;
import java.util.List;

/**
 * A {@link PixelitorEdit} that represents multiple edits
 * that need to be undone/redone together.
 * Similar in purpose to javax.swing.undo.CompoundEdit
 */
public class MultiEdit extends PixelitorEdit {
    private final List<PixelitorEdit> children;

    public MultiEdit(String name, Composition comp, PixelitorEdit first, PixelitorEdit second) {
        super(name, comp);
        children = new ArrayList<>(2);
        children.add(first);
        children.add(second);
    }

    public MultiEdit(String name, Composition comp) {
        super(name, comp);
        children = new ArrayList<>(2);
        // child edits will be added later via add
    }

    public MultiEdit(String name, Composition comp, PixelitorEdit[] array) {
        super(name, comp);

        children = new ArrayList<>(array.length);
        Collections.addAll(children, array);
    }

    /**
     * Combines two possibly null edits into a single edit.
     */
    public static PixelitorEdit combine(PixelitorEdit first,
                                        PixelitorEdit second,
                                        String name) {
        if (first == null && second == null) {
            return null;
        }
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }

        assert first.getComp() == second.getComp();
        return new MultiEdit(name, first.getComp(), first, second);
    }

    public void add(PixelitorEdit edit) {
        children.add(edit);
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        for (int i = children.size() - 1; i >= 0; i--) { // undo in reverse order
            PixelitorEdit edit = children.get(i);
            edit.undo();
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        for (PixelitorEdit edit : children) {
            edit.redo();
        }
    }

    @Override
    public void die() {
        super.die();

        for (PixelitorEdit edit : children) {
            edit.die();
        }
    }

    public List<PixelitorEdit> getChildren() {
        return children;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        for (PixelitorEdit edit : children) {
            node.add(edit.createDebugNode());
        }

        return node;
    }
}