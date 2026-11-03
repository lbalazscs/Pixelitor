/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import javax.swing.undo.CompoundEdit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents multiple edits that need to be undone/redone together.
 * Similar in purpose to {@link CompoundEdit}.
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

    public MultiEdit(String name, Composition comp, PixelitorEdit[] edits) {
        super(name, comp);

        children = new ArrayList<>(edits.length);
        Collections.addAll(children, edits);
    }

    private MultiEdit(String name, Composition comp, List<PixelitorEdit> children) {
        super(name, comp);
        this.children = children;
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

    /**
     * Combines the non-null edits into one: null if there are none,
     * the edit itself if there is only one, otherwise a new MultiEdit.
     */
    public static PixelitorEdit combineNonNull(String name, Composition comp,
                                               Collection<? extends PixelitorEdit> edits) {
        List<PixelitorEdit> nonNull = edits.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));

        return switch (nonNull.size()) {
            case 0 -> null;
            case 1 -> nonNull.getFirst();
            default -> new MultiEdit(name, comp, nonNull);
        };
    }

    public void add(PixelitorEdit edit) {
        assert edit != null;
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
