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

package pixelitor.tools.pen.history;

import pixelitor.Composition;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.PenToolMode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class ConvertPathToSelectionEdit extends PixelitorEdit {
    private final Path path;
    private final PixelitorEdit selectionEdit;
    private final PenToolMode mode;

    public ConvertPathToSelectionEdit(Composition comp,
                                      Path path,
                                      PixelitorEdit selectionEdit,
                                      PenToolMode mode) {
        super("Convert Path to Selection", comp);
        this.path = path;
        this.selectionEdit = selectionEdit;
        this.mode = mode;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        selectionEdit.undo();
        comp.setActivePath(path);

        Tools.PEN.setModeChooserCombo(mode);
        Tools.PEN.setPath(path, "ConvertPathToSelectionEdit.undo");
        Tools.PEN.activate();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        selectionEdit.redo();

        comp.setActivePath(null);
        Tools.PEN.resetStateToInitial();
        Tools.SELECTION.activate();
    }
}
