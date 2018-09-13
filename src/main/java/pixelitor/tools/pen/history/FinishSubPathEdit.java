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
import pixelitor.tools.pen.SubPath;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import static pixelitor.tools.pen.PathBuilder.State.BEFORE_SUBPATH;

/**
 * Created when a subpath is finished by ctrl-click
 */
public class FinishSubPathEdit extends PixelitorEdit {
    private final SubPath subPath;

    public FinishSubPathEdit(Composition comp, SubPath subPath) {
        super("Finish Subpath", comp);
        this.subPath = subPath;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        Tools.PEN.setPathBuildingInProgressState("FinishSubPathEdit.undo");
        subPath.setFinished(false, "FinishSubPathEdit.undo");
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        Tools.PEN.setBuilderState(BEFORE_SUBPATH, "FinishSubPathEdit.redo");
        subPath.setFinished(true, "FinishSubPathEdit.redo");
    }
}
