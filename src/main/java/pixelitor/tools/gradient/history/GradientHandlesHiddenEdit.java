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
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.Tools;
import pixelitor.tools.gradient.Gradient;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class GradientHandlesHiddenEdit extends PixelitorEdit {
    private final Gradient gradient;

    public GradientHandlesHiddenEdit(Composition comp, Gradient gradient) {
        super("Hide Gradient Handles", comp);
        this.gradient = gradient;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        Tools.GRADIENT.setGradient(gradient, false, comp.getIC());
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        Tools.GRADIENT.setGradient(null, false, comp.getIC());
    }
}
