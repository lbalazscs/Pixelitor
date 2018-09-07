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
import pixelitor.tools.pen.AnchorPoint;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.PenToolMode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import static pixelitor.tools.pen.PathBuilder.State.BEFORE_SUBPATH;

public class SubPathStartEdit extends PixelitorEdit {
    private final Path path;
    private final AnchorPoint point;
    private final boolean wasFirst;

    public SubPathStartEdit(Composition comp, Path path, AnchorPoint point, boolean wasFirst) {
        super("Subpath Start", comp);
        this.path = path;
        this.point = point;
        this.wasFirst = wasFirst;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        boolean noMoreLeft = path.deleteLastSubPath();
        assert wasFirst == noMoreLeft;
        if (noMoreLeft) {
            Tools.PEN.setPath(null, "SubPathStartEdit.undo");
        }
        PenToolMode.BUILD.setState(BEFORE_SUBPATH, "SubPathStartEdit.undo");
        comp.repaint();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        path.startNewSubPath(point, false);
        if (wasFirst) {
            Tools.PEN.setPath(path, "SubPathStartEdit.redo");
        }
        comp.repaint();
    }
}
