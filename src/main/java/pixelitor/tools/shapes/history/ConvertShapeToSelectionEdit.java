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

package pixelitor.tools.shapes.history;

import pixelitor.Composition;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.StyledShape;
import pixelitor.tools.transform.TransformBox;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.util.Objects;

public class ConvertShapeToSelectionEdit extends PixelitorEdit {
    private final TransformBox box;
    private final StyledShape styledShape;
    private final PixelitorEdit selectionEdit;

    public ConvertShapeToSelectionEdit(Composition comp,
                                       TransformBox box,
                                       StyledShape styledShape,
                                       PixelitorEdit selectionEdit) {
        super("Convert Path to Selection", comp);

        this.box = Objects.requireNonNull(box);
        this.styledShape = Objects.requireNonNull(styledShape);
        this.selectionEdit = Objects.requireNonNull(selectionEdit);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        selectionEdit.undo();
        Tools.SHAPES.restoreBox(styledShape, box);
        Tools.SHAPES.activate();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        selectionEdit.redo();
        Tools.SHAPES.reset();
        Tools.LASSO_SELECTION.activate();
    }
}
