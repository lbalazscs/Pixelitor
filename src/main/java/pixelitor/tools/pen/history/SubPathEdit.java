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

import pixelitor.history.PixelitorEdit;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.SubPath;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class SubPathEdit extends PixelitorEdit {
    private final SubPath before;
    private final SubPath after;
    private final Path path;
    private final int index;

    public SubPathEdit(String name, SubPath before, SubPath after) {
        super(name, after.getComp());

        this.before = before;
        this.after = after;
        this.path = after.getPath();
        this.index = path.indexOf(after);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        // the zoom might have changed
        before.coCoordsChanged(comp.getIC());

        path.changeSubPath(index, before);

        comp.repaint();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        // the zoom might have changed
        after.coCoordsChanged(comp.getIC());

        path.changeSubPath(index, after);

        comp.repaint();
    }
}
