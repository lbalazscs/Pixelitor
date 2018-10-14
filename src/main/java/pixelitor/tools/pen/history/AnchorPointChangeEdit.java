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
import pixelitor.tools.pen.AnchorPoint;
import pixelitor.tools.pen.SubPath;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;


public class AnchorPointChangeEdit extends PixelitorEdit {
    private final AnchorPoint before;
    private final AnchorPoint after;

    public AnchorPointChangeEdit(String name, Composition comp,
                                 AnchorPoint before, AnchorPoint after) {
        super(name, comp);
        this.before = before;
        this.after = after;

        assert before.getSubPath() == after.getSubPath();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        SubPath subPath = after.getSubPath();
        subPath.replacePoint(after, before);
        comp.repaint();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        SubPath subPath = before.getSubPath();
        subPath.replacePoint(before, after);
        comp.repaint();
    }
}
