/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.selection.Selection;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Shape;
import java.util.Objects;

/**
 * Represents the change of a selection shape
 * (via add, subtract, intersect, invert, move)
 */
public class SelectionShapeChangeEdit extends PixelitorEdit {
    private Shape backupShape;

    public SelectionShapeChangeEdit(String name, Composition comp, Shape backupShape) {
        super(name, comp);

        this.backupShape = Objects.requireNonNull(backupShape);
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
        Shape tmp;

        Selection selection = comp.getSelection();
        if (selection == null) {
            throw new IllegalStateException(
                    "no selection in " + comp.getName());
        }

        tmp = selection.getShape();

        selection.setShape(backupShape);

        backupShape = tmp;
    }
}
