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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Checks the test coverage of history edits by ensuring that for every
 * history "add", a corresponding "undo" and "redo" operation is also tested.
 */
public final class HistoryChecker {
    // simulates the undo history stack
    private final Deque<EditState> undoStack = new ArrayDeque<>();
    // simulates the redo history stack
    private final Deque<EditState> redoStack = new ArrayDeque<>();

    private int maxUntestedEdits = 1;

    /**
     * Registers that a new undoable edit has been added.
     */
    public synchronized void registerAdd(String editName) {
        List<EditState> untestedEdits = undoStack.stream()
            .filter(edit -> !edit.isFullyTested())
            .toList();

        if (untestedEdits.size() >= maxUntestedEdits) {
            throw new AssertionError(String.format(
                "Cannot add \"%s\" because the limit of %d has been reached: %s",
                editName, maxUntestedEdits, untestedEdits
            ));
        }

        undoStack.push(new EditState(editName));
        // adding a new command clears the redo stack
        redoStack.clear();
    }

    /**
     * Registers that an undo operation has been performed.
     */
    public synchronized void registerUndo(String editName) {
        if (undoStack.isEmpty()) {
            throw new AssertionError(String.format("Undo called for \"%s\", but the undo stack is empty.", editName));
        }

        EditState lastEdit = undoStack.peek();
        if (!Objects.equals(lastEdit.name, editName)) {
            throw new AssertionError(String.format(
                "Undo called for \"%s\", but the last edit was \"%s\".", editName, lastEdit.name
            ));
        }

        EditState undoneEdit = undoStack.pop();
        undoneEdit.hasBeenUndone = true;
        redoStack.push(undoneEdit);
    }

    /**
     * Registers that a redo operation has been performed.
     */
    public synchronized void registerRedo(String editName) {
        if (redoStack.isEmpty()) {
            throw new AssertionError(String.format("Redo called for \"%s\", but the redo stack is empty.", editName));
        }

        EditState lastUndoneEdit = redoStack.peek();
        if (!Objects.equals(lastUndoneEdit.name, editName)) {
            throw new AssertionError(String.format(
                "Redo called for \"%s\", but the next available redo is \"%s\".", editName, lastUndoneEdit.name
            ));
        }

        EditState redoneEdit = redoStack.pop();
        redoneEdit.hasBeenRedone = true;
        undoStack.push(redoneEdit);
    }

    /**
     * Sets the maximum number of edits that can be added
     * consecutively without an immediate undo/redo test.
     */
    public synchronized void setMaxUntestedEdits(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("max = " + max);
        }
        this.maxUntestedEdits = max;
    }

    /**
     * Verifies that all registered edits have been fully tested and clears the checker's state.
     */
    public synchronized void verifyAndClear() {
        try {
            List<String> untestedEdits = undoStack.stream()
                .filter(edit -> !edit.isFullyTested())
                .map(EditState::toString)
                .toList();

            if (!untestedEdits.isEmpty()) {
                throw new AssertionError("History check failed: The following edits were not fully tested: " + untestedEdits);
            }
        } finally {
            clear(); // clear state for the next test
        }
    }

    /**
     * Clears the internal state of the checker.
     */
    public synchronized void clear() {
        undoStack.clear();
        redoStack.clear();
        maxUntestedEdits = 1; // reset to default
    }

    /**
     * Internal state holder for a single edit.
     */
    private static class EditState {
        final String name;
        boolean hasBeenUndone = false;
        boolean hasBeenRedone = false;

        EditState(String name) {
            this.name = name;
        }

        public boolean isFullyTested() {
            return hasBeenUndone && hasBeenRedone;
        }

        @Override
        public String toString() {
            String undoneStatus = hasBeenUndone ? "undone" : "not undone";
            String redoneStatus = hasBeenRedone ? "redone" : "not redone";
            return String.format("\"%s\" (%s, %s)", name, undoneStatus, redoneStatus);
        }
    }
}
