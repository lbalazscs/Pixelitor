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

package pixelitor.tools.gradient.history;

import pixelitor.history.ImageEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.Drawable;
import pixelitor.tools.Tools;
import pixelitor.tools.gradient.Gradient;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * The modifications made to an existing gradient that has
 * been applied to a {@link Drawable}.
 */
public class GradientChangeEdit extends PixelitorEdit {
    private final Drawable dr;
    private final Gradient before;
    private final Gradient after;
    private ImageEdit imageEdit;
    private final boolean imageEditNeeded;

    public GradientChangeEdit(String editName, Drawable dr, Gradient before, Gradient after) {
        super(editName == null ? "Change Gradient" : editName, dr.getComp());
        this.dr = dr;
        this.before = before;
        this.after = after;

        // if both gradients are solid overlays, then simply re-rendering
        // the gradient is enough to perfectly restore the pixel state
        imageEditNeeded = !before.isSolidOverlay() || !after.isSolidOverlay();

        if (imageEditNeeded) {
            // capture the pixel data before the "after" gradient was applied
            imageEdit = ImageEdit.createEmbedded(dr);
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        if (imageEditNeeded) {
            imageEdit.undo();
        }

        Tools.GRADIENT.setGradient(before, !imageEditNeeded, dr);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        if (imageEditNeeded) {
            imageEdit.redo();
        }

        Tools.GRADIENT.setGradient(after, !imageEditNeeded, dr);
    }

    @Override
    public void die() {
        super.die();

        if (imageEditNeeded) {
            imageEdit.die();
        }
    }
}
