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
import pixelitor.tools.pen.SubPath;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import static pixelitor.tools.pen.PathBuilder.State.BEFORE_SUBPATH;
import static pixelitor.tools.pen.PathBuilder.State.MOVING_TO_NEXT_ANCHOR_POINT;

public class AddAnchorPointEdit extends PixelitorEdit {
    private final SubPath subPath;
    private final AnchorPoint anchorPoint;
    private final boolean finishSubPath;

    public AddAnchorPointEdit(Composition comp,
                              SubPath subPath,
                              AnchorPoint anchorPoint,
                              boolean finishSubPath) {
        super("Add Anchor Point", comp);
        this.subPath = subPath;
        this.anchorPoint = anchorPoint;
        this.finishSubPath = finishSubPath;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        subPath.deleteLast();
        Tools.PEN.setBuilderState(MOVING_TO_NEXT_ANCHOR_POINT, "AddAnchorPointEdit.undo");
        comp.repaint();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        subPath.addPoint(anchorPoint);
        if (finishSubPath) {
            Tools.PEN.setBuilderState(BEFORE_SUBPATH, "AddAnchorPointEdit.redo");
        }
        comp.repaint();
    }
}
