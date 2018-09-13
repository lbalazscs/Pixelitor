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

public class AddAnchorPointEdit extends PixelitorEdit {
    private final SubPath subPath;
    private final AnchorPoint anchorPoint;

    public AddAnchorPointEdit(Composition comp,
                              SubPath subPath,
                              AnchorPoint anchorPoint) {
        super("Add Anchor Point", comp);
        this.subPath = subPath;
        this.anchorPoint = anchorPoint;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        subPath.deleteLast();

        comp.repaint();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        subPath.addPoint(anchorPoint);

        Tools.PEN.setPathBuildingInProgressState("AddAnchorPointEdit.redo");

        comp.repaint();
    }
}
