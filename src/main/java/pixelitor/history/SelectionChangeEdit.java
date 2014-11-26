/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.history;

import pixelitor.Composition;
import pixelitor.selection.Selection;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Shape;

/**
 * Represents the change of a selection shape (via add, subtract, intersect or invert)
 */
public class SelectionChangeEdit extends PixelitorEdit {
    private Shape backupShape;

    public SelectionChangeEdit(Composition comp, Shape backupShape, String name) {
        super(comp, name);

        if (backupShape == null) {
            throw new IllegalArgumentException("backupShape is null");
        }

        this.backupShape = backupShape;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        swapShapes();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        swapShapes();
    }

    private void swapShapes() {
        Selection selection = comp.getSelection();
        Shape tmp;
        if (selection != null) {
            tmp = selection.getShape();
            selection.setShape(backupShape);
        } else {
            throw new IllegalStateException();
        }

        backupShape = tmp;
        History.postEdit(this);
    }

    @Override
    public boolean canRepeat() {
        return false;
    }
}
