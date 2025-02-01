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
import pixelitor.history.HandleMovedEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.TextLayer;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.PathActions;
import pixelitor.tools.pen.PathTool;
import pixelitor.tools.transform.history.TransformBoxChangedEdit;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.util.ArrayList;
import java.util.List;

/**
 * Only represents the deletion of a path or a subpath, or a path flipping.
 * Similarly, {@link SubPathEdit} is only used to represent
 * the deletion of an anchor point.
 * Other types of path editing are tracked either by
 * {@link HandleMovedEdit} or by {@link TransformBoxChangedEdit}.
 */
public class PathEdit extends PixelitorEdit {
    private final Path before;
    private final Path after;
    private final PathTool toolBefore;
    private final List<TextLayer> formerUsers;

    public PathEdit(String name, Composition comp, Path before, Path after, PathTool tool) {
        super(name, comp);
        this.before = before;
        this.after = after;
        this.toolBefore = tool;

        formerUsers = new ArrayList<>();
        comp.forEachNestedLayer(TextLayer.class, textLayer -> {
            if (textLayer.isOnPath()) {
                formerUsers.add(textLayer);
            }
        });
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        setPath(before);
        for (TextLayer textLayer : formerUsers) {
            if (isPathDeletion()) {
                textLayer.usePathEditing();
            } else {
                textLayer.pathChanged(false);
            }
        }
        comp.repaint();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        setPath(after);
        for (TextLayer textLayer : formerUsers) {
            textLayer.pathChanged(isPathDeletion());
        }
        comp.repaint();
    }

    private void setPath(Path newPath) {
        comp.setActivePath(newPath);

        Tool activeTool = Tools.getActive();
        if (activeTool instanceof PathTool activePathTool) {
            Tools.PEN.setPath(newPath);
            if (newPath == null) {
                Tools.PEN.activate();
            }
            PathActions.setActionsEnabled(newPath != null);

            if (toolBefore != null && activePathTool != toolBefore) {
                // return to the tool before the edit
                toolBefore.activate();
            }
        }
    }

    // whether this edit represents a path deletion
    private boolean isPathDeletion() {
        return after == null;
    }
}

