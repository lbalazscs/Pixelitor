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

public class PathEdit extends PixelitorEdit {
    private final Path before;
    private final Path after;
    private final PenToolMode modeBefore;

    public PathEdit(String name, Composition comp, Path before, Path after) {
        super(name, comp);
        this.before = before;
        this.after = after;
        modeBefore = Tools.PEN.getMode();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        setPath(before);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        setPath(after);
    }

    private void setPath(Path path) {
        comp.setActivePath(path);

        if (Tools.PEN.isActive()) {
            Tools.PEN.setPath(path);
            if (path == null) {
                Tools.PEN.startBuilding(false);
            } else if (Tools.PEN.getMode() != modeBefore) {
                // if the path is not null, return
                // to the mode before the edit
                modeBefore.start();
            }
            comp.repaint();
        }
    }
}
