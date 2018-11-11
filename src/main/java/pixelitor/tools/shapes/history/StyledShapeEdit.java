/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Represents an edit where the {@link StyledShape}
 * changes, but its associated transform box doesn't
 */
public class StyledShapeEdit extends PixelitorEdit {
    private StyledShape backup;

    public StyledShapeEdit(Composition comp,
                           StyledShape backup) {
        super("Change Shape", comp);
        this.backup = backup;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        swapStyledShapes();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        swapStyledShapes();
    }

    private void swapStyledShapes() {
        StyledShape tmp = Tools.SHAPES.getStyledShape();
        Tools.SHAPES.setStyledShape(backup);
        backup = tmp;

        comp.imageChanged();
    }
}
