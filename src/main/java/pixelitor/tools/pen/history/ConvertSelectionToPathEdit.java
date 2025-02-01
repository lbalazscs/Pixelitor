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

package pixelitor.tools.pen.history;

import pixelitor.Composition;
import pixelitor.history.PixelitorEdit;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Shape;

public class ConvertSelectionToPathEdit extends PixelitorEdit {
    private final Shape oldSelectionShape;
    private final Path oldPath;

    public ConvertSelectionToPathEdit(Composition comp,
                                      Shape oldSelectionShape, Path oldPath) {
        super("Convert Selection to Path", comp);
        this.oldSelectionShape = oldSelectionShape;
        this.oldPath = oldPath;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        assert !comp.hasSelection();

        comp.setActivePath(oldPath);
        comp.createSelectionFrom(oldSelectionShape);
        Tools.LASSO_SELECTION.activate();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        SelectionActions.selectionToPath(comp, false);
    }
}
