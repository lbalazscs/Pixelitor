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
import pixelitor.tools.Tools;
import pixelitor.tools.pen.SubPath;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import static pixelitor.tools.pen.BuildState.MOVING_TO_NEXT_ANCHOR;

public class CloseSubPathEdit extends PixelitorEdit {
    private final SubPath subPath;

    public CloseSubPathEdit(Composition comp, SubPath subPath) {
        super("Close Subpath", comp);
        this.subPath = subPath;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        subPath.undoClosing();
        Tools.PEN.setBuildState(MOVING_TO_NEXT_ANCHOR);
        comp.repaint();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        subPath.close(false);
        comp.repaint();
    }
}
