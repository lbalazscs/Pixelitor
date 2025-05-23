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
 * Represents the creation of a new gradient on a {@link Drawable}.
 */
public class NewGradientEdit extends PixelitorEdit {
    private final Drawable dr;
    private final Gradient gradient;
    private final ImageEdit imageEdit; // stores the pixel data changes

    public NewGradientEdit(Drawable dr, Gradient gradient) {
        super("Create Gradient", dr.getComp());
        this.dr = dr;
        this.gradient = gradient;

        // capture the pixels before the gradient is applied
        imageEdit = ImageEdit.createEmbedded(dr);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        Tools.GRADIENT.setGradient(null, false, dr);
        imageEdit.undo();
        dr.update();
        dr.updateIconImage();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        // this must be called even if the gradient is regenerated,
        // in order to maintain the image edit's internal state
        imageEdit.redo();

        // restore the gradient handles and settings in the gradient tool's UI
        // (there's no need to regenerate the gradient)
        Tools.GRADIENT.setGradient(gradient, false, dr);

        dr.update();
        dr.updateIconImage();
    }
}
