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
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.SubPath;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import static pixelitor.tools.pen.BuildState.IDLE;

/**
 * Represents the starting of a new subpath within a path.
 */
public class SubPathStartEdit extends PixelitorEdit {
    private final Path path;
    private final boolean wasFirstSP;
    private final SubPath subPath;

    public SubPathStartEdit(Composition comp, Path path, SubPath subPath) {
        super("Subpath Start", comp);

        assert path != null;

        this.path = path;
        wasFirstSP = path.getNumSubPaths() == 1;
        this.subPath = subPath;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        boolean noMoreLeft = path.deleteLastSubPath();
        assert wasFirstSP == noMoreLeft;
        if (noMoreLeft) {
            Tools.PEN.removePath();
        }
        Tools.PEN.setBuildState(IDLE);
        comp.repaint();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        path.addSubPath(subPath);
        subPath.setFinished(false);
        if (wasFirstSP) {
            comp.setActivePath(path);
        }
        Tools.PEN.setBuildingInProgressState();
        comp.repaint();
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addBoolean("was first subpath", wasFirstSP);
        node.add(path.createDebugNode("path " + path.getId()));

        return node;
    }
}
