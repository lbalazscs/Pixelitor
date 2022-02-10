/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.shapes.history;

import pixelitor.Composition;
import pixelitor.history.PartialImageEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.StyledShape;
import pixelitor.tools.transform.TransformBox;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class FinalizeShapeEdit extends PixelitorEdit {
    private final PartialImageEdit imageEdit;
    private final TransformBox box;
    private final StyledShape styledShape;

    public FinalizeShapeEdit(Composition comp, PartialImageEdit imageEdit,
                             TransformBox box, StyledShape styledShape) {
        super("Finalize Shape", comp);

        // the image edit can be null!
        assert box != null;
        assert styledShape != null;

        this.imageEdit = imageEdit;
        this.box = box;
        this.styledShape = styledShape;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        if (imageEdit != null) {
            imageEdit.undo();
        }
        Tools.SHAPES.restoreBox(styledShape, box);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        if (imageEdit != null) {
            imageEdit.redo();
        }
        Tools.SHAPES.resetInitialState();
    }

    @Override
    public void die() {
        super.die();
        if (imageEdit != null) {
            imageEdit.die();
        }
    }
}
