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
import pixelitor.tools.move.MoveTool;
import pixelitor.tools.transform.TransformBox;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Represents the cancellation of a "Free Transform" session,
 * which can be undone to restore the interactive {@link TransformBox}.
 */
public class CancelTransformEdit extends PixelitorEdit {
    private final TransformUISnapshot uiSnapshot;

    public CancelTransformEdit(String name, Composition comp, TransformUISnapshot snapshot) {
        super(name, comp);
        this.uiSnapshot = snapshot;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        // restore the interactive transform session to its state just before it was canceled
        MoveTool moveTool = Tools.MOVE;
        moveTool.activate();
        moveTool.restoreTransformSession(uiSnapshot);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        // re-execute the cancellation logic without creating a new history entry
        MoveTool moveTool = Tools.MOVE;
        if (moveTool.isFreeTransforming()) {
            moveTool.cancelTransform(false);
        }
    }
}
