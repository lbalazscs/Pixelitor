/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.history.HandleMovedEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.TextLayer;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.PenToolMode;
import pixelitor.tools.transform.history.TransformBoxChangedEdit;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.util.ArrayList;
import java.util.List;

/**
 * Only represents the deletion of a path or a subpath.
 * Similarly, {@link SubPathEdit} is only used to represent
 * the deletion of an anchor point.
 * Other types of path editing are tracked either by
 * {@link HandleMovedEdit} or by {@link TransformBoxChangedEdit}.
 */
public class PathEdit extends PixelitorEdit {
    private final Path before;
    private final Path after;
    private final PenToolMode modeBefore;
    private List<TextLayer> formerUsers;

    public PathEdit(String name, Composition comp, Path before, Path after) {
        super(name, comp);
        this.before = before;
        this.after = after;
        modeBefore = Tools.PEN.getMode();

        if (isPathDeletion()) {
            formerUsers = new ArrayList<>();
            comp.forEachNestedLayer(TextLayer.class, textLayer -> {
                if (textLayer.isOnPath()) {
                    formerUsers.add(textLayer);
                }
            });
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        setPath(before);
        if (isPathDeletion()) {
            for (TextLayer textLayer : formerUsers) {
                textLayer.usePathEditing();
            }
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        setPath(after);
        if (isPathDeletion()) {
            for (TextLayer textLayer : formerUsers) {
                textLayer.pathChanged(true);
            }
        }
    }

    private void setPath(Path path) {
        comp.setActivePath(path);

        if (Tools.PEN.isActive()) {
            if (path == null) {
                Tools.PEN.removePath();
                Tools.PEN.startBuilding(false);
            } else {
                Tools.PEN.setPath(path);
                if (Tools.PEN.getMode() != modeBefore) {
                    // if the path is not null, return
                    // to the mode before the edit
                    modeBefore.start();
                }
            }
            comp.repaint();
        }
    }

    // whether this edit represents a path deletion event
    private boolean isPathDeletion() {
        return after == null;
    }
}
