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

package pixelitor.tools.transform.history;

import pixelitor.Composition;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.Tools;
import pixelitor.tools.transform.TransformBox;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A lightweight edit representing a single incremental modification
 * of a {@link TransformBox} during a free transform session.
 */
public class TransformStepEdit extends PixelitorEdit {
    private TransformBox.Memento state;

    public TransformStepEdit(String name, Composition comp, TransformBox.Memento state) {
        super(name, comp);
        this.state = state;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        if (!swapTransformBoxState()) {
            throw new CannotUndoException();
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        if (!swapTransformBoxState()) {
            throw new CannotRedoException();
        }
    }

    /**
     * Returns true if successful.
     */
    private boolean swapTransformBoxState() {
        TransformBox transformBox = Tools.MOVE.getTransformBox();
        if (transformBox == null) {
            // can happen if the session was terminated unexpectedly
            return false;
        }

        TransformBox.Memento currentState = transformBox.createMemento();
        transformBox.restoreFrom(state);
        this.state = currentState;
        return true; // success
    }
}
