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
import pixelitor.history.MultiEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.Tools;
import pixelitor.tools.move.MoveTool;
import pixelitor.tools.transform.TransformBox;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Represents an applied "Free Transform" session,
 * which can be undone to restore the interactive {@link TransformBox}.
 */
public class ApplyTransformEdit extends MultiEdit {
    private final TransformUISnapshot uiSnapshot;

    public ApplyTransformEdit(String name, Composition comp, PixelitorEdit contentEdit, TransformUISnapshot snapshot) {
        super(name, comp);
        if (contentEdit != null) {
            // if the content edit is itself a MultiEdit (e.g., from transforming a layer
            // and selection), unpack its children into this edit to avoid unnecessary nesting
            if (contentEdit instanceof MultiEdit multi) {
                multi.getChildren().forEach(this::add);
            } else {
                this.add(contentEdit);
            }
        }
        this.uiSnapshot = snapshot;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        // after undoing the data changes, restore the interactive transform
        // session to the state it was in just before the transform was applied
        MoveTool moveTool = Tools.MOVE;
        moveTool.activate();
        moveTool.restoreTransformSession(uiSnapshot);
    }

    @Override
    public void redo() throws CannotRedoException {
        // if an interactive session was restored by a preceding undo, end it before redoing
        MoveTool moveTool = Tools.MOVE;
        if (moveTool.isFreeTransforming()) {
            moveTool.endTransformSession();
        }

        // re-apply the data changes
        super.redo();
    }
}

