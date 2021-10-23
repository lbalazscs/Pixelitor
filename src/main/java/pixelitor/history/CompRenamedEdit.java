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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class CompRenamedEdit extends PixelitorEdit {
    private final String oldName;
    private final String newName;

    public CompRenamedEdit(Composition comp, String oldName, String newName) {
        super("Rename Image", comp);
        this.oldName = oldName;
        this.newName = newName;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        comp.setName(oldName);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        comp.setName(newName);
    }
}
