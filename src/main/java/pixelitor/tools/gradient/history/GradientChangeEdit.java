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

package pixelitor.tools.gradient.history;

import pixelitor.Composition;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.Tools;
import pixelitor.tools.gradient.Gradient;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class GradientChangeEdit extends PixelitorEdit {
    private Gradient before;
    private Gradient after;
    private ImageEdit imageEdit;
    private final boolean imageEditNeeded;

    public GradientChangeEdit(Composition comp, Gradient before, Gradient after) {
        super("Gradient Change", comp);
        this.before = before;
        this.after = after;

        imageEditNeeded = !before.fullyCovers() || !after.fullyCovers();

        if (imageEditNeeded) {
            imageEdit = ImageEdit.create(comp, "", true);
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        if (imageEditNeeded) {
            imageEdit.undo();
        }

        Tools.GRADIENT.setGradient(before, !imageEditNeeded, comp.getIC());

        History.notifyMenus(this);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        if (imageEditNeeded) {
            imageEdit.redo();
        }

        Tools.GRADIENT.setGradient(after, !imageEditNeeded, comp.getIC());

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        before = null;
        after = null;
        if (imageEditNeeded) {
            imageEdit.die();
        }
    }
}
